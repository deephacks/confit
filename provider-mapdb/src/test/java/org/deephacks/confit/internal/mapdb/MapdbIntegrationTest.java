package org.deephacks.confit.internal.mapdb;


import org.deephacks.confit.test.FeatureTests;
import org.deephacks.confit.test.FeatureTestsBuilder.TestRound;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.deephacks.confit.test.integration.IntegrationConfigTests;
import org.deephacks.confit.test.integration.IntegrationNotificationTests;
import org.deephacks.confit.test.integration.IntegrationTestsBuilder;
import org.deephacks.confit.test.integration.IntegrationValidationTests;
import org.deephacks.confit.test.query.BeanQueryTest;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * TCK tests from MapDB bean manager.
 */
@RunWith(FeatureTestsRunner.class)
public class MapdbIntegrationTest implements FeatureTests {

    @Override
    public List<TestRound> build() {
        MapdbUtil.create();
        MapdbBeanManager manager = new MapdbBeanManager();
        return IntegrationTestsBuilder.named(MapdbIntegrationTest.class.getSimpleName())
                .using(manager)
                .addTest(BeanQueryTest.class)
                .addTest(IntegrationNotificationTests.class)
                .addTest(IntegrationConfigTests.class)
                .addTest(IntegrationValidationTests.class)
                .build();
    }

}
