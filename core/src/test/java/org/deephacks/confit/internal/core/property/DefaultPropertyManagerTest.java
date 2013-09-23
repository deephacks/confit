package org.deephacks.confit.internal.core.property;

import com.google.common.base.Optional;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaId;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.test.JUnitUtils;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.matchers.IsCollectionContaining;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultPropertyManagerTest {
    static DefaultPropertyManager properties;
    static {
        File dir = JUnitUtils.computeMavenProjectRoot(DefaultPropertyManagerTest.class);
        File conf = new File(dir, "src/test/resources/test.conf");
        properties = new DefaultPropertyManager(conf);
    }

    private Schema singleton;
    private Schema instances;
    private Schema references;

    private String SINGLETON_SCHEMA = "singleton";
    private String INSTANCES_SCHEMA = "instances";
    private String REFERENCES_SCHEMA = "references";

    private String SINGLE_PROPERTY_NAME = "single.property";
    private String SINGLE_PROPERTY_VALUE = "single.value";

    private String LIST_PROPERTY_NAME = "list.property";
    private String[] LIST_PROPERTY_VALUES = new String[] {"1", "2"};

    private String SINGLE_REF_NAME = "single.ref";
    private BeanId SINGLE_REF_VALUE = BeanId.create("1", REFERENCES_SCHEMA);

    private String LIST_REF_NAME = "list.ref";
    private BeanId[] LIST_REF_VALUES = new BeanId[] { BeanId.create("1", REFERENCES_SCHEMA) , BeanId.create("1", REFERENCES_SCHEMA)};
    private Map<String, Schema> schemas = new HashMap<>();

    @Before
    public void before() {
        SchemaProperty singleProperty = SchemaProperty.create(SINGLE_PROPERTY_NAME, "", String.class.getName(),
                "", false, new ArrayList<String>(), "", false);

        SchemaPropertyList listProperty = SchemaPropertyList.create(LIST_PROPERTY_NAME, "", Integer.class.getName(),
                "", false, new ArrayList<String>(), new ArrayList<String>(), List.class.getName(), false);

        SchemaPropertyRef refProperty = SchemaPropertyRef.create(SINGLE_REF_NAME, "", REFERENCES_SCHEMA,
                String.class, "", false, false, false);

        SchemaPropertyRefList refListProperty = SchemaPropertyRefList.create(LIST_REF_NAME, "", REFERENCES_SCHEMA,
                String.class, "", false, List.class.getName(), false);

        singleton = Schema.create(SchemaId.create(SINGLETON_SCHEMA, "", true), "", SINGLETON_SCHEMA, "");
        singleton.add(singleProperty);
        singleton.add(listProperty);
        singleton.add(refProperty);
        singleton.add(refListProperty);

        instances = Schema.create(SchemaId.create(INSTANCES_SCHEMA, "", false), "", INSTANCES_SCHEMA, "");
        instances.add(singleProperty);
        instances.add(listProperty);
        instances.add(refProperty);
        instances.add(refListProperty);

        references = Schema.create(SchemaId.create(REFERENCES_SCHEMA, "", false), "", REFERENCES_SCHEMA, "");
        schemas.put(references.getName(), references);
        schemas.put(singleton.getName(), singleton);
        schemas.put(instances.getName(), instances);
    }

    @Test
    public void test_list_singleton() {
        List<Bean> beans = properties.list(singleton, schemas);
        Assert.assertThat(beans.size(), Is.is(1));
        Bean bean = beans.get(0);
        assertBean(bean);
    }

    @Test
    public void test_list_instances() {
       List<Bean> beans = properties.list(instances, schemas);
       Assert.assertThat(beans.size(), Is.is(2));
       for (Bean bean : beans) {
           assertBean(bean);
       }
    }

    @Test
    public void test_regular_property() {
        Optional<String> value = properties.get(SINGLE_PROPERTY_NAME);
        Assert.assertTrue(value.isPresent());
        Assert.assertThat(value.get(), Is.is(SINGLE_PROPERTY_VALUE));
    }

    @Test
    public void test_get_singleton_beanid() {
        Optional<Bean> bean = properties.get(BeanId.createSingleton(SINGLETON_SCHEMA), schemas);
        Assert.assertTrue(bean.isPresent());
        assertBean(bean.get());
    }

    @Test
    public void test_get_instance_beanid() {
        Optional<Bean> bean = properties.get(BeanId.create("1", INSTANCES_SCHEMA), schemas);
        Assert.assertTrue(bean.isPresent());
        assertBean(bean.get());

        bean = properties.get(BeanId.create("2", INSTANCES_SCHEMA), schemas);
        Assert.assertTrue(bean.isPresent());
        assertBean(bean.get());
    }

    private void assertBean(Bean bean) {
        Assert.assertThat(bean.getSingleValue(SINGLE_PROPERTY_NAME), Is.is(SINGLE_PROPERTY_VALUE));
        Assert.assertThat(bean.getValues(LIST_PROPERTY_NAME), IsCollectionContaining.hasItems(LIST_PROPERTY_VALUES));
        Assert.assertThat(bean.getFirstReference(SINGLE_REF_NAME), Is.is(SINGLE_REF_VALUE));
        Assert.assertThat(bean.getReference(LIST_REF_NAME), IsCollectionContaining.hasItems(LIST_REF_VALUES));
    }

}
