package org.deephacks.confit.internal.berkeley;

import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureSetupTeardown;
import org.junit.Before;

@FeatureSetupTeardown(BeanManager.class)
public class BerkeleyBeanManagerSetup {

    @Before
    public void before() throws Exception {
        BerkeleyUtil.delete();
    }
}
