package org.deephacks.confit.internal.core.cdi;

import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.ConfigScope;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@SuppressWarnings(value = { "unchecked", "rawtypes" })
public class ConfigCdiContext implements Context {
    private ConfigContext ctx;
    private CreationalContext cctx = null;
    private static final ConcurrentHashMap<Class<?>, Object> cache = new ConcurrentHashMap<>();

    public ConfigCdiContext() {
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
            ctx = CDI.current().select(ConfigContext.class).get();
        }
        final Object object;
        if (cache.containsKey(bean.getBeanClass())) {
            object = cache.get(bean.getBeanClass());
        } else {
            object = ctx.get(bean.getBeanClass());
            cache.put(bean.getBeanClass(), object);
        }
        return (T) object;
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        if (cctx == null) {
            return null;
        }
        if (ctx == null) {
            ctx = CDI.current().select(ConfigContext.class).get();
        }
        Bean<T> bean = (Bean<T>) contextual;

        final Object object;
        if (cache.containsKey(bean.getBeanClass())) {
            object = cache.get(bean.getBeanClass());
        } else {
            object = ctx.get(bean.getBeanClass());
            cache.put(bean.getBeanClass(), object);
        }
        return (T) object;
    }

    @Override
    public boolean isActive() {
        return true;
    }
}