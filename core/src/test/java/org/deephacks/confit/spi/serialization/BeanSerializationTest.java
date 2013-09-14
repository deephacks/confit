package org.deephacks.confit.spi.serialization;

import com.google.common.collect.ImmutableMap;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.test.ConfigTestData;
import org.deephacks.confit.test.ConfigTestData.Child;
import org.deephacks.confit.test.ConfigTestData.Grandfather;
import org.deephacks.confit.test.ConfigTestData.Parent;
import org.junit.Test;

import java.util.List;

import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

public class BeanSerializationTest {
    private static final SchemaManager schemaManager = SchemaManager.lookup();
    private static final InMemoryUniqueId propertyIds = new InMemoryUniqueId();
    static {
        schemaManager.register(Child.class, Parent.class, Grandfather.class);
    }

    @Test
    public void test_basic_serialization() {
        Bean bean = ConfigTestData.getChild("bean").toBean();
        BeanSerialization serialization = new BeanSerialization(propertyIds);
        byte[] data = serialization.write(bean);
        Bean result = serialization.read(data, bean.getId(), bean.getSchema());
        assertReflectionEquals(bean, result, LENIENT_ORDER);
    }

    @Test
    public void test_single_level_reference_serialization() {
        Parent parent = getParentWithReferences("parent");
        Bean bean = parent.toBean();
        removeBeanReferenceInstances(bean);
        BeanSerialization serialization = new BeanSerialization(propertyIds);
        byte[] data = serialization.write(bean);
        Bean result = serialization.read(data, bean.getId(), bean.getSchema());
        assertReflectionEquals(bean, result, LENIENT_ORDER);
    }

    @Test
    public void test_multi_level_reference_serialization() {
        Parent p1 = getParentWithReferences("p1");
        Parent p2 = getParentWithReferences("p2");

        Grandfather g1 = ConfigTestData.getGrandfather("g1");
        g1.add(p1, p2);
        g1.setProp20(ImmutableMap.of("p1", p1, "p2", p2));
        Bean bean = g1.toBean();
        removeBeanReferenceInstances(bean);
        BeanSerialization serialization = new BeanSerialization(propertyIds);
        byte[] data = serialization.write(bean);
        Bean result = serialization.read(data, bean.getId(), bean.getSchema());
        assertReflectionEquals(bean, result, LENIENT_ORDER);
    }


    private Parent getParentWithReferences(String instanceId) {
        Child c1 = ConfigTestData.getChild("c1");
        Child c2 = ConfigTestData.getChild("c2");

        Parent parent = ConfigTestData.getParent(instanceId);
        parent.add(c1, c2);
        parent.setProp6(c2);
        parent.setProp20(ImmutableMap.of("c1", c1, "c2", c2));
        return parent;
    }

    /**
     * Reading a serialized bean does not initialize it bean reference with an actual instance,
     * and we need to remove these in order to do assertions.
     */
    private void removeBeanReferenceInstances(Bean bean) {
        for (String prop : bean.getReferenceNames()) {
            List<BeanId> beanIds = bean.getReference(prop);
            for (BeanId id : beanIds) {
                id.setBean(null);
            }
        }
    }

}
