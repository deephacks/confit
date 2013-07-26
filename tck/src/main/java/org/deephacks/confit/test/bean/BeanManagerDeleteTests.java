package org.deephacks.confit.test.bean;

import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.deephacks.confit.test.LookupProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.deephacks.confit.model.Events.CFG311;
import static org.deephacks.confit.test.JUnitUtils.generateBeans;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(FeatureTestsRunner.class)
public class BeanManagerDeleteTests {
    BeanManager manager = LookupProxy.lookup(BeanManager.class);

    @Before
    public void before() {

    }

    @Test
    public void test_delete_default() {
        List<Bean> beans = generateBeans(2, 2);
        for (Bean bean : beans) {
            bean.setDefault();
        }
        manager.create(beans);
        for (Bean bean : beans) {
            try {
                manager.delete(bean.getId());
                fail("Should not be able to delete default instances");
            } catch (AbortRuntimeException e) {
                assertThat(e.getEvent().getCode(), is(CFG311));
            }
        }

    }
}
