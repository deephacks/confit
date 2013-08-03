package org.deephacks.confit.test.bean;

import com.google.common.base.Optional;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.junit.runner.RunWith;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(FeatureTestsRunner.class)
public class BeanManagerGetTests {
    BeanManager manager = BeanManager.lookup();

    public void test_get_eagerly() {
        // add child
        Bean child = Bean.create(BeanId.create("child", "java.lang.String"));
        child.addProperty("property1", "true");
        child.addProperty("property2", "false");
        manager.create(child);

        // add parent that reference child
        Bean parent = Bean.create(BeanId.create("parent", "java.lang.String"));
        parent.addReference("refName", BeanId.create("child", "java.lang.String"));
        parent.addProperty("property1", "prop1");
        parent.addProperty("property2", "prop2");
        manager.create(parent);

        // add grandparent that reference parent
        BeanId grandParentId = BeanId.create("grandparent", "java.lang.String");
        Bean grandparent = Bean.create(grandParentId);
        grandparent.addReference("refName", BeanId.create("parent", "java.lang.String"));
        manager.create(grandparent);

        // query parent and see if bean reference got fetched.
        Optional<Bean> grandpa = manager.getEager(grandParentId);

        List<String> childs = grandpa.get().getReferenceNames();
        assertThat(childs.size(), is(1));
        BeanId childRef = grandpa.get().getReference(childs.get(0)).get(0);
        Bean childBean = childRef.getBean();
        assertThat(childBean.getId(), is(childBean.getId()));
        assertEquals(childBean.getSingleValue("property1"), "prop1");
        assertEquals(childBean.getSingleValue("property2"), "prop2");

        // parent cool, lets look for children
        childs = childBean.getReferenceNames();
        assertThat(childs.size(), is(1));
        childRef = childBean.getReference(childs.get(0)).get(0);
        childBean = childRef.getBean();

        assertThat(childBean.getId(), is(childBean.getId()));
        assertEquals(childBean.getSingleValue("property1"), "true");
        assertEquals(childBean.getSingleValue("property2"), "false");
    }

}