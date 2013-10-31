package org.deephacks.confit.test.bean;

import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(FeatureTestsRunner.class)
public class BeanManagerDeleteTests {
    BeanManager manager = BeanManager.lookup();

    @Before
    public void before() {

    }
}
