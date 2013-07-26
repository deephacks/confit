package org.deephacks.confit.test.cdi;

import java.util.ArrayList;
import java.util.List;

import org.deephacks.confit.test.FeatureTestsBuilder;

public class CdiFeatureTestBuilder extends FeatureTestsBuilder {

    public static CdiFeatureTestBuilder named(String name) {
        CdiFeatureTestBuilder builder = new CdiFeatureTestBuilder();
        builder.name = name;
        return builder;
    }

    @Override
    protected List<Class<?>> getTests() {
        List<Class<?>> tests = new ArrayList<Class<?>>();
        tests.add(CdiFeatureTest.class);
        return tests;
    }
}
