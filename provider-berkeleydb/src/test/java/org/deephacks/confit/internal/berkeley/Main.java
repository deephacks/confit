package org.deephacks.confit.internal.berkeley;


import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.test.ConfigTestData;
import org.deephacks.confit.test.ConfigTestData.Child;
import org.deephacks.confit.test.ConfigTestData.Grandfather;
import org.deephacks.confit.test.ConfigTestData.Parent;

import java.util.Arrays;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        SchemaManager schemaManager = SchemaManager.lookup();
        schemaManager.register(Child.class, Parent.class, Grandfather.class);
        BerkeleyUtil.create();
        BerkeleyBeanManager manager = new BerkeleyBeanManager();
        Bean c1 = ConfigTestData.getChild("c1").toBean();
        Bean c2 = ConfigTestData.getChild("c2").toBean();
        Bean c3 = ConfigTestData.getChild("c3").toBean();
        Bean c4 = ConfigTestData.getChild("c4").toBean();
        Bean c5 = ConfigTestData.getChild("c5").toBean();
        Bean p1 = ConfigTestData.getParent("p1").toBean();
        Bean p2 = ConfigTestData.getParent("p2").toBean();
        Bean p3 = ConfigTestData.getParent("p3").toBean();
        Bean p4 = ConfigTestData.getParent("p4").toBean();
        Bean p5 = ConfigTestData.getParent("p5").toBean();
        Bean g1 = ConfigTestData.getGrandfather("g1").toBean();
        Bean g2 = ConfigTestData.getGrandfather("g2").toBean();
        Bean g3 = ConfigTestData.getGrandfather("g3").toBean();
        Bean g4 = ConfigTestData.getGrandfather("g4").toBean();
        Bean g5 = ConfigTestData.getGrandfather("g5").toBean();
        //manager.create(Arrays.asList(c1, c2, c3, c4, c5, p1, p2, p3, p4, p5, g1, g2, g3, g4, g5) );

        Child cc1 = ConfigTestData.getChild("cc1");
        Parent pp1 = ConfigTestData.getParent("pp1");
        pp1.add(cc1);
        manager.create(Arrays.asList(pp1.toBean()) );
        Map<BeanId,Bean> list = manager.list(c1.getSchema().getName());

        System.out.println(list.keySet());
        System.out.println(manager.getEager(p1.getId()));
    }
}
