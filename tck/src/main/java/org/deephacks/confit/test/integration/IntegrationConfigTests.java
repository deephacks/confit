package org.deephacks.confit.test.integration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import junit.framework.AssertionFailedError;
import org.deephacks.confit.ConfigChanges;
import org.deephacks.confit.ConfigChanges.ConfigChange;
import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.ConfigObserver;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.test.ConfigTestData.*;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.deephacks.confit.model.Events.*;
import static org.deephacks.confit.test.ConfigTestData.*;
import static org.deephacks.confit.test.ConversionUtils.toBean;
import static org.deephacks.confit.test.ConversionUtils.toBeans;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

@RunWith(FeatureTestsRunner.class)
public class IntegrationConfigTests {
    private TestConfigObserver observer = new TestConfigObserver();
    private ConfigContext config = ConfigContext.lookup();
    private AdminContext admin = AdminContext.lookup();
    private Child c1;
    private Child c2;
    private Parent p1;
    private Parent p2;
    private Grandfather g1;
    private Grandfather g2;
    private SingletonParent sp1;
    private Singleton s1;
    private Collection<Bean> defaultBeans;

    @Before
    public void setupDefaultConfigData() {
        sp1 = new SingletonParent();
        s1 = new Singleton();

        c1 = getChild("c1");
        c2 = getChild("c2");

        p1 = getParent("p1");
        p1.add(c2, c1);
        p1.set(c1);
        p1.put(c1);
        p1.put(c2);

        p2 = getParent("p2");
        p2.add(c1, c2);
        p2.set(c2);
        p2.put(c1);
        p2.put(c2);

        g1 = getGrandfather("g1");
        g1.add(p1, p2);

        g2 = getGrandfather("g2");
        g2.add(p1, p2);
        g2.put(p1);
        config.registerObserver(observer);
        config.register(Person.class, Grandfather.class, Parent.class, Child.class, Singleton.class,
                SingletonParent.class, JSR303Validation.class);
        if (defaultBeans == null) {
            // toBeans steals quite a bit of performance when having larger hierarchies.
            defaultBeans = ImmutableList.copyOf(toBeans(c1, c2, p1, p2, g1, g2));
        }
    }

    @After
    public void after() {
        observer.clear();
    }

    @Test
    public void test_create_set_merge_non_existing_property() {
        createDefault();
        observer.clear();
        Bean bean = Bean.create(c1.getBeanId());
        bean.addProperty("non_existing", "bogus");
        try {
            admin.create(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
            assertThat(observer.getChanges().size(), is(0));
        }
        try {
            admin.set(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
            assertThat(observer.getChanges().size(), is(0));
        }
        try {
            admin.merge(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
            assertThat(observer.getChanges().size(), is(0));
        }
        try {
            bean = Bean.create(BeanId.create("c5", CHILD_SCHEMA_NAME));
            bean.setReference("non_existing", c1.getBeanId());
            admin.create(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG111));
            assertThat(observer.getChanges().size(), is(0));
        }
        bean = Bean.create(c1.getBeanId());
        bean.addProperty("non_existing", "bogus");
        try {
            admin.set(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
            assertThat(observer.getChanges().size(), is(0));
        }
        try {
            admin.merge(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
            assertThat(observer.getChanges().size(), is(0));
        }
    }

    /**
     * Test the possibility for:
     *
     * 1) Creating individual beans that have references to eachother.
     * 2) Created beans can be fetched individually.
     * 3) That the config view sees the same result.
     */
    @Test
    public void test_create_single_then_get_list() {
        createThenGet(c1);
        createThenGet(c2);
        listAndAssert(c1.getBeanId().getSchemaName(), c1, c2);
        createThenGet(p1);
        createThenGet(p2);
        listAndAssert(p1.getBeanId().getSchemaName(), p1, p2);
        createThenGet(g1);
        createThenGet(g2);
        listAndAssert(g1.getBeanId().getSchemaName(), g1, g2);
    }

    /**
     * Test the possibility for:
     *
     * 1) Creating a collection of beans that have references to eachother.
     * 2) Created beans can be fetched individually afterwards.
     * 3) Created beans can be listed afterwards.
     * 4) That the config view sees the same result as admin view.
     */
    @Test
    public void test_create_multiple_then_get_list() {
        createDefault();
        getAndAssert(c1);
        getAndAssert(c2);
        listAndAssert(c1.getBeanId().getSchemaName(), c1, c2);
        getAndAssert(p1);
        getAndAssert(p2);
        listAndAssert(p1.getBeanId().getSchemaName(), p1, p2);
        getAndAssert(g1);
        getAndAssert(g2);
        listAndAssert(g1.getBeanId().getSchemaName(), g1, g2);

    }

    /**
     * Test that lookup beans have their default instance created after registration.
     */
    @Test
    public void test_register_singleton() {
        Singleton singleton = config.get(Singleton.class);
        assertNotNull(singleton);
    }

    /**
     * Test that lookup references are automatically assigned.
     */
    @Test
    public void test_singleton_references() {
        // provision a bean without the lookup reference.
        Bean singletonParent = toBean(sp1);
        admin.create(singletonParent);

        // assert that the lookup reference is set for config
        SingletonParent parent = config.get(SingletonParent.class);
        assertNotNull(parent.getSingleton());

        // assert that the lookup is available from admin but
        // references, however, is not set because it does not exist
        Bean result = admin.get(singletonParent.getId()).get();
        assertNotNull(result);
    }

    /**
     * Test the possibility for:
     *
     * 1) Setting an empty bean that will erase properties and references.
     * 3) Bean that was set empty can be fetched individually.
     * 4) That the config view sees the same result as admin view.
     */
    @Test
    public void test_set_get_single() {
        createDefault();
        Grandfather empty = new Grandfather("g1");
        Bean empty_expect = toBean(empty);

        admin.set(empty_expect);
        Bean empty_result = admin.get(empty.getBeanId()).get();
        assertReflectionEquals(empty_expect, empty_result, LENIENT_ORDER);
    }

    @Test
    public void test_set_get_list() {
        createDefault();
        Grandfather empty_g1 = new Grandfather("g1");
        Grandfather empty_g2 = new Grandfather("g2");
        Collection<Bean> empty_expect = toBeans(empty_g1, empty_g2);
        admin.set(empty_expect);
        Collection<Bean> empty_result = admin.list(empty_g1.getBeanId().getSchemaName());
        assertReflectionEquals(empty_expect, empty_result, LENIENT_ORDER);
        runtimeAllAndAssert(empty_g1.getClass(), empty_g1, empty_g2);
    }

    @Test
    public void test_set_empty_properties() {
        createThenGet(c1);
        Bean b = Bean.create(BeanId.create("c1", CHILD_SCHEMA_NAME));
        admin.set(b);
        Bean result = admin.get(b.getId()).get();
        assertThat(result.getPropertyNames().size(), is(0));
    }

    @Test
    public void test_merge_get_single() {
        createDefault();

        Grandfather merged = new Grandfather("g1");
        merged.setProp14(TimeUnit.NANOSECONDS);
        merged.setProp19(Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS));
        merged.setProp1("newName");
        merged.setProp8((byte) 0);
        merged.setProp9(0);
        merged.setProp10((short) 0);
        merged.setProp11(0);
        merged.setProp12(0);
        merged.setProp13(false);
        merged.setProp21(0);

        Bean mergeBean = toBean(merged);
        admin.merge(mergeBean);

        // modify the original to fit the expected merge
        g1.setProp1(merged.getProp1());
        g1.setProp19(merged.getProp19());
        g1.setProp14(merged.getProp14());
        g1.setProp8(merged.getProp8());
        g1.setProp9(merged.getProp9());
        g1.setProp10(merged.getProp10());
        g1.setProp11(merged.getProp11());
        g1.setProp12(merged.getProp12());
        g1.setProp13(merged.getProp13());
        g1.setProp21(merged.getProp21());
        getAndAssert(g1);
    }

    @Test
    public void test_merge_get_list() {
        createDefault();

        Grandfather g1_merged = new Grandfather("g1");
        g1_merged.setProp14(TimeUnit.NANOSECONDS);
        g1_merged.setProp19(Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS));
        g1_merged.setProp1("newName");
        g1_merged.setProp1("newName");
        g1_merged.setProp8((byte) 0);
        g1_merged.setProp9(0);
        g1_merged.setProp10((short) 0);
        g1_merged.setProp11(0);
        g1_merged.setProp12(0);
        g1_merged.setProp13(false);
        g1_merged.setProp21(0);

        Grandfather g2_merged = new Grandfather("g2");
        g2_merged.setProp14(TimeUnit.NANOSECONDS);
        g2_merged.setProp19(Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS));
        g2_merged.setProp1("newName");
        g2_merged.setProp8((byte) 0);
        g2_merged.setProp9(0);
        g2_merged.setProp10((short) 0);
        g2_merged.setProp11(0);
        g2_merged.setProp12(0);
        g2_merged.setProp13(false);
        g2_merged.setProp21(0);

        Collection<Bean> mergeBeans = toBeans(g1_merged, g2_merged);
        admin.merge(mergeBeans);

        // modify the original to fit the expected merge
        g1.setProp1(g1_merged.getProp1());
        g1.setProp19(g1_merged.getProp19());
        g1.setProp14(g1_merged.getProp14());
        g1.setProp8(g1_merged.getProp8());
        g1.setProp9(g1_merged.getProp9());
        g1.setProp10(g1_merged.getProp10());
        g1.setProp11(g1_merged.getProp11());
        g1.setProp12(g1_merged.getProp12());
        g1.setProp13(g1_merged.getProp13());
        g1.setProp21(g1_merged.getProp21());

        g2.setProp1(g2_merged.getProp1());
        g2.setProp19(g2_merged.getProp19());
        g2.setProp14(g2_merged.getProp14());
        g2.setProp8(g2_merged.getProp8());
        g2.setProp9(g2_merged.getProp9());
        g2.setProp10(g2_merged.getProp10());
        g2.setProp11(g2_merged.getProp11());
        g2.setProp12(g2_merged.getProp12());
        g2.setProp13(g2_merged.getProp13());
        g2.setProp21(g2_merged.getProp21());

        listAndAssert(g1.getBeanId().getSchemaName(), g1, g2);

    }

    @Test
    public void test_merge_and_set_broken_references() {
        createDefault();
        // try merge a invalid single reference
        Bean b = Bean.create(BeanId.create("p1", PARENT_SCHEMA_NAME));
        b.addReference("prop6", BeanId.create("non_existing_child_ref", CHILD_SCHEMA_NAME));
        try {
            admin.merge(b);
            fail("Should not be possible to merge invalid reference");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301 && e.getEvent().getCode() != CFG304) {
                e.printStackTrace();
                fail("Should not be possible to merge invalid reference");
            }
            assertThat(observer.getChanges().size(), is(0));
        }

        // try merge a invalid reference on collection
        b = Bean.create(BeanId.create("p2", PARENT_SCHEMA_NAME));
        b.addReference("prop7", BeanId.create("non_existing_child_ref", CHILD_SCHEMA_NAME));
        try {
            admin.merge(b);
            fail("Should not be possible to merge invalid reference");
            assertThat(observer.getChanges().size(), is(0));
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301 && e.getEvent().getCode() != CFG304) {
                e.printStackTrace();
                fail("Should not be possible to merge invalid reference");
            }
        }

        // try set a invalid single reference
        b = Bean.create(BeanId.create("parent4", PARENT_SCHEMA_NAME));
        b.addReference("prop6", BeanId.create("non_existing_child_ref", CHILD_SCHEMA_NAME));
        try {
            admin.set(b);
            fail("Should not be possible to merge beans that does not exist");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301 && e.getEvent().getCode() != CFG304) {
                e.printStackTrace();
                fail("Should not be possible to merge invalid reference");
            }
            assertThat(observer.getChanges().size(), is(0));
        }

        // try merge a invalid single reference
        b = Bean.create(BeanId.create("p1", PARENT_SCHEMA_NAME));
        b.addReference("prop6", BeanId.create("non_existing_child_ref", CHILD_SCHEMA_NAME));
        try {
            admin.set(b);
            fail("Should not be possible to merge invalid reference");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301 && e.getEvent().getCode() != CFG304) {
                e.printStackTrace();
                fail("Should not be possible to merge invalid reference");
            }
            assertThat(observer.getChanges().size(), is(0));
        }
    }

    @Test
    public void test_merge_delete_check_predecessors() {
        Grandfather g1 = new Grandfather("g1");
        Parent p1 = new Parent("p1");
        Child c1 = new Child("c1");
        g1.add(p1);

        admin.create(toBean(c1));
        admin.create(toBean(p1));
        admin.create(toBean(g1));

        Bean gb1 = toBean(g1);
        Bean pb1 = toBean(p1);
        admin.get(pb1.getId());

        // delete the parent reference by setting the property to empty list
        gb1.setReferences("prop7", new ArrayList<BeanId>());

        admin.merge(gb1);
        try {
            admin.delete(pb1.getId());
        } catch (AbortRuntimeException e) {
            fail("Should be possible to delete parent since grandfather "
                    + "no longer have a reference to it");
        }
    }

    @Test
    public void test_set_delete_check_predecessors() {
        Grandfather g1 = new Grandfather("g1");
        Parent p1 = new Parent("p1");
        Child c1 = new Child("c1");
        g1.add(p1);

        admin.create(toBean(c1));
        admin.create(toBean(p1));
        admin.create(toBean(g1));

        Bean gb1 = toBean(g1);
        Bean pb1 = toBean(p1);
        admin.get(pb1.getId());

        // delete the parent reference by setting the property to empty list
        gb1.setReferences("prop7", new ArrayList<BeanId>());

        admin.set(gb1);
        try {
            admin.delete(pb1.getId());
        } catch (AbortRuntimeException e) {
            fail("Should be possible to delete parent since grandfather "
                    + "no longer have a reference to it");
        }
    }

    @Test
    public void test_delete_bean() {
        createDefault();
        admin.delete(g1.getBeanId());
        Optional<Bean> optional = admin.get(g1.getBeanId());
        assertFalse(optional.isPresent());
    }

    @Test
    public void test_delete_beans() {
        createDefault();
        admin.delete(g1.getBeanId().getSchemaName(), Arrays.asList("g1", "g2"));
        List<Bean> result = admin.list(g1.getBeanId().getSchemaName());
        assertThat(result.size(), is(0));
    }

    @Test
    public void test_delete_reference_violation() {
        admin.create(toBeans(g1, g2, p1, p2, c1, c2));
        observer.clear();
        // test single
        try {
            admin.delete(BeanId.create("c1", CHILD_SCHEMA_NAME));
            fail("Should not be possible to delete a bean with references");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG302));
            assertThat(observer.getChanges().size(), is(0));
        }
        // test multiple
        try {
            admin.delete(CHILD_SCHEMA_NAME, Arrays.asList("c1", "c2"));
            fail("Should not be possible to delete a bean with references");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG302));
            assertThat(observer.getChanges().size(), is(0));
        }
    }

    @Test
    public void test_set_merge_without_schema() {
        Bean b = Bean.create(BeanId.create("1", "missing_schema_name"));
        try {
            admin.create(b);
            fail("Cant add beans without a schema.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG101));
            assertThat(observer.getChanges().size(), is(0));
        }
        try {
            admin.merge(b);
            fail("Cant add beans without a schema.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG101));
            assertThat(observer.getChanges().size(), is(0));
        }
    }

    @Test
    public void test_set_merge_violating_types() {
        admin.create(toBeans(g1, g2, p1, p2, c1, c2));
        observer.clear();
        Bean child = Bean.create(BeanId.create("c1", CHILD_SCHEMA_NAME));
        // child merge invalid byte
        try {
            child.setProperty("prop8", "100000");
            admin.set(child);
            fail("10000 does not fit java.lang.Byte");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
            assertThat(observer.getChanges().size(), is(0));
        }
        // child merge invalid integer
        try {
            child.addProperty("prop3", "2.2");
            admin.merge(child);
            fail("2.2 does not fit a collection of java.lang.Integer");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
            assertThat(observer.getChanges().size(), is(0));
        }
        // parent set invalid enum value
        Bean parent = Bean.create(BeanId.create("g1", GRANDFATHER_SCHEMA_NAME));
        try {
            parent.setProperty("prop14", "not_a_enum");
            admin.set(parent);
            fail("not_a_enum is not a value of TimeUnit");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
            assertThat(observer.getChanges().size(), is(0));
        }
        // parent merge invalid value to enum list
        parent = Bean.create(BeanId.create("p1", PARENT_SCHEMA_NAME));
        try {
            parent.addProperty("prop19", "not_a_enum");
            admin.merge(parent);
            fail("not_a_enum is not a value of TimeUnit");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
            assertThat(observer.getChanges().size(), is(0));
        }

        // grandfather merge invalid multiplicity type, i.e. single on multi value.
        Bean grandfather = Bean.create(BeanId.create("g1", GRANDFATHER_SCHEMA_NAME));
        try {
            grandfather.addProperty("prop1", Arrays.asList("1", "2"));
            admin.merge(grandfather);
            fail("Cannot add mutiple values to a single valued property.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG106));
            assertThat(observer.getChanges().size(), is(0));
        }

        // grandfather set invalid multiplicity type, multi value on single.
        grandfather = Bean.create(BeanId.create("p1", PARENT_SCHEMA_NAME));
        try {
            grandfather.addProperty("prop11", "2.0");
            admin.set(parent);
            fail("Cannot add a value to a single typed value.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
            assertThat(observer.getChanges().size(), is(0));
        }

    }

    @Test
    public void test_circular_references() {
        String personSchema = "person";
        config.register(Person.class);

        BeanId aId = BeanId.create("a", personSchema);
        BeanId bId = BeanId.create("b", personSchema);
        BeanId cId = BeanId.create("c", personSchema);
        BeanId dId = BeanId.create("d", personSchema);

        Bean a = Bean.create(aId);
        Bean b = Bean.create(bId);
        Bean c = Bean.create(cId);
        Bean d = Bean.create(dId);

        admin.create(Arrays.asList(a, b, c, d));

        a.setReference("bestFriend", bId);
        b.setReference("bestFriend", aId);
        c.setReference("bestFriend", dId);
        d.setReference("bestFriend", cId);

        a.addReference("closeFriends", Arrays.asList(bId, cId, dId));
        b.addReference("closeFriends", Arrays.asList(aId, cId, dId));
        c.addReference("closeFriends", Arrays.asList(aId, bId, dId));
        d.addReference("closeFriends", Arrays.asList(aId, bId, cId));

        a.addReference("colleauges", Arrays.asList(bId, cId, dId));
        b.addReference("colleauges", Arrays.asList(aId, cId, dId));
        c.addReference("colleauges", Arrays.asList(aId, bId, dId));
        d.addReference("colleauges", Arrays.asList(aId, bId, cId));
        /**
         * Now test list operations from admin and config to make
         * sure that none of them lookup stuck in infinite recrusion.
         */

        admin.merge(Arrays.asList(a, b, c, d));
        admin.set(Arrays.asList(a, b, c, d));
        admin.list("person");
        admin.get(BeanId.create("b", "person"));
        config.list(Person.class);
        config.get("c", Person.class);

    }

    private void createThenGet(Object object) throws AssertionFailedError {
        Bean bean = toBean(object);
        observer.clear();
        admin.create(bean);
        assertThat(observer.getChanges().size(), is(1));
        assertFalse(observer.isBeforePresent(object.getClass()));
        getAndAssert(object);
    }

    private void getAndAssert(Object object) throws AssertionFailedError {
        Bean bean = toBean(object);
        Bean result = admin.get(bean.getId()).get();
        assertReflectionEquals(bean, result, LENIENT_ORDER);
        runtimeGetAndAssert(object, bean);
    }

    /**
     * Create the default testdata structure.
     */
    private void createDefault() {
        observer.clear();
        admin.create(defaultBeans);
        assertThat(observer.getChanges().size(), is(defaultBeans.size()));
        observer.clear();
    }

    private void listAndAssert(String schemaName, Object... objects) {
        Collection<Bean> beans = admin.list(schemaName);
        assertReflectionEquals(toBeans(objects), beans, LENIENT_ORDER);
        runtimeAllAndAssert(objects[0].getClass(), objects);
    }

    private void runtimeGetAndAssert(Object object, Bean bean) throws AssertionFailedError {
        Object o = config.get(bean.getId().getInstanceId(), object.getClass()).get();
        assertReflectionEquals(object, o, LENIENT_ORDER);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void runtimeAllAndAssert(Class clazz, Object... objects) throws AssertionFailedError {
        List<Object> reslut = config.list(clazz);
        assertReflectionEquals(objects, reslut, LENIENT_ORDER);
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
