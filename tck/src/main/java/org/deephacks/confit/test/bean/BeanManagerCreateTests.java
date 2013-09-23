package org.deephacks.confit.test.bean;

import com.google.common.base.Optional;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.deephacks.confit.test.JUnitUtils.generateBeans;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(FeatureTestsRunner.class)
public class BeanManagerCreateTests {
    BeanManager manager = BeanManager.lookup();

    public void test_create_single() {
        List<Bean> beans = generateBeans(2, 2);
        for (Bean b : beans) {
            manager.create(b);
        }

        for (Bean b : beans) {
            Optional<Bean> r = manager.getEager(b.getId());
            assertThat(r.get(), is(b));
        }
    }

    @Test
    public void test_create_all() {
        List<Bean> beans = generateBeans(2, 2);
        manager.create(beans);
        for (Bean b : beans) {
            Optional<Bean> r = manager.getEager(b.getId());
            assertThat(r.get(), is(b));
        }
    }
}