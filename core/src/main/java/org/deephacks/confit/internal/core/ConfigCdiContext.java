package org.deephacks.confit.internal.core;

import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.ConfigScope;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
@SuppressWarnings(value = { "unchecked", "rawtypes" })
public class ConfigCdiContext implements Context {
    private BeanManager bm;
    private ConfigContext ctx;
    private CreationalContext cctx = null;
    private Map<Object, Object> cache = new HashMap<>();

    public ConfigCdiContext(BeanManager bm) {
        this.bm = bm;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ConfigScope.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        Bean<T> bean = (Bean<T>) contextual;
        cctx = creationalContext;
        if (ctx == null) {
            ctx = lookupRuntimeContext();
        }
        return (T) ctx.get(bean.getBeanClass());
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        if (cctx == null) {
            return null;
        }
        if (ctx == null) {
            ctx = lookupRuntimeContext();
        }
        Bean<T> bean = (Bean<T>) contextual;
        return (T) ctx.get(bean.getBeanClass());
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private ConfigContext lookupRuntimeContext() {
        Set<Bean<?>> beans = bm.getBeans(ConfigContext.class);
        Bean<?> bean = bm.resolve(beans);
        CreationalContext cc = bm.createCreationalContext(bean);
        return (ConfigContext) bm.getReference(bean, ConfigContext.class, cc);
    }
}