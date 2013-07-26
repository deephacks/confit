/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.confit.test;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.junit.runners.Parameterized.Parameters;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link FeatureTestsBuilder} is to be used by {@link FeatureTests} to build a set of
 * feature tests.
 */
public abstract class FeatureTestsBuilder {
    /** name of the test suite as shown in junit test execution */
    protected String name;
    /** Run before every test */
    private Map<Class<?>, Object> setUp = new LinkedHashMap<Class<?>, Object>();
    /** Run after every test */
    private Map<Class<?>, Object> tearDown = new LinkedHashMap<Class<?>, Object>();
    /** TestSetupTeardown classes found on classpath */
    private static final Map<String, Class<?>> setupTeardownRegistry = new HashMap<String, Class<?>>();
    /** TestSetupTeardown to run for each test */
    private static final LinkedHashMap<String, Class<?>> setupTeardowns = new LinkedHashMap<String, Class<?>>();
    /** TestSetupTeardown that are parameterized to run for each test */
    private static final Map<Class<?>, Method> parameterizedMethods = new HashMap<Class<?>, Method>();

    static {
        // search classpath for @TestSetupTeardown annotated classes
        findSetupTeardownClasses();
    }

    /**
     * Set implementation for a specific service interface. Will automatically
     * enforce any setup/teardown requirements that the implementation might have
     * by searching classpath for TestSetupTeardown annotated classes.
     */
    public FeatureTestsBuilder using(Class<?> service, Object impl) {
        // make sure Lookup.get().lookup() find the the right implementation.
        LookupProxy.register(service, impl);
        Class<?> setupTeardown = setupTeardownRegistry.get(service.getName());
        if (setupTeardown != null) {
            // make sure implementation setup/teardown is run.
            Method m = getParameterizedMethod(setupTeardown);
            if (m != null) {
                parameterizedMethods.put(setupTeardown, m);
            } else {
                setupTeardowns.put(setupTeardown.getName(), setupTeardown);
            }
        }
        return this;
    }

    /**
     * Add setup classes to be run @Before
     */
    public FeatureTestsBuilder withSetUp(Object setUp) {

        this.setUp.put(setUp.getClass(), setUp);

        return this;
    }

    protected Map<Class<?>, Object> getSetUp() {
        return setUp;
    }

    /**
     * Add teardown classes to be run @After
     */
    public FeatureTestsBuilder withTearDown(Object tearDown) {
        this.tearDown.put(tearDown.getClass(), setUp);
        return this;
    }

    protected Map<Class<?>, Object> getTearDown() {
        return tearDown;
    }

    protected abstract List<Class<?>> getTests();

    public List<TestRound> build() {
        final List<Class<?>> testClasses = getTests();
        List<TestRound> rounds = new ArrayList<>();
        for (Class<?> test : testClasses) {
            ArrayList<Object> setups = new ArrayList<>();
            try {
                for (Class<?> setupTeardown : setupTeardowns.values()) {
                    setups.add(newInstance(setupTeardown));
                }
                for (Class<?> setupTeardown : parameterizedMethods.keySet()) {
                    Method m = parameterizedMethods.get(setupTeardown);
                    @SuppressWarnings("unchecked")
                    Collection<Object[]> argMatrix = (Collection<Object[]>) m.invoke(null,
                            (Object[]) null);
                    for (Object[] args : argMatrix) {
                        Object setup = newInstance(setupTeardown, args);
                        ArrayList<Object> roundSetups = new ArrayList<Object>(setups);
                        roundSetups.add(setup);
                        rounds.add(new TestRound(test, args[0].toString(), roundSetups));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (parameterizedMethods.size() == 0) {
                rounds.add(new TestRound(test, setups));
            }
        }
        return rounds;
    }

    public Object newInstance(Class<?> cls) {
        try {
            return cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object newInstance(Class<?> cls, Object[] args) {
        try {
            return getConstructor(cls).newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Constructor<?> getConstructor(Class<?> setupTeardown) {
        for (Constructor<?> c : setupTeardown.getConstructors()) {
            if (c.getParameterTypes().length == 1) {
                return c;
            }
        }
        throw new RuntimeException("Parameterized class must have a constructor with one argument.");
    }

    private Method getParameterizedMethod(Class<?> setupTeardown) {
        for (Method m : setupTeardown.getDeclaredMethods()) {
            try {
                if (m.getAnnotation(Parameters.class) != null) {
                    return m;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * Search classpath and init TestSetupTeardown classes.
     */
    private static void findSetupTeardownClasses() {
        AnnotationDB db = new AnnotationDB();
        try {
            URL[] urls = ClasspathUrlFinder.findClassPaths();
            db.scanArchives(urls);
            Map<String, Set<String>> annotationIndex = db.getAnnotationIndex();

            for (String cls : annotationIndex.get(FeatureSetupTeardown.class.getName())) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class<?> setup = cl.loadClass(cls);
                FeatureSetupTeardown anno = setup.getAnnotation(FeatureSetupTeardown.class);
                if (setupTeardownRegistry.get(anno.value().getName()) != null) {
                    if (!cls.toLowerCase().contains("default")) {
                        setupTeardownRegistry.put(anno.value().getName(), setup);
                    }
                } else {
                    setupTeardownRegistry.put(anno.value().getName(), setup);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestRound {
        private Optional<String> name;
        private Class<?> testClass;
        private List<Object> setupTeardowns = new ArrayList<>();

        public TestRound(Class<?> testClass, String name, List<Object> setupTeardowns) {
            Preconditions.checkNotNull(testClass);
            Preconditions.checkNotNull(name);
            Preconditions.checkNotNull(setupTeardowns);
            this.name = Optional.of(name);
            this.setupTeardowns.addAll(setupTeardowns);
            this.testClass = testClass;
        }

        public TestRound(Class<?> cls, List<Object> setupTeardowns) {
            this.name = Optional.absent();
            this.setupTeardowns.addAll(setupTeardowns);
            this.testClass = cls;
        }

        public Optional<String> getName() {
            return name;
        }

        public Class<?> getTestClass() {
            return testClass;
        }

        public List<Object> getSetupTeardowns() {
            return setupTeardowns;
        }
    }

}
