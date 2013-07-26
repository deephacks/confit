package org.deephacks.confit.test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.deephacks.confit.test.FeatureTestsBuilder.TestRound;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.MethodRule;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * FeatureTestsRunner is used by {@link FeatureTestsRunner}. A {@link FeatureTestsRunner}
 * must be annotated {@link RunWith} with value {@link FeatureTestsRunner}.
 */
@SuppressWarnings("deprecation")
public class FeatureTestsRunner extends ParentRunner<Runner> implements Filterable {
    /** store last ran FeatureTests in tmp dir */
    private static final File tmp = new File(System.getProperty("java.io.tmpdir"));
    /** file containing the class name of last run FeatureTests */
    private static final File lastRunFeatureFile = new File(tmp, "last_feature_tests");
    /** only set if user filter test (using eclipse junit panel for example) */
    private Filter filter;

    public FeatureTestsRunner(Class<?> klass) throws Throwable {
        super(klass);
    }

    @Override
    protected List<Runner> getChildren() {
        try {
            return getRunners();
        } catch (InitializationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Description describeChild(Runner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(Runner child, RunNotifier notifier) {
        child.run(notifier);
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        this.filter = filter;
        super.filter(filter);
    }

    @Override
    protected Statement classBlock(final RunNotifier notifier) {
        Statement statement = childrenInvoker(notifier);

        // When tests are filtered (using eclipse junit panel,
        // executing a single test, for example) the TestClass is
        // incorrectly put before any {@link FeatureSetupTeardown}.

        // following lines are commented from super.classBlock()
        // that accomplish goals stated above.

        // statement= withBeforeClasses(statement);
        // statement= withAfterClasses(statement);
        // statement= withClassRules(statement);

        return statement;
    }

    @Override
    public void run(final RunNotifier notifier) {
        EachTestNotifier testNotifier = new EachTestNotifier(notifier, getDescription());
        try {
            Statement statement = this.classBlock(notifier);
            statement.evaluate();
        } catch (AssumptionViolatedException e) {
            testNotifier.fireTestIgnored();
        } catch (StoppedByUserException e) {
            throw e;
        } catch (Throwable e) {
            testNotifier.addFailure(e);
        }
    }

    public List<Runner> getRunners() throws InitializationError {
        final Object test = getFeatureTest(getTestClass().getJavaClass());
        final Class<?> testClass = test.getClass();
        final ArrayList<Runner> runners = new ArrayList<>();
        List<TestRound> rounds = getRounds(test);
        if (rounds != null) {
            writeSuiteContext(testClass);
        }
        if (filter != null && rounds == null) {
            // means the user choose to run a single test from
            // a suite of tests, in eclipse for example.
            Class<?> suite = readSuiteContext();
            if (suite != null) {
                rounds = getRounds(getFeatureTest(suite));
            }
        }
        if (rounds != null) {
            for (TestRound round : rounds) {
                runners.add(getRunnerForParameters(round));
            }
        } else {
            throw new IllegalArgumentException(
                    "Means one of the following things: corrupt/missing "
                            + lastRunFeatureFile.getAbsolutePath() + " or "
                            + "feature tests was run without using a "
                            + FeatureTestsBuilder.class.getName() + ".");
        }
        return runners;
    }

    public BlockJUnit4ClassRunner getRunnerForParameters(TestRound round)
            throws InitializationError {
        return new TestClassRunnerForParameters(round);
    }

    public Object getFeatureTest(Class<?> cls) {
        try {
            return cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<TestRound> getRounds(final Object test) {
        try {
            Class<?> testClass = test.getClass();
            if (FeatureTests.class.isAssignableFrom(testClass)) {
                Method m = testClass.getDeclaredMethod("build", (Class<?>[]) null);
                return (List<TestRound>) m.invoke(test, (Object[]) null);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * The feature suite class context is lost when a user decide to filter out
     * and run a single test method (from the eclipse junit panel for example).
     * Therefore write it to file so the text execution context can be reconstructed.
     */
    private void writeSuiteContext(Class<?> suiteClass) {
        try {
            Files.write(suiteClass.getName().getBytes(), lastRunFeatureFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> readSuiteContext() {
        try {
            String className = Files.readFirstLine(lastRunFeatureFile, Charsets.UTF_8);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (className != null && !"".equals(className.trim())) {
                return cl.loadClass(className);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public class TestClassRunnerForParameters extends BlockJUnit4ClassRunner {
        protected final TestRound round;

        public TestClassRunnerForParameters(TestRound round) throws InitializationError {
            super(round.getTestClass());
            this.round = round;
        }

        /**
         * There was no natural way of extend this method, so its almost completely copied from BlockJUnit4ClassRunner.
         *
         * @see org.junit.runners.BlockJUnit4ClassRunner#methodBlock(org.junit.runners.model.FrameworkMethod)
         */

        @Override
        protected Statement methodBlock(FrameworkMethod method) {
            Object test;
            try {
                test = new ReflectiveCallable() {
                    @Override
                    protected Object runReflectiveCall() throws Throwable {
                        return createTest();
                    }
                }.run();
            } catch (Throwable e) {
                return new Fail(e);
            }

            Statement statement = methodInvoker(method, test);
            statement = possiblyExpectingExceptions(method, test, statement);
            statement = withPotentialTimeout(method, test, statement);
            statement = withBefores(method, test, statement);
            // put {@link FeatureSetupTeardown} before regular @Before
            for (Object setupTeardown : round.getSetupTeardowns()) {
                statement = withSetupTeardownBefores(setupTeardown, statement);
            }
            statement = withAfters(method, test, statement);
            // put {@link FeatureSetupTeardown} after regular @After
            for (Object setupTeardown : round.getSetupTeardowns()) {
                statement = withSetupTeardownAfters(setupTeardown, statement);
            }
            statement = withRules(method, test, statement);

            return statement;
        }

        @Override
        protected Statement classBlock(RunNotifier notifier) {
            Statement statement = childrenInvoker(notifier);
            statement = withBeforeClasses(statement);
            // put {@link FeatureSetupTeardown} before regular @BeforeClass
            for (Object setupTeardown : round.getSetupTeardowns()) {
                statement = withBeforeSetupTeardownClasses(setupTeardown, statement);
            }
            statement = withAfterClasses(statement);
            // put {@link FeatureSetupTeardown} after regular @AfterClasss
            for (Object setupTeardown : round.getSetupTeardowns()) {
                statement = withAfterSetupTeardownClasses(setupTeardown, statement);
            }
            statement = withClassRules(statement);
            return statement;
        }

        protected Statement withSetupTeardownBefores(Object target, Statement statement) {
            List<FrameworkMethod> befores = getMethods(target.getClass(), Before.class);
            return befores.isEmpty() ? statement : new RunBefores(statement, befores, target);
        }

        protected Statement withSetupTeardownAfters(Object target, Statement statement) {
            List<FrameworkMethod> befores = getMethods(target.getClass(), After.class);
            return befores.isEmpty() ? statement : new RunAfters(statement, befores, target);
        }

        protected Statement withAfterSetupTeardownClasses(Object setupTeardown, Statement statement) {
            List<FrameworkMethod> teardowns = getMethods(setupTeardown.getClass(), AfterClass.class);
            return teardowns.isEmpty() ? statement : new RunAfters(statement, teardowns, null);
        }

        protected Statement withBeforeSetupTeardownClasses(Object setupTeardown, Statement statement) {
            List<FrameworkMethod> befores = getMethods(setupTeardown.getClass(), BeforeClass.class);
            return befores.isEmpty() ? statement : new RunBefores(statement, befores, null);
        }

        /**
         * Simply copied from BlockJUnit4ClassRunner, since its not accessible.
         */
        private Statement withRules(FrameworkMethod method, Object target, Statement statement) {
            Statement result = statement;
            for (MethodRule each : getTestClass().getAnnotatedFieldValues(target, Rule.class,
                    MethodRule.class))
                result = each.apply(result, method, target);
            return result;
        }

        private List<FrameworkMethod> getMethods(Class<?> cls, Class<? extends Annotation> a) {
            ArrayList<FrameworkMethod> methods = new ArrayList<FrameworkMethod>();
            for (Method m : cls.getDeclaredMethods()) {
                Annotation result = m.getAnnotation(a);
                if (result != null) {
                    methods.add(new FrameworkMethod(m));
                }
            }
            return methods;
        }

        @Override
        public Object createTest() throws Exception {
            return getTestClass().getOnlyConstructor().newInstance();
        }

        private Statement withClassRules(Statement statement) {
            List<TestRule> classRules = classRules();
            return classRules.isEmpty() ? statement : new RunRules(statement, classRules,
                    getDescription());
        }

        @Override
        protected String getName() {
            return round.getTestClass().getName();
        }

        @Override
        protected String testName(final FrameworkMethod method) {
            if (round.getName().isPresent()) {
                return String.format("%s[%s]", method.getName(), round.getName().get());
            } else {
                return method.getName();
            }

        }

        @Override
        protected void validateConstructor(List<Throwable> errors) {
            validateOnlyOneConstructor(errors);
        }

        @Override
        protected Annotation[] getRunnerAnnotations() {
            return new Annotation[0];
        }
    }

}
