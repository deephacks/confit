package org.deephacks.confit.internal.core.cdi;

import org.deephacks.confit.Config;
import org.deephacks.confit.ConfigContext;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class ConfigCdiBootstrap implements Extension {
private static final Set<Class<?>> schemas = new HashSet<>();

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        event.addContext(new ConfigCdiContext());
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        ConfigContext ctx = CDI.current().select(ConfigContext.class).get();
        for (Class<?> cls : schemas){
            ctx.register(cls);
        }
    }

    public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat) {
        AnnotatedType<?> type = pat.getAnnotatedType();
        if (type.isAnnotationPresent(Config.class)) {
            schemas.add(pat.getAnnotatedType().getJavaClass());
        }
    }
}
