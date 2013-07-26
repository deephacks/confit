package org.deephacks.confit;

import org.deephacks.confit.model.Bean;
import org.deephacks.confit.test.JUnitUtils;
import org.deephacks.confit.test.JUnitUtils.ConfigClass;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BigBeans {
    public static final File generatedDir = new File("/tmp/"+ UUID.randomUUID().toString());

    public static List<Bean> createBeans(TestSize size, ConfigContext ctx) {
        List<Bean> beans = new ArrayList<>();
        Set<ConfigClass> generatedClasses = JUnitUtils.generate(size.getMinilang(), size.getNumprops(), size.getNumvalues());
        Set<Class<?>> classes = JUnitUtils.compile(generatedClasses, generatedDir);
        ctx.register(classes.toArray(new Class[0]));
        for (ConfigClass cls : generatedClasses) {
            beans.addAll(cls.getBeans());
        }
        return beans;
    }

    public static enum TestSize {
        /**
         *   - creates 1000 A, 2000 B and 300 C instances.
         *   - Assign 200 random A instances to 10 random B references
         *   - Assign 3 random A instances to 300 random C references
         *   - Assign 200 random B instances to 10 random C references
         */
        SMALL("A=1000, B=200$10, C=3$300; B=2000, C=200$10; C=30", 10, 10),
        MEDIM("A=100000, B=200$10, C=3$300; B=20000, C=200$10; C=300", 10, 10),
        BIG("A=1000000, B=200$10, C=3$300; B=200000, C=200$10; C=3000", 10, 10);

        private final String minilang;
        private final int numprops;
        private final int numvalues;

        private  TestSize(String minilang, int numprops, int numvalues) {
            this.minilang = minilang;
            this.numprops = numprops;
            this.numvalues = numvalues;
        }

        public String getMinilang() {
            return minilang;
        }

        public int getNumprops() {
            return numprops;
        }

        public int getNumvalues() {
            return numvalues;
        }
    }
}
