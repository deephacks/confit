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
package org.deephacks.confit.internal.cached;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.internal.core.runtime.DefaultBeanManager;
import org.deephacks.confit.query.ConfigResultSet;
import org.deephacks.confit.test.ConfigTestData.Child;
import org.deephacks.confit.test.ConfigTestData.Grandfather;
import org.deephacks.confit.test.ConfigTestData.Parent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.deephacks.confit.internal.core.runtime.ConversionUtils.toBean;
import static org.deephacks.confit.query.ConfigQueryBuilder.*;
import static org.deephacks.confit.test.ConfigTestData.getGrandfather;
import static org.deephacks.confit.test.ConfigTestData.getParent;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class ConfigQueryTest {
    static {
        System.setProperty("confit.query.enabled", "true");
    }
    static final ConfigContext runtime = ConfigContext.get();
    static final AdminContext admin = AdminContext.get();
    private Grandfather g1;
    private Grandfather g2;
    private Grandfather g3;
    private Grandfather g4;

    private Parent p1;
    private Parent p2;

    static {
        runtime.register(Grandfather.class, Parent.class, Child.class);
    }

    @Before
    public void before() {
        DefaultBeanManager.clear();
        runtime.register(Grandfather.class, Parent.class, Child.class);
        g1 = getGrandfather("g1");
        g1.setProp1("value");
        g1.setProp3(Arrays.asList(1, 2, 3));
        g1.setProp12(1.0);

        g2 = getGrandfather("g2");
        g2.setProp1("test");
        g2.setProp3(Arrays.asList(3, 4, 5));
        g2.setProp12(2.0);

        g3 = getGrandfather("g3");
        g3.setProp1("value");
        g3.setProp3(Arrays.asList(5, 6, 7));
        g3.setProp12(3.0);

        g4 = getGrandfather("g4");
        g4.setProp1(null);
        g4.setProp3(Arrays.asList(7, 8, 9));
        g4.setProp12(null);

        p1 = getParent("p1");
        p2 = getParent("p2");

        g3.setProp7(Arrays.asList(p1, p2));

        admin.create(toBean(p1));
        admin.create(toBean(p2));

        admin.create(toBean(g1));
        admin.create(toBean(g2));
        admin.create(toBean(g3));
        admin.create(toBean(g4));
    }

    @After
    public void after() {
        // clear index and cache data for schema
        runtime.unregister(Grandfather.class);
    }

    /**
     * Include instances that have a field that equal a certain value.
     */
    @Test
    public void test_single_equal() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(equal("prop1", "test"))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        assertThat(list.size(), is(1));
        ArrayList<String> ids = new ArrayList<>();
        for (Grandfather g : list) {
            ids.add(g.getId());
        }
        assertThat(ids, hasItems(new String[]{"g2"}));
    }

    /**
     * Include instances that have a field that does not equal a certain value.
     */
    @Test
    public void test_single_not_equal() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(not(equal("prop1", "test")))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        assertThat(list.size(), is(3));
        ArrayList<String> ids = new ArrayList<>();
        for (Grandfather g : list) {
            ids.add(g.getId());
        }
        assertThat(ids, hasItems(new String[]{"g1", "g3", "g4"}));
    }


    /**
     * Include instances that have a field that contain a certain value.
     */
    @Test
    public void test_single_contains() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(contains("prop1", "val"))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        assertThat(list.size(), is(2));
        ArrayList<String> ids = new ArrayList<>();
        for (Grandfather g : list) {
            ids.add(g.getId());
        }
        assertThat(ids, hasItems(new String[]{"g1", "g3"}));
    }

    /**
     * Include instances that have a field that does not than a certain value.
     */
    @Test
    public void test_not_contains() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(not(contains("prop1", "value")))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        ArrayList<String> ids = new ArrayList<>();
        for (Grandfather g : list) {
            ids.add(g.getId());
        }
        assertThat(list.size(), is(2));
        assertThat(ids, hasItems(new String[]{"g2", "g4"}));
    }

    /**
     * Include instances that have a field greater than a certain value.
     */
    @Test
    public void test_single_greaterThan() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(greaterThan("prop12", 2.0))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getId(), is("g3"));
    }

    /**
     * Include instances that have a field less than a certain value.
     */
    @Test
    public void test_single_lessThan() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(lessThan("prop12", 2.0))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getId(), is("g1"));
    }

    /**
     * Include instances that have a field less than and other field contain a certain value.
     */
    @Test
    public void test_lessThan_and_contains() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(and(
                        lessThan("prop12", 2.0),
                        contains("prop1", "value")))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getId(), is("g1"));
    }


    /**
     * Include instances that have a field less than and other field that does not contain
     * a certain value.
     */
    @Test
    public void test_lessThan_and_not_contains() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(and(
                        lessThan("prop12", 3.0),
                        not(contains("prop1", "value"))))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);

        assertThat(list.size(), is(1));
        assertThat(list.get(0).getId(), is("g2"));
    }

    /**
     * Include instances that have a field great than or other field contain a certain value.
     */
    @Test
    public void test_greaterThan_or_contains() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(or(
                        greaterThan("prop12", 1.0),
                        contains("prop1", "value")))
                .retrieve();
        // framework does not do decuplication atm
        // so do it manually with a hashset
        Set<Grandfather> list = Sets.newHashSet(result);
        Set<String> ids = new HashSet<>();
        for (Grandfather g : list) {
            ids.add(g.getId());
        }
        assertThat(ids.size(), is(3));
        assertThat(ids, hasItems(new String[]{"g1", "g2", "g3"}));
    }

    /**
     * Include instances that have a null field.
     */
    @Test
    public void test_not_has() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(not(has("prop12")))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getId(), is("g4"));
    }

    /**
     * Include instances that have certain values in list field.
     */
    @Test
    public void test_in_query() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(in("prop3", 1, 7))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        ArrayList<String> ids = new ArrayList<>();
        for (Grandfather g : list) {
            ids.add(g.getId());
        }
        assertThat(list.size(), is(3));
        assertThat(ids, hasItems(new String[]{"g1", "g3", "g4"}));
    }

    /**
     * Exclude instances that does not have certain values in list field.
     */
    @Test
    public void test_not_in_query() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(not(in("prop3", 1, 8)))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        ArrayList<String> ids = new ArrayList<>();
        for (Grandfather g : list) {
            ids.add(g.getId());
        }
        assertThat(list.size(), is(2));
        assertThat(ids, hasItems(new String[]{"g2", "g3"}));
    }

    /**
     * Test that queries can match instance id reference lists.
     */
    @Test
    public void test_query_references() {
        ConfigResultSet<Grandfather> result = runtime.newQuery(Grandfather.class)
                .add(contains("prop7", "p1"))
                .retrieve();
        List<Grandfather> list = Lists.newArrayList(result);
        assertThat(list.size(), is(1));
        Grandfather found = list.get(0);
        List<String> parentIds = new ArrayList<>();
        for (Parent parent : found.getProp7()) {
            parentIds.add(parent.getId());
        }

        assertEquals(found.getId(), "g3");
        assertThat(parentIds, hasItems(new String[]{"p1", "p2"}));
    }
}