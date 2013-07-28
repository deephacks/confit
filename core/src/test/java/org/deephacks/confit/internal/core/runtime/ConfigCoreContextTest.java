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
package org.deephacks.confit.internal.core.runtime;

import org.deephacks.confit.Config;
import org.deephacks.confit.Id;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.test.ConfigDefaultSetup;
import org.deephacks.confit.test.ConfigTestData.*;
import org.deephacks.confit.test.DateTime;
import org.deephacks.confit.test.DurationTime;
import org.junit.Before;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.deephacks.confit.model.Events.CFG108;
import static org.deephacks.confit.model.Events.CFG306;
import static org.deephacks.confit.test.ConfigTestData.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

public class ConfigCoreContextTest extends ConfigDefaultSetup {

    @Before
    public void before() {
        DefaultBeanManager.clear();
        setupDefaultConfigData();
    }

    @Test
    public void test_get() {
        admin.create(defaultBeans);
        Grandfather g_runtime = config.get("g1", Grandfather.class).get();
        assertReflectionEquals(g1, g_runtime, ReflectionComparatorMode.LENIENT_ORDER);
    }

    @Test
    public void test_all() {
        admin.create(defaultBeans);

        Grandfather g1_runtime = config.get("g1", Grandfather.class).get();
        assertReflectionEquals(g1, g1_runtime, ReflectionComparatorMode.LENIENT_ORDER);

        Grandfather g2_runtime = config.get("g2", Grandfather.class).get();
        assertReflectionEquals(g2, g2_runtime, ReflectionComparatorMode.LENIENT_ORDER);
        List<Grandfather> all = config.list(Grandfather.class);

        List<Grandfather> g_list = Arrays.asList(g1, g2);
        ArrayList<Grandfather> result = new ArrayList<>();
        for (Grandfather g : all) {
            if (g.getBeanId().getInstanceId().equals("g1") ||  g.getBeanId().getInstanceId().equals("g2"))
            result.add(g);
        }
        assertReflectionEquals(g_list, result, ReflectionComparatorMode.LENIENT_ORDER);
    }

    @Config(name = "immutable", desc = "")
    static class ImmutableConfig {
        @Id(desc = "")
        private String id;

        @Config(desc = "")
        private final String test = "test";
    }

    /**
     * Test that final @Property are treated as immutable, that AdminContext should not be able
     * to set it.
     */
    @Test
    @SuppressWarnings("unused")
    public void test_immutable() {
        config.register(ImmutableConfig.class);
        Bean b = Bean.create(BeanId.create("1", "immutable"));
        b.setProperty("test", "something else");
        try {
            admin.set(b);
            fail("Should not be able to set immutable properties");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG306));
        }
        try {
            admin.create(b);
            fail("Should not be able to set immutable properties");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG306));
        }
        try {
            admin.merge(b);
            fail("Should not be able to set immutable properties");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG306));
        }

    }
    @Config(name = "transient", desc = "")
    static class TransientConfig {
        @Id(desc = "")
        private String id;

        @Config(desc = "")
        private transient String test = "test";
    }
    /**
     * Test that transient @Property cannot be registered.
     */
    @Test
    @SuppressWarnings("unused")
    public void test_transient_modifier() {
        try {
            config.register(TransientConfig.class);
            fail("Transient properties should not be allowed");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG108));
        }
    }

    /**
     * Test that non-final static @Property cannot be registered.
     */
    @Test
    public void test_non_final_static_modifier() {

        try {
            config.register(NonFinalStaticPropConfig.class);
            fail("Non final static properties should not be allowed");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG108));
        }
    }

    @Config(name = "nonfinalstaticprop", desc = "")
    @SuppressWarnings("unused")
    static class NonFinalStaticPropConfig {
        @Id(desc = "")
        private String id;

        @Config(desc = "")
        private static String test = "test";
    }

    @Test
    public void test_non_final_static_id() {

        try {
            config.register(NonFinalStaticIdConfig.class);
            fail("Non final static properties should not be allowed");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG108));
        }
    }

    @Config(name = "nonfinalstaticid", desc = "")
    @SuppressWarnings("unused")
    static class NonFinalStaticIdConfig {
        @Id(desc = "")
        private static String id;

    }

    @Config(desc = "")
    static class SingletonConfig {
    }

    @Test
    @SuppressWarnings("unused")
    public void test_singleton() {
        config.register(SingletonConfig.class);
        SingletonConfig singleton = config.get(SingletonConfig.class);
        assertNotNull(singleton);
    }

    /**
     * Test that configuration falls back to application.conf if bean manager does
     * not have requested instances.
     */
    @Test
    public void test_get_file_fallback() throws Exception {
        Grandfather g10 = config.get("g10", Grandfather.class).get();
        assertGrandfather10(g10);
    }

    /**
     * Test that configuration falls back to application.conf if bean manager does
     * not have requested instances.
     */
    @Test
    public void test_list_file_fallback() throws Exception {
        List<Grandfather> grandfathers = config.list(Grandfather.class);
        assertThat(grandfathers.size(), is(3));
        for (Grandfather g : grandfathers) {
            if (g.getBeanId().equals("g10")) {
                assertGrandfather10(g);
            }
        }
    }

    @Test
    public void test_singleton_file_fallback() throws Exception {
        SingletonParent singletonParent = config.get(SingletonParent.class);
        assertNotNull(singletonParent);

        // test that reference to other get exist
        assertThat(singletonParent.getProperty(), is(SINGLETON_PARENT_SCHEMA_NAME));
        Singleton singleton = singletonParent.getSingleton();
        assertNotNull(singleton);
        assertThat(singleton.getProperty(), is(SINGLETON_SCHEMA_NAME));

        // also check that references to other non-get instances exist
        Parent p10 = singleton.getParent();
        assertThat(p10.getProp1(), is("p10"));
        // ... and its references
        List<Child> children = p10.getProp7();
        assertNotNull(children);
        assertThat(children.size(), is(1));
        Child child = children.get(0);
        assertThat(child.getBeanId().getInstanceId(), is("c10"));
        assertThat(child.getProp11(), is(1.1f));
    }

    /**
     * Test that admin configuration override to application.conf
     */
    @Test
    public void test_singleton_admin_override_file_fallback() {
        Singleton singleton = new Singleton();
        singleton.setProperty("override");
        admin.createObject(singleton);
        singleton = config.get(Singleton.class);
        assertThat(singleton.getProperty(), is("override"));
    }

    /**
     * Test that admin configuration override to application.conf
     */
    @Test
    public void test_admin_override_get_file_fallback() {
        Grandfather g10 = new Grandfather("g10");
        g10.setProp1("g10");
        admin.createObject(g10);
        Grandfather g = config.get("g10", Grandfather.class).get();
        assertThat(g.getProp1(), is("g10"));
    }

    /**
     * Test that admin configuration override to application.conf
     */
    @Test
    public void test_admin_override_list_file_fallback() {
        Grandfather g10 = new Grandfather("g10");
        g10.setProp1("g10");
        Grandfather g11 = new Grandfather("g11");
        g10.setProp1("g11");
        // override 2 of 3 instances
        admin.createObjects(Arrays.asList(getGrandfather("g10"), getGrandfather("g11")));
        List<Grandfather> result = config.list(Grandfather.class);
        assertThat(result.size(), is(2));
        for (Grandfather g : result) {
            if (g.getBeanId().equals("g10")) {
                assertEquals(g.getProp1(), "g10");
            }
            if (g.getBeanId().equals("g11")) {
                assertEquals(g.getProp1(), "g11");
            }
        }
    }

    @Test
    public void test_disparate_unregistered_file_fallback() {
        A a = config.get(A.class);
        assertThat(a.getName(), is("A"));
        B b = config.get(B.class);
        assertThat(b.getName(), is("B"));
    }

    private void assertGrandfather10(Grandfather g) throws Exception {
        assertThat(g.getProp1(), is("defaultValue"));
        assertThat(g.getProp2(), hasItems("c", "b", "a"));
        assertThat(g.getProp4(), is(new DateTime("2002-09-24-06:00")));
        assertThat(g.getProp5(), is(new DurationTime("PT15H")));
        assertThat(g.getProp8(), is((byte) 1));
        assertThat(g.getProp9(), is(1000000000000L));
        assertThat(g.getProp10(), is((short) 123));
        assertThat(g.getProp11(), is(12313.13f));
        assertThat(g.getProp12(), is(238.476238746834796));
        assertThat(g.getProp13(), is(true));
        assertThat(g.getProp14(), is(TimeUnit.NANOSECONDS));
        assertThat(g.getProp15(), is(new URL("http://www.deephacks.org")));
        assertThat(g.getProp16(), is(new File(".")));
        assertThat(g.getProp17(), hasItems(new File("."), new File(".")));
        assertThat(g.getProp18(), hasItems(new URL("http://www.deephacks.org"), new URL(
                "http://www.google.se")));
        assertThat(g.getProp19(), hasItems(TimeUnit.DAYS, TimeUnit.HOURS));
        assertThat(g.getProp21(), is(1));

        List<Parent> parents = g.getProp7();
        assertThat(parents.size(), is(2));
        for (Parent p : parents) {
            assertThat(p.getProp7().get(0).getBeanId().getInstanceId(), is("c10"));
            assertThat(p.getProp7().get(0).getProp11(), is(1.1f));
        }
    }
}
