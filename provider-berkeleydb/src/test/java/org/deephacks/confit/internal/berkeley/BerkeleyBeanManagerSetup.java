package org.deephacks.confit.internal.berkeley;

import org.junit.Before;

//@FeatureSetupTeardown(BeanManager.class)
public class BerkeleyBeanManagerSetup {

    @Before
    public void before() throws Exception {
        BerkeleyUtil.delete();
    }
}
