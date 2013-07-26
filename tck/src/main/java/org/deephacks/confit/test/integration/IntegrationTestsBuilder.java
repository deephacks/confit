package org.deephacks.confit.test.integration;

import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.test.FeatureTestsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * IntegrationTestSuiteBuilder assume impl of ConfigContext and AdminContext
 * to be available on classpath during 'test' phase.
 */
public class IntegrationTestsBuilder extends FeatureTestsBuilder {
    private ArrayList<Class<?>> tests = new ArrayList<>();

    public static IntegrationTestsBuilder named(String name) {
        IntegrationTestsBuilder builder = new IntegrationTestsBuilder();
        builder.name = name;
        return builder;
    }

    public IntegrationTestsBuilder using(BeanManager manager) {
        using(BeanManager.class, manager);
        return this;
    }

    public IntegrationTestsBuilder using(SchemaManager manager) {
        using(SchemaManager.class, manager);
        return this;
    }

    public IntegrationTestsBuilder addTest(Class<?> cls) {
        tests.add(cls);
        return this;
    }

    @Override
    protected List<Class<?>> getTests() {
        if( tests != null && !tests.isEmpty()) {
            return tests;
        }
        ArrayList<Class<?>> tests = new ArrayList<>();
        tests.add(IntegrationConfigTests.class);
        withSetUp(new IntegrationConfigTests());
        tests.add(IntegrationValidationTests.class);
        withSetUp(new IntegrationValidationTests());
        return tests;
    }
}
