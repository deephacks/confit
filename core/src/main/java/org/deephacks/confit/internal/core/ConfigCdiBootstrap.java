package org.deephacks.confit.internal.core;

import org.deephacks.confit.Config;
import org.deephacks.confit.ConfigContext;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class ConfigCdiBootstrap implements Extension {
private static final Set<Class<?>> schemas = new HashSet<>();

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager bm) {
        event.addContext(new ConfigCdiContext(bm));
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager bm) {
        ConfigContext ctx = getInstance(ConfigContext.class, bm);
        for (Class<?> cls : schemas){
            ctx.register(cls);
        }
    }

    public static <T> T getInstance(Class<T> cls, BeanManager bm){
        Set<Bean<?>> beans = bm.getBeans(cls);
        Bean<?> bean = bm.resolve(beans);
        CreationalContext cc = bm.createCreationalContext(bean);
        return (T) bm.getReference(bean, cls, cc);
    }

    public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat) {
        AnnotatedType<?> type = pat.getAnnotatedType();
        if (type.isAnnotationPresent(Config.class)) {
            schemas.add(pat.getAnnotatedType().getJavaClass());
        }
    }
}
