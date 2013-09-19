package org.deephacks.confit.internal.berkeley;


import org.deephacks.confit.test.FeatureTests;
import org.deephacks.confit.test.FeatureTestsBuilder.TestRound;
import org.deephacks.confit.test.integration.IntegrationConfigTests;
import org.deephacks.confit.test.integration.IntegrationNotificationTests;
import org.deephacks.confit.test.integration.IntegrationTestsBuilder;
import org.deephacks.confit.test.integration.IntegrationValidationTests;
import org.deephacks.confit.test.query.BeanQueryTest;

import java.util.List;

/**
 * TCK tests from MapDB bean manager.
 */
//@RunWith(FeatureTestsRunner.class)
public class BerkeleyIntegrationTest implements FeatureTests {

    @Override
    public List<TestRound> build() {
        BerkeleyUtil.create();
        BerkeleyBeanManager manager = new BerkeleyBeanManager();
        return IntegrationTestsBuilder.named(BerkeleyIntegrationTest.class.getSimpleName())
                .using(manager)
                .addTest(BeanQueryTest.class)
                .addTest(IntegrationNotificationTests.class)
                .addTest(IntegrationConfigTests.class)
                .addTest(IntegrationValidationTests.class)
                .build();
    }

}
