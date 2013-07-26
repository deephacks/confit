package org.deephacks.confit.test.cdi;

import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.deephacks.confit.test.ConversionUtils.toBean;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Singleton
@RunWith(CdiFeatureTestsRunner.class)
public class CdiFeatureTest {
    @Inject
    private ConfigContext config;

    @Inject
    private AdminContext admin;

    @Inject
    private CdiSingletonConfig singleton;

    @Before
    public void setup() {
        // touch get to force injection
        singleton.getValue();
    }

    @Test
    public void test_singleton_injection() {
        assertThat(singleton.getValue(), is("value"));
    }

    @Test
    public void test_context_injection() {
        CdiSingletonConfig config = new CdiSingletonConfig("newvalue");
        Bean bean = toBean(config);
        admin.create(bean);
        admin.set(bean);
        BeanId id = BeanId.createSingleton(CdiSingletonConfig.class.getSimpleName());
        bean = admin.get(id).get();
        assertThat(bean.getValues("value").get(0), is("newvalue"));

        config = this.config.get(CdiSingletonConfig.class);
        assertThat(config.getValue(), is("newvalue"));
    }
}
