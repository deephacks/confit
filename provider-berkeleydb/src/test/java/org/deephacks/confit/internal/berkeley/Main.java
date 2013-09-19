package org.deephacks.confit.internal.berkeley;


import org.deephacks.confit.model.Bean;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.test.ConfigTestData;
import org.deephacks.confit.test.ConfigTestData.Child;

public class Main {
    public static void main(String[] args) {
        SchemaManager schemaManager = SchemaManager.lookup();
        schemaManager.register(Child.class);
        BerkeleyUtil.create();
        BerkeleyBeanManager manager = new BerkeleyBeanManager();

        Bean c1 = ConfigTestData.getChild("hello").toBean();
        manager.create(c1);
        System.out.println(manager.getEager(c1.getId()));
    }
}
