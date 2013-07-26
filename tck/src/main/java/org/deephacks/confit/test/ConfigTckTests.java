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
import junit.framework.AssertionFailedError;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.test.ConfigTestData.Grandfather;
import org.deephacks.confit.test.ConfigTestData.Person;
import org.deephacks.confit.test.ConfigTestData.Singleton;
import org.deephacks.confit.test.ConfigTestData.SingletonParent;
import org.deephacks.confit.test.validation.BinaryTree;
import org.deephacks.confit.test.validation.BinaryTreeUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.deephacks.confit.model.Events.*;
import static org.deephacks.confit.test.ConversionUtils.toBean;
import static org.deephacks.confit.test.ConversionUtils.toBeans;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

/**
 * A funcational set of end-to-end tests for running compatibility tests.
 *
 * Theses tests are intended to be easily reused as a test suite for simplifying
 * testing compatibility of many different combinations of service providers
 * and configurations.
 *
 * It is the responsibility of subclasses to initalize the lookup of
 * service providers and their behaviour.
 *
 */
public abstract class ConfigTckTests extends ConfigDefaultSetup {
    /**
     * This method can be used to do initalize tests in the subclass
     * before the superclass.
     */
    public abstract void before();

    @Before
    public final void beforeMethod() {
        before();
        setupDefaultConfigData();
    }

    @Test
    public void test_create_set_merge_non_existing_property() {
        createDefault();
        Bean bean = Bean.create(c1.getBeanId());
        bean.addProperty("non_existing", "bogus");
        try {
            admin.create(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
        }
        try {
            admin.set(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
        }
        try {
            admin.merge(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
        }
        try {
            bean = Bean.create(BeanId.create("c5", ConfigTestData.CHILD_SCHEMA_NAME));
            bean.setReference("non_existing", c1.getBeanId());
            admin.create(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG111));
        }
        bean = Bean.create(c1.getBeanId());
        bean.addProperty("non_existing", "bogus");

        try {
            admin.set(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
        }
        try {
            admin.merge(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
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
     * Test that get beans have their default instance created after registration.
     */
    @Test
    public void test_register_singleton() {
        Singleton singleton = config.get(Singleton.class);
        assertNotNull(singleton);
    }

    /**
     * Test that get references are automatically assigned without needing to provision them
     * from admin context.
     */
    @Test
    public void test_singleton_references() {
        // provision a bean without the get reference.
        Bean singletonParent = toBean(sp1);
        admin.create(singletonParent);

        // asert that the get reference is set for config
        SingletonParent parent = config.get(singletonParent.getId().getInstanceId(),
                SingletonParent.class).get();
        assertNotNull(parent.getSingleton());

        // assert that the get reference is set for admin
        Bean result = admin.get(singletonParent.getId()).get();
        BeanId singletonId = result.getFirstReference("get");
        assertThat(singletonId, is(s1.getBeanId()));
        assertThat(singletonId.getBean(), is(toBean(s1)));

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
    public void test_merge_get_single() {
        createDefault();

        Grandfather merged = new Grandfather("g1");
        merged.setProp14(TimeUnit.NANOSECONDS);
        merged.setProp19(Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS));
        merged.setProp1("newName");

        Bean mergeBean = toBean(merged);
        admin.merge(mergeBean);

        // modify the original to fit the expected merge
        g1.setProp1(merged.getProp1());
        g1.setProp19(merged.getProp19());
        g1.setProp14(merged.getProp14());
        getAndAssert(g1);
    }

    @Test
    public void test_merge_get_list() {
        createDefault();

        Grandfather g1_merged = new Grandfather("g1");
        g1_merged.setProp14(TimeUnit.NANOSECONDS);
        g1_merged.setProp19(Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS));
        g1_merged.setProp1("newName");

        Grandfather g2_merged = new Grandfather("g2");
        g2_merged.setProp14(TimeUnit.NANOSECONDS);
        g2_merged.setProp19(Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS));
        g2_merged.setProp1("newName");

        Collection<Bean> mergeBeans = toBeans(g1_merged, g2_merged);
        admin.merge(mergeBeans);

        // modify the original to fit the expected merge
        g1.setProp1(g1_merged.getProp1());
        g1.setProp19(g1_merged.getProp19());
        g1.setProp14(g1_merged.getProp14());

        g2.setProp1(g2_merged.getProp1());
        g2.setProp19(g2_merged.getProp19());
        g2.setProp14(g2_merged.getProp14());

        listAndAssert(g1.getBeanId().getSchemaName(), g1, g2);

    }

    @Test
    public void test_merge_and_set_broken_references() {
        createDefault();
        // try merge a invalid single reference
        Bean b = Bean.create(BeanId.create("p1", ConfigTestData.PARENT_SCHEMA_NAME));
        b.addReference("prop6", BeanId.create("non_existing_child_ref", ""));
        try {
            admin.merge(b);
            fail("Should not be possible to merge invalid reference");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301) {
                fail("Should not be possible to merge invalid reference");
            }
        }

        // try merge a invalid reference on collection
        b = Bean.create(BeanId.create("p2", ConfigTestData.PARENT_SCHEMA_NAME));
        b.addReference("prop7", BeanId.create("non_existing_child_ref", ""));
        try {
            admin.merge(b);
            fail("Should not be possible to merge invalid reference");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301) {
                fail("Should not be possible to merge invalid reference");
            }
        }

        // try set a invalid single reference
        b = Bean.create(BeanId.create("parent4", ConfigTestData.PARENT_SCHEMA_NAME));
        b.addReference("prop6", BeanId.create("non_existing_child_ref", ""));
        try {
            admin.set(b);
            fail("Should not be possible to merge beans that does not exist");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301) {
                fail("Should not be possible to merge invalid reference");
            }
        }

        // try merge a invalid single reference
        b = Bean.create(BeanId.create("p1", ConfigTestData.PARENT_SCHEMA_NAME));
        b.addReference("prop6", BeanId.create("non_existing_child_ref", ""));
        try {
            admin.set(b);
            fail("Should not be possible to merge invalid reference");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301) {
                fail("Should not be possible to merge invalid reference");
            }
        }

    }

    @Test
    public void test_delete_bean() {
        createDefault();

        admin.delete(g1.getBeanId());
        Optional<Bean> bean = admin.get(g1.getBeanId());
        assertFalse(bean.isPresent());
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
        admin.createObjects(Arrays.asList(g1, g2, p1, p2, c1, c2));
        // test single
        try {
            admin.delete(BeanId.create("c1", ConfigTestData.CHILD_SCHEMA_NAME));
            fail("Should not be possible to delete a bean with references");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG302));
        }
        // test multiple
        try {
            admin.delete(ConfigTestData.CHILD_SCHEMA_NAME, Arrays.asList("c1", "c2"));
            fail("Should not be possible to delete a bean with references");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG302));
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
        }
        try {
            admin.merge(b);
            fail("Cant add beans without a schema.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG101));
        }
    }

    @Test
    public void test_set_merge_violating_types() {
        admin.create(toBeans(g1, g2, p1, p2, c1, c2));

        Bean child = Bean.create(BeanId.create("c1", ConfigTestData.CHILD_SCHEMA_NAME));
        // child merge invalid byte
        try {
            child.setProperty("prop8", "100000");
            admin.set(child);
            fail("10000 does not fit java.lang.Byte");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
        }
        // child merge invalid integer
        try {
            child.addProperty("prop3", "2.2");
            admin.merge(child);
            fail("2.2 does not fit a collection of java.lang.Integer");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
        }
        // parent set invalid enum value
        Bean parent = Bean.create(BeanId.create("g1", ConfigTestData.GRANDFATHER_SCHEMA_NAME));
        try {
            parent.setProperty("prop14", "not_a_enum");
            admin.set(parent);
            fail("not_a_enum is not a value of TimeUnit");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
        }
        // parent merge invalid value to enum list
        parent = Bean.create(BeanId.create("p1", ConfigTestData.PARENT_SCHEMA_NAME));
        try {
            parent.addProperty("prop19", "not_a_enum");
            admin.merge(parent);
            fail("not_a_enum is not a value of TimeUnit");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
        }

        // grandfather merge invalid multiplicity type, i.e. single on multi value.
        Bean grandfather = Bean.create(BeanId.create("g1", ConfigTestData.GRANDFATHER_SCHEMA_NAME));
        try {
            grandfather.addProperty("prop1", Arrays.asList("1", "2"));
            admin.merge(grandfather);
            fail("Cannot add mutiple values to a single valued property.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG106));
        }

        // grandfather set invalid multiplicity type, multi value on single.
        grandfather = Bean.create(BeanId.create("p1", ConfigTestData.PARENT_SCHEMA_NAME));
        try {
            grandfather.addProperty("prop11", "2.0");
            admin.set(parent);
            fail("Cannot add a value to a single typed value.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
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
         * sure that none of them get stuck in infinite recrusion.
         */

        admin.merge(Arrays.asList(a, b, c, d));
        admin.set(Arrays.asList(a, b, c, d));
        admin.list("person");
        admin.get(BeanId.create("b", "person"));
        config.list(Person.class);
        config.get("c", Person.class);

    }

    @Test
    public void test_JSR303_validation_success() {
        jsr303.setProp("Valid upper value for @FirstUpperValidator");
        jsr303.setWidth(2);
        jsr303.setHeight(2);
        Bean jsr303Bean = toBean(jsr303);
        admin.create(jsr303Bean);
    }

    @Test
    public void test_JSR303_validation_failures() {
        jsr303.setProp("Valid upper value for @FirstUpperValidator");
        jsr303.setWidth(20);
        jsr303.setHeight(20);
        Bean jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Area exceeds constraint");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }

        jsr303.setProp("test");
        jsr303.setWidth(1);
        jsr303.setHeight(1);
        jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Prop does not have first upper case.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }
        jsr303.setProp("T");
        jsr303.setWidth(1);
        jsr303.setHeight(1);
        jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Prop must be longer than one char");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }

        jsr303.setProp("Valid upper value for @FirstUpperValidator");
        jsr303.setWidth(null);
        jsr303.setHeight(null);
        jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Width and height may not be null.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }
    }

    /**
     * Test that JSR303 validation can operate on references in order
     * to validate that a binary tree is correct after modification.
     *
     * This is what the test-tree looks like.
     *
     *          _______5______
     *         /              \
     *     ___4            ___8__
     *    /               /      \
     *   _2               6      10
     *  /  \               \    /
     *  1   3               7   9
     */
    @Test
    public void test_JSR303_reference_validation() {
        config.register(BinaryTree.class);

        // generate the tree seen in the javadoc
        Set<Bean> beans = BinaryTreeUtils.getTree(5, Arrays.asList(8, 4, 10, 2, 5, 1, 9, 6, 3, 7));
        admin.create(beans);
        BinaryTree root = config.get("5", BinaryTree.class).get();
        BinaryTreeUtils.printPretty(root);
        /**
         * TEST1: a correct delete of 8 where 9 takes its place
         */
        Bean eight = BinaryTreeUtils.getBean(8, beans);
        Bean ten = BinaryTreeUtils.getBean(10, beans);
        // 8: set value 9
        eight.setProperty("value", "9");
        // 10: remove reference to 9
        ten.remove("left");
        admin.set(Arrays.asList(eight, ten));

        root = config.get("5", BinaryTree.class).get();
        // take a look at the tree after delete
        BinaryTreeUtils.printPretty(root);
        /**
         * TEST2: set 4 to 7 should fail
         */
        try {
            Bean four = BinaryTreeUtils.getBean(4, beans);
            four.setProperty("value", "7");
            admin.merge(four);
            fail("setting 4 to 7 should not be possible.");
        } catch (AbortRuntimeException e) {
            // BinaryTreeValidator should notice that 7 satisfies left 2
            // but not parent 5 (7 should be to right of 5)
            assertThat(e.getEvent().getCode(), is(CFG309));
            // display error message to prove that validator found the error.
            System.out.println(e.getEvent().getMessage());
        }
    }

    private void createThenGet(Object object) throws AssertionFailedError {
        admin.createObject(object);
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
        admin.create(defaultBeans);

    }

    private void listAndAssert(String schemaName, Object... objects) {
        Collection<Bean> beans = admin.list(schemaName);
        assertReflectionEquals(toBeans(objects), beans, LENIENT_ORDER);
        runtimeAllAndAssert(objects[0].getClass(), objects);
    }

    private void runtimeGetAndAssert(Object object, Bean bean) throws AssertionFailedError {
        Object o = config.get(bean.getId().getInstanceId(), object.getClass());
        assertReflectionEquals(object, o, LENIENT_ORDER);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void runtimeAllAndAssert(Class clazz, Object... objects) throws AssertionFailedError {
        List<Object> reslut = config.list(clazz);
        assertReflectionEquals(objects, reslut, LENIENT_ORDER);
    }
}
