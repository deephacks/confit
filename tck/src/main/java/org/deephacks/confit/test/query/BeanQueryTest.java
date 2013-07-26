package org.deephacks.confit.test.query;


import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.test.ConfigTestData.*;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.deephacks.confit.admin.query.BeanQueryBuilder.*;
import static org.deephacks.confit.test.ConfigTestData.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

@RunWith(FeatureTestsRunner.class)
public class BeanQueryTest {
    private AdminContext admin = AdminContext.get();
    private ConfigContext config = ConfigContext.get();

    private List<Grandfather> grandfathers = new ArrayList<>();
    private List<Parent> parents = new ArrayList<>();
    private List<Child> children = new ArrayList<>();

    @Before
    public void setup() {
        config.register(Grandfather.class, Parent.class, Child.class);

        Parent p1 = getParent("p1");
        Parent p2 = getParent("p2");
        Parent p3 = getParent("p3");
        Parent p4 = getParent("p4");
        Parent p5 = getParent("p5");
        admin.createObjects(Arrays.asList(p1, p2, p3, p4, p5));

        Grandfather g1 = getGrandfather("g1");
        g1.setProp1("test");
        g1.setProp12(1.0);
        g1.setProp3(Arrays.asList(1, 2, 3));
        g1.setProp7(Arrays.asList(p1, p2, p3, p4, p5));
        Grandfather g2 = getGrandfather("g2");
        g2.setProp12(333.333);
        g2.setProp3(Arrays.asList(3, 4, 5));
        g2.setProp7(Arrays.asList(p2, p3, p4, p5));
        Grandfather g3 = getGrandfather("g3");
        g3.setProp12(3.0);
        g3.setProp3(Arrays.asList(3, 4, 6));
        g3.setProp7(Arrays.asList(p3, p4, p5));
        Grandfather g4 = getGrandfather("g4");
        g4.setProp12(4.0);
        g4.setProp3(Arrays.asList(6, 7, 8));
        g4.setProp7(Arrays.asList(p4, p5));
        Grandfather g5 = getGrandfather("g5");
        g5.setProp12(5.0);
        g5.setProp3(Arrays.asList(8, 9, 10));
        g5.setProp7(Arrays.asList(p5));

        admin.createObjects(Arrays.asList(g1, g2, g3, g4, g5));
    }

    @Test
    public void test_select_all() {
        List<Bean> list = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .retrieve();
        assertThat(list.size(), is(5));
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : list) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[] {"g1", "g2", "g3", "g4", "g5"}));
    }

    /**
     * Include instances that have a field that equal a certain value.
     */
    @Test
    public void test_single_equal() {
        List<Bean> list = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(equal("prop1", "test"))
                .retrieve();
        assertThat(list.size(), is(1));
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : list) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[] {"g1"}));
    }

    @Test
    public void test_single_equal_number() {
        List<Bean> list = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(equal("prop12", 333.333))
                .retrieve();
        assertThat(list.size(), is(1));
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : list) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[] {"g2"}));
    }

    /**
     * Include instances that have a field that does not equal a certain value.
     */
    @Test
    public void test_single_not_equal() {
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(not(equal("prop1", "value")))
                .retrieve();
        assertThat(result.size(), is(1));
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[] {"g1"}));
    }


    /**
     * Include instances that have a field that contain a certain value.
     */
    @Test
    public void test_single_contains() {
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(contains("prop1", "val"))
                .retrieve();
        assertThat(result.size(), is(4));
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[] {"g2","g3","g4","g5"}));
    }

    /**
     * Include instances that have a field that does not than a certain value.
     */
    @Test
    public void test_not_contains() {
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(not(contains("prop1", "val")))
                .retrieve();
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(result.size(), is(1));
        assertThat(ids, hasItems(new String[] {"g1"}));
    }

    @Test
    public void test_single_greaterThan() {
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(greaterThan("prop12", 4.0))
                .retrieve();
        assertThat(result.size(), is(2));
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[]{"g2", "g5"}));
    }

    @Test
    public void test_single_lessThan() {
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(lessThan("prop12", 2.0))
                .retrieve();
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getId().getInstanceId(), is("g1"));
    }


    @Test
    public void test_lessThan_and_contains() {
        try {
            List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                    .add(lessThan("prop12", 1000.0))
                    .add(contains("prop1", "tes"))
                    .retrieve();
            assertThat(result.size(), is(1));
            assertThat(result.get(0).getId().getInstanceId(), is("g1"));
        } catch(Exception e) {
            if (e.getMessage().contains("Invalid character string format for type DECIMAL")){
                // Exception thrown by derby. seems to happen when writing queries with multiple conditions
                // and casts in them like the following query:

                // SELECT b.BEAN_ID FROM CONFIG_BEAN b
                // INNER JOIN CONFIG_PROPERTY p0 ON b.BEAN_ID = p0.FK_BEAN_ID
                // INNER JOIN CONFIG_PROPERTY p1 ON b.BEAN_ID = p1.FK_BEAN_ID
                // WHERE b.BEAN_SCHEMA_NAME='GrandfatherSchemaName'
                // AND (p0.prop_name='prop1' AND p0.PROP_VALUE NOT LIKE '%t%')
                // AND (p1.prop_name='prop12' AND CAST(p1.prop_value AS DECIMAL) < 1000.0)
                // GROUP BY b.BEAN_ID
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void test_lessThan_and_not_contains() {
        try {
            List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                    .add(not(contains("prop1", "t")))
                    .add(lessThan("prop12", 1000.0))
                    .retrieve();

            assertThat(result.size(), is(4));
            ArrayList<String> ids = new ArrayList<>();
            for (Bean g : result) {
                ids.add(g.getId().getInstanceId());
            }
            assertThat(ids, hasItems(new String[]{"g2", "g3", "g4", "g5", }));
        } catch(Exception e) {
            if (e.getMessage().contains("Invalid character string format for type DECIMAL")){
                // Exception thrown by derby. seems to happen when writing queries with multiple conditions
                // and casts in them like the following query:

                // SELECT b.BEAN_ID FROM CONFIG_BEAN b
                // INNER JOIN CONFIG_PROPERTY p0 ON b.BEAN_ID = p0.FK_BEAN_ID
                // INNER JOIN CONFIG_PROPERTY p1 ON b.BEAN_ID = p1.FK_BEAN_ID
                // WHERE b.BEAN_SCHEMA_NAME='GrandfatherSchemaName'
                // AND (p0.prop_name='prop1' AND p0.PROP_VALUE NOT LIKE '%t%')
                // AND (p1.prop_name='prop12' AND CAST(p1.prop_value AS DECIMAL) < 1000.0)
                // GROUP BY b.BEAN_ID
            } else {
                throw new RuntimeException(e);
            }
        }
    }


    @Test
    public void test_in_query() {
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(in("prop3", 3, 4))
                .retrieve();
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(result.size(), is(2));
        assertThat(ids, hasItems(new String[]{"g2", "g3"}));
    }

    /**
     * FIXME: negating IN queries does not work at the moment.
     */
/*
    @Test
    public void test_not_in_query() {
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(not(in("prop3", 3, 4)))
                .retrieve();
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getBeanId().getInstanceId());
        }
        assertThat(result.size(), is(2));
        assertThat(ids, hasItems(new String[] {"g1", "g4", "g5"}));
    }
*/

    @Test
    public void test_query_references() {
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(contains("prop7", "p1"))
                .retrieve();
        assertThat(result.size(), is(1));
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[]{"g1"}));

        result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(contains("prop7", "p2"))
                .retrieve();
        assertThat(result.size(), is(2));
        ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[]{"g1", "g2"}));

        result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(contains("prop7", "p3"))
                .retrieve();
        assertThat(result.size(), is(3));
        ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[]{"g1", "g2", "g3"}));
    }

    @Test
    public void test_query_references_and_properties() {
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(contains("prop7", "p5"))
                .add(contains("prop1", "t"))
                .retrieve();
        assertThat(result.size(), is(1));
        ArrayList<String> ids = new ArrayList<>();
        for (Bean g : result) {
            ids.add(g.getId().getInstanceId());
        }
        assertThat(ids, hasItems(new String[]{"g1"}));

    }

    @Test
    public void test_pagination() {
        List<String> seen = new ArrayList<>();
        List<Bean> result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                .add(contains("prop7", "p5"))
                .setFirstResult(0)
                .setMaxResults(1)
                .retrieve();
        assertThat(result.size(), is(1));
        String instanceId = result.get(0).getId().getInstanceId();
        seen.add(instanceId);
        for (int i = 1; i < 5; i++) {
            result = admin.newQuery(GRANDFATHER_SCHEMA_NAME)
                    .add(contains("prop7", "p5"))
                    .setFirstResult(i)
                    .setMaxResults(1)
                    .retrieve();
            assertThat(result.size(), is(1));
            instanceId = result.get(0).getId().getInstanceId();
            assertTrue(!seen.contains(instanceId));
            seen.add(instanceId);
        }
    }
}
