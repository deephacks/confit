package org.deephacks.confit.internal.jaxrs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Optional;
import org.deephacks.confit.internal.core.runtime.BeanToObjectConverter;
import org.deephacks.confit.internal.core.runtime.ClassToSchemaConverter;
import org.deephacks.confit.internal.core.runtime.DefaultBeanManager;
import org.deephacks.confit.internal.core.runtime.FieldToSchemaPropertyConverter;
import org.deephacks.confit.internal.core.runtime.ObjectToBeanConverter;
import org.deephacks.confit.jaxrs.AdminContextJaxrsProxy;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.Conversion;
import org.deephacks.confit.test.ConfigTestData.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.deephacks.confit.model.Events.CFG088;
import static org.deephacks.confit.model.Events.CFG090;
import static org.deephacks.confit.model.Events.CFG101;
import static org.deephacks.confit.test.ConfigTestData.*;
import static org.deephacks.confit.test.ConversionUtils.toBean;
import static org.deephacks.confit.test.ConversionUtils.toBeans;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

public class JaxrsConfigEndpointTest {
    private static final Conversion conversion = Conversion.get();
    static {
        conversion.register(new BeanToObjectConverter());
        conversion.register(new ObjectToBeanConverter());
        conversion.register(new ClassToSchemaConverter());
        conversion.register(new FieldToSchemaPropertyConverter());
    }
    static {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    /** Usually cast to AdminContext, but keep as proxy is nice for test debugging */
    private AdminContextJaxrsProxy admin = AdminContextJaxrsProxy.get("localhost", 8080);

    private Grandfather g1 = getGrandfather("g1");
    private Grandfather g2 = getGrandfather("g2");
    private Grandfather g3 = getGrandfather("g3");

    private Parent p1 = getParent("p1");
    private Parent p2 = getParent("p2");
    private Parent p3 = getParent("p3");

    private Collection<Grandfather> grandfathers = Arrays.asList(g1, g2, g3);
    private Collection<Parent> parents = Arrays.asList(p1, p2, p3);
    private Collection<Bean> grandfathersBeans;
    private Collection<Bean> parentBeans;

    @BeforeClass
    public static void beforeClass(){
        JettyServer.start();
    }

    @Before
    public void before(){
        DefaultBeanManager.clear();
        grandfathersBeans = toBeans(g1, g2, g3);
        parentBeans = toBeans(p1, p2, p3);
    }

    @Test
    public void testGetBean() {
        Optional<Bean> optional = admin.get(BeanId.create("bogus", "bogus"));
        assertFalse(optional.isPresent());
        Bean bg1 = toBean(g1);
        admin.create(bg1);
        Optional<Bean> bean = admin.get(g1.getBeanId());
        assertTrue(bean.isPresent());
        assertReflectionEquals(bg1, bean.get(), LENIENT_ORDER);
    }

    @Test
    public void testGetSingleton() {
        Optional<Singleton> optional = admin.get(Singleton.class);
        assertFalse(optional.isPresent());
        Singleton s = new Singleton();
        Bean bs = toBean(s);
        admin.create(bs);
        Optional<Bean> bean = admin.get(s.getBeanId());
        assertTrue(bean.isPresent());
        assertReflectionEquals(bs, bean.get(), LENIENT_ORDER);
    }

    @Test
    public void testGetObject() {
        Optional<Grandfather> optional = admin.get(Grandfather.class, "g1");
        assertFalse(optional.isPresent());
        admin.createObject(g1);
        optional = admin.get(Grandfather.class, "g1");
        assertTrue(optional.isPresent());
        assertReflectionEquals(toBean(g1), toBean(optional.get()), LENIENT_ORDER);
    }

    @Test
    public void testListBean(){
        List<Bean> beans = admin.list(GRANDFATHER_SCHEMA_NAME);
        assertThat(beans.size(), is(0));
        admin.create(grandfathersBeans);
        beans = admin.list(GRANDFATHER_SCHEMA_NAME);
        assertThat(beans.size(), is(3));
        assertReflectionEquals(grandfathersBeans, beans, LENIENT_ORDER);
        try {
            admin.list("bogus");
            fail("Cannot list unregistered schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG101));
        }
    }

    @Test
    public void testListObjects() {
        Collection<Parent> objects = admin.list(Parent.class);
        assertThat(objects.size(), is(0));
        admin.createObjects(parents);
        objects = admin.list(Parent.class);
        assertThat(objects.size(), is(3));
        assertReflectionEquals(parentBeans, toBeans(objects.toArray(new Object[objects.size()])), LENIENT_ORDER);
        try {
            admin.list(String.class);
            fail("Cannot list unregistered schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG101));
        }
    }

    @Test
    public void testListBeans() {
        try {
            admin.list("schemaName", Arrays.asList("c1", "c2"));
            fail("Cannot list unregistered schema");
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101/CDG304
            assertThat(e.getEvent().getCode(), is(CFG090));
        }
        admin.create(grandfathersBeans);
        Collection<Bean> beans = admin.list(GRANDFATHER_SCHEMA_NAME, Arrays.asList("g1", "g2"));
        assertThat(beans.size(), is(2));
        Collection<Bean> expected = toBeans(g1, g2);
        assertReflectionEquals(expected, beans, LENIENT_ORDER);
    }

    @Test
    public void testCreateBean() {
        // tested from get tests
    }

    @Test
    public void testCreateObject() {
        // tested from get tests
    }

    @Test
    public void testCreateBeans() {
        // tested from list tests
    }

    @Test
    public void testCreateObjects() {
        // tested from list tests
    }

    @Test
    public void testSetBean() {
        try {
            admin.set(Bean.create(BeanId.create("bogus", "bogus")));
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101/CDG304
            assertThat(e.getEvent().getCode(), is(CFG090));
        }

        Grandfather g1 = new Grandfather("g1");
        Bean bg1 = toBean(g1);
        admin.create(bg1);
        bg1.setProperty("prop1", "testSetBean");
        admin.set(bg1);
        Optional<Bean> optional = admin.get(bg1.getId());
        assertTrue(optional.isPresent());
        // assert new value
        String value = optional.get().getSingleValue("prop1");
        assertThat(value, is("testSetBean"));
        // assert that other values have been reset
        assertNull(optional.get().getSingleValue("prop9"));
        assertNull(optional.get().getSingleValue("prop2"));
    }

    @Test
    public void testSetObject() {
        try {
            admin.setObject("");
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101
            assertThat(e.getEvent().getCode(), is(CFG088));
        }
        assertFalse(admin.get(Grandfather.class, "g1").isPresent());
        admin.createObject(g1);
        g1 = new Grandfather("g1");
        g1.setProp1("testSetObject");
        admin.setObject(g1);
        Optional<Grandfather> optional = admin.get(Grandfather.class, "g1");
        assertTrue(optional.isPresent());
        assertThat(optional.get().getProp1(), is("testSetObject"));
        assertNull(optional.get().getProp9());
        assertNull(optional.get().getProp2());
    }

    @Test
    public void testSetBeans() {
        try {
            admin.set(Arrays.asList(Bean.create(BeanId.create("bogus", "bogus"))));
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101/CDG304
            assertThat(e.getEvent().getCode(), is(CFG090));
        }
        admin.create(grandfathersBeans);

        // assert successful create
        Optional<Bean> optional = admin.get(BeanId.create("g1", GRANDFATHER_SCHEMA_NAME));
        assertTrue(optional.isPresent());
        assertNotNull(optional.get().getSingleValue("prop9"));
        assertNotNull(optional.get().getSingleValue("prop2"));

        Grandfather g1 = new Grandfather("g1");
        g1.setProp1("testSetBeans1");
        Grandfather g2 = new Grandfather("g2");
        g2.setProp1("testSetBeans2");
        Collection<Bean> beans = toBeans(g1, g2);
        admin.set(beans);

        // test g1
        optional = admin.get(BeanId.create("g1", GRANDFATHER_SCHEMA_NAME));
        assertTrue(optional.isPresent());
        // assert that new value have been set
        assertThat(optional.get().getSingleValue("prop1"), is("testSetBeans1"));
        // assert that previously set value have been reset
        assertNull(optional.get().getSingleValue("prop9"));

        // test g2
        optional = admin.get(BeanId.create("g2", GRANDFATHER_SCHEMA_NAME));
        assertTrue(optional.isPresent());
        // assert that new value have been set
        assertThat(optional.get().getSingleValue("prop1"), is("testSetBeans2"));
        // assert that previously set value have been reset
        assertNull(optional.get().getSingleValue("prop2"));
    }

    @Test
    public void testSetObjects() {
        try {
            admin.setObjects(Arrays.asList(new Grandfather("bogus")));
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101/CDG304
            assertThat(e.getEvent().getCode(), is(CFG090));
        }

        admin.createObjects(grandfathers);

        // assert successful create
        Optional<Grandfather> optional = admin.get(Grandfather.class, "g1");
        assertTrue(optional.isPresent());

        Grandfather g1 = new Grandfather("g1");
        g1.setProp1("testSetObjects1");
        Grandfather g2 = new Grandfather("g2");
        g2.setProp1("testSetObjects2");
        admin.setObjects(Arrays.asList(g1, g2));

        // test g1
        optional = admin.get(Grandfather.class, "g1");
        assertTrue(optional.isPresent());
        // assert that new value have been set
        assertThat(optional.get().getProp1(), is("testSetObjects1"));
        // assert that previously set value have been reset
        assertNull(optional.get().getProp9());

        // test g2
        optional = admin.get(Grandfather.class, "g2");
        assertTrue(optional.isPresent());
        // assert that new value have been set
        assertThat(optional.get().getProp1(), is("testSetObjects2"));
        // assert that previously set value have been reset
        assertNull(optional.get().getProp2());

    }

    @Test
    public void testMergeBean() {
        try {
            admin.merge(Bean.create(BeanId.create("bogus", "bogus")));
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101/CDG304
            assertThat(e.getEvent().getCode(), is(CFG090));
        }

        Grandfather g1 = getGrandfather("g1");
        Bean bg1 = toBean(g1);
        admin.create(bg1);
        // merge in a single property
        g1 = new Grandfather("g1");
        bg1 = toBean(g1);
        bg1.setProperty("prop1", "testMergeBean");
        admin.merge(bg1);
        Optional<Bean> optional = admin.get(bg1.getId());
        assertTrue(optional.isPresent());
        // assert new value
        String value = optional.get().getSingleValue("prop1");
        assertThat(value, is("testMergeBean"));
        // assert that other values are untouched
        assertNotNull(optional.get().getSingleValue("prop9"));
        assertNotNull(optional.get().getSingleValue("prop2"));
    }

    @Test
    public void mergeObject()  {
        try {
            admin.mergeObject("");
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101
            assertThat(e.getEvent().getCode(), is(CFG088));
        }
        assertFalse(admin.get(Grandfather.class, "g1").isPresent());
        admin.createObject(g1);
        g1 = new Grandfather("g1");
        g1.setProp1("testSetObject");
        admin.mergeObject(g1);
        Optional<Grandfather> optional = admin.get(Grandfather.class, "g1");
        assertTrue(optional.isPresent());
        assertThat(optional.get().getProp1(), is("testSetObject"));
        assertNotNull(optional.get().getProp9());
        assertNotNull(optional.get().getProp2());
    }

    @Test
    public void testMergeBeans()  {
        try {
            admin.merge(Bean.create(BeanId.create("bogus", "bogus")));
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101/CDG304
            assertThat(e.getEvent().getCode(), is(CFG090));
        }
        admin.create(grandfathersBeans);

        // assert successful create
        Optional<Bean> optional = admin.get(BeanId.create("g1", GRANDFATHER_SCHEMA_NAME));
        assertTrue(optional.isPresent());
        String prop9 = optional.get().getSingleValue("prop9");
        String prop2 = optional.get().getSingleValue("prop2");

        Grandfather g1 = new Grandfather("g1");
        g1.setProp1("testMergeBean1");
        Grandfather g2 = new Grandfather("g2");
        g2.setProp1("testMergeBean2");
        Collection<Bean> beans = toBeans(g1, g2);
        admin.merge(beans);

        // test g1
        optional = admin.get(BeanId.create("g1", GRANDFATHER_SCHEMA_NAME));
        assertTrue(optional.isPresent());
        // assert that new value have been set
        assertThat(optional.get().getSingleValue("prop1"), is("testMergeBean1"));
        // assert that previous values are untouched
        assertThat(optional.get().getSingleValue("prop9"), is(prop9));
        assertThat(optional.get().getSingleValue("prop2"), is(prop2));

        // test g2
        optional = admin.get(BeanId.create("g2", GRANDFATHER_SCHEMA_NAME));
        assertTrue(optional.isPresent());
        // assert that new value have been set
        assertThat(optional.get().getSingleValue("prop1"), is("testMergeBean2"));
        // assert that previous values are untouched
        assertThat(optional.get().getSingleValue("prop9"), is(prop9));
        assertThat(optional.get().getSingleValue("prop2"), is(prop2));
    }

    @Test
    public void testMergeObjects() {
        try {
            admin.mergeObjects(Arrays.asList(new Grandfather("bogus")));
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101/CDG304
            assertThat(e.getEvent().getCode(), is(CFG090));
        }

        admin.createObjects(grandfathers);

        // assert successful create
        Optional<Grandfather> optional = admin.get(Grandfather.class, "g1");
        assertTrue(optional.isPresent());

        Grandfather g1 = new Grandfather("g1");
        g1.setProp1("testSetObjects1");
        Grandfather g2 = new Grandfather("g2");
        g2.setProp1("testSetObjects2");
        admin.mergeObjects(Arrays.asList(g1, g2));

        // test g1
        optional = admin.get(Grandfather.class, "g1");
        assertTrue(optional.isPresent());
        // assert that new value have been set
        assertThat(optional.get().getProp1(), is("testSetObjects1"));
        // assert that previously set value have been reset
        assertNotNull(optional.get().getProp9());

        // test g2
        optional = admin.get(Grandfather.class, "g2");
        assertTrue(optional.isPresent());
        // assert that new value have been set
        assertThat(optional.get().getProp1(), is("testSetObjects2"));
        // assert that previously set value have been reset
        assertNotNull(optional.get().getProp2());


    }

    @Test
    public void testDeleteBean() {
        BeanId id = BeanId.create("g1", GRANDFATHER_SCHEMA_NAME);
        try {
            admin.delete(id);
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101/CDG304
            assertThat(e.getEvent().getCode(), is(CFG090));
        }

        admin.create(toBean(g1));
        Optional<Bean> optional = admin.get(id);
        assertTrue(optional.isPresent());
        admin.delete(id);
        optional = admin.get(id);
        assertFalse(optional.isPresent());
    }

    @Test
    public void testDeleteBeans() {
        try {
            admin.delete(GRANDFATHER_SCHEMA_NAME, Arrays.asList("g1", "g2"));
        } catch (AbortRuntimeException e) {
            // don't know if schema or instances were invalid from
            // a HTTP 404 response, hence CFG090 and not CFG101/CDG304
            assertThat(e.getEvent().getCode(), is(CFG090));
        }

        admin.create(grandfathersBeans);
        Collection<Bean> beans = admin.list(GRANDFATHER_SCHEMA_NAME);
        assertThat(beans.size(), is(grandfathersBeans.size()));
        admin.delete(GRANDFATHER_SCHEMA_NAME, Arrays.asList("g1", "g2"));

        Optional<Bean> optional = admin.get(BeanId.create("g1", GRANDFATHER_SCHEMA_NAME));
        assertFalse(optional.isPresent());

        optional = admin.get(BeanId.create("g2", GRANDFATHER_SCHEMA_NAME));
        assertFalse(optional.isPresent());

        optional = admin.get(BeanId.create("g3", GRANDFATHER_SCHEMA_NAME));
        assertTrue(optional.isPresent());
    }

    @Test
    public void testGetSchemas() {
        final Map<String,Schema> schemas = admin.getSchemas();
        assertThat(schemas.values().size(), is(5));
        for (Schema schema : schemas.values()) {
            Schema expected = conversion.convert(schema.getClassType(), Schema.class);
            assertReflectionEquals(expected, schema, LENIENT_ORDER);
        }
    }

    @Test
    public void testGetSchema() {
        Optional<Schema> optional = admin.getSchema("bogus");
        assertFalse(optional.isPresent());
        optional = admin.getSchema(GRANDFATHER_SCHEMA_NAME);
        assertTrue(optional.isPresent());
        Schema schema = optional.get();
        Schema expected = conversion.convert(Grandfather.class, Schema.class);
        assertReflectionEquals(expected, schema, LENIENT_ORDER);
    }

    @Test
    public void testQuery() {
        /*
        admin.newQuery("bogus").retrieve();
        */
    }
}
