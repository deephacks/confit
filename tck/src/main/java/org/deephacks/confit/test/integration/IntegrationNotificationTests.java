package org.deephacks.confit.test.integration;

import org.deephacks.confit.ConfigChanges;
import org.deephacks.confit.ConfigChanges.ConfigChange;
import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.ConfigObserver;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.test.ConfigTestData;
import org.deephacks.confit.test.ConfigTestData.Child;
import org.deephacks.confit.test.ConfigTestData.Grandfather;
import org.deephacks.confit.test.ConfigTestData.Parent;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

@RunWith(FeatureTestsRunner.class)
public class IntegrationNotificationTests {
    private static ConfigContext config = ConfigContext.lookup();
    private static AdminContext admin = AdminContext.lookup();
    private static TestConfigObserver observer = new TestConfigObserver();
    static {
        config.register(Grandfather.class, Parent.class, Child.class);
        config.registerObserver(observer);
    }

    @Test
    public void test_simple_create_notification() {
        Grandfather grandfather = ConfigTestData.getGrandfather("1");
        admin.createObject(grandfather);
        assertThat(observer.getChanges().size(), is(1));
        for (ConfigChange<Grandfather>  change : observer.getChanges(Grandfather.class)) {
            assertFalse(change.before().isPresent());
            assertTrue(change.after().isPresent());
            assertReflectionEquals(change.after().get(), grandfather, LENIENT_ORDER);
        }
    }

    @Test
    public void test_simple_delete_notification() {
        Parent parent = ConfigTestData.getParent("1");
        admin.createObject(parent);
        assertThat(observer.getChanges().size(), is(1));
        for (ConfigChange<Grandfather>  change : observer.getChanges(Grandfather.class)) {
            assertFalse(change.before().isPresent());
            assertTrue(change.after().isPresent());
            assertReflectionEquals(change.before().get(), parent, LENIENT_ORDER);
        }
    }

    @Test
    public void test_simple_set_notification() {
        Parent before = ConfigTestData.getParent("1");
        Parent after = ConfigTestData.getParent("1");
        admin.createObject(before);
        assertThat(observer.getChanges().size(), is(1));
        after.setProp1("new");
        admin.setObject(after);
        for (ConfigChange<Parent> change : observer.getChanges(Parent.class)) {
            assertTrue(change.before().isPresent());
            assertTrue(change.after().isPresent());
            Parent beforeChange = change.before().get();
            Parent afterChange = change.after().get();
            assertReflectionEquals(beforeChange, before, LENIENT_ORDER);
            assertReflectionEquals(afterChange, after, LENIENT_ORDER);
        }
    }

    @Test
    public void test_simple_merge_notification() {
        Parent before = ConfigTestData.getParent("1");
        Parent after = new Parent("1");
        admin.createObject(before);
        assertThat(observer.getChanges().size(), is(1));
        after.setProp1("new");
        admin.mergeObject(after);
        for (ConfigChange<Parent>  change : observer.getChanges(Parent.class)) {
            assertTrue(change.before().isPresent());
            assertTrue(change.after().isPresent());
            Parent beforeChange = change.before().get();
            Parent afterChange = change.after().get();
            assertReflectionEquals(beforeChange, before, LENIENT_ORDER);
            assertReflectionEquals(afterChange, after, LENIENT_ORDER);
        }
    }


    public static class TestConfigObserver implements ConfigObserver {
        private ConfigChanges changes = new ConfigChanges();

        @Override
        public void notify(ConfigChanges changes) {
            this.changes = changes;
        }

        public ConfigChanges getChanges() {
            return changes;
        }

        public <T> Collection<ConfigChange<T>> getChanges(Class<T> cls) {
            return changes.getChanges(cls);
        }

        public <T> ConfigChange<T> getFirstChange(Class<T> cls) {
            return changes.getChanges(cls).iterator().next();
        }

        public <T> T getFirstAfter(Class<T> cls) {
            return getFirstChange(cls).after().get();
        }

        public <T> T getFirstBefore(Class<T> cls) {
            return getFirstChange(cls).before().get();
        }

        public void clear() {
            changes = new ConfigChanges();
        }

        public <T> boolean isBeforePresent(Class<T> cls) {
            return getFirstChange(cls).before().isPresent();
        }
        public <T> boolean isAfterPresent(Class<T> cls) {
            return getFirstChange(cls).after().isPresent();
        }
    }
}
