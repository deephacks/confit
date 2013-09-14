package org.deephacks.confit.internal.mapdb;

import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureSetupTeardown;
import org.junit.Before;

@FeatureSetupTeardown(BeanManager.class)
public class MapdbBeanManagerSetup {

    @Before
    public void before() throws Exception {
        MapdbUtil.delete();
    }
}
