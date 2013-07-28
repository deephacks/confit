/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.confit.internal.core.runtime;

import com.google.common.base.Optional;
import org.deephacks.confit.Config;
import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.internal.core.ConfigCore;
import org.deephacks.confit.internal.core.DefaultSchemaManager;
import org.deephacks.confit.internal.core.Lookup;
import org.deephacks.confit.internal.core.SystemProperties;
import org.deephacks.confit.internal.core.notification.DefaultNotificationManager;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.query.ConfigQuery;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.CacheManager;
import org.deephacks.confit.spi.Conversion;
import org.deephacks.confit.spi.NotificationManager;
import org.deephacks.confit.spi.NotificationManager.Observer;
import org.deephacks.confit.spi.SchemaManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.deephacks.confit.model.Events.CFG102_NOT_CONFIGURABLE;
import static org.deephacks.confit.model.Events.CFG303;

/**
 * ConfigCoreContext is responsible for separating the admin, config and spi
 * context so that no dependencies (compile nor config) exist between them.
 */
@Singleton
public final class ConfigCoreContext extends ConfigContext {
    private static Conversion conversion;
    private static final Lookup lookup = Lookup.get();
    private SchemaManager schemaManager;
    private BeanManager beanManager;
    private NotificationManager notificationManager;
    private Optional<CacheManager> cacheManager;
    private HashMap<String, Schema> schemas = new HashMap<>();
    private SystemProperties properties = SystemProperties.instance();
    private static HashMap<BeanId, Bean> FILE_CONFIG;
    private static final ThreadLocal<String> RECURSION_SHORTCIRCUIT = new ThreadLocal<>();
    private AtomicBoolean LOOKUP_DONE = new AtomicBoolean(false);

    static {
        conversion = Conversion.get();
        conversion.register(new ObjectToBeanConverter());
        conversion.register(new ClassToSchemaConverter());
        conversion.register(new FieldToSchemaPropertyConverter());
        conversion.register(new BeanToObjectConverter());
    }

    @Inject
    private ConfigCore core;

    @Override
    public void register(Class<?>... configurable) {
        doLookup();
        for (Class<?> clazz : configurable) {
            getSchema(clazz);
        }
    }

    @Override
    public void unregister(Class<?>... configurable) {
        doLookup();
        for (Class<?> clazz : configurable) {
            Schema schema = conversion.convert(clazz, Schema.class);
            schemaManager.removeSchema(schema.getName());
            if (cacheManager.isPresent()) {
                cacheManager.get().removeSchema(schema);
            }
        }
    }

    @Override
    public void registerDefault(Object... instances) {
        doLookup();
        for (Object instance : instances) {
            Bean bean = conversion.convert(instance, Bean.class);
            bean.setDefault();
            try {
                beanManager.create(bean);
            } catch (AbortRuntimeException e) {
                // ignore if bean already exist
                if (e.getEvent().getCode() != CFG303) {
                    throw e;
                }
            }
        }
    }

    @Override
    public <T> T get(Class<T> configurable) {
        doLookup();
        Schema schema = getSchema(configurable);
        BeanId singleton = getSingletonId(schema, configurable);
        Optional<Bean> bean;
        try {
            if (configurable.getName().equals(RECURSION_SHORTCIRCUIT.get())) {
                bean = Optional.absent();
            } else {
                RECURSION_SHORTCIRCUIT.set(configurable.getName());
                bean = beanManager.getEager(singleton);
            }
        } finally {
            RECURSION_SHORTCIRCUIT.set(null);
        }
        if (!bean.isPresent()) {
            initFile(configurable);
            Bean fileBean = FILE_CONFIG.get(singleton);
            if (fileBean != null) {
                fileBean.set(schema);
                bean = Optional.of(fileBean);
            }
            if (bean.isPresent()) {
                T object = conversion.convert(bean.get(), configurable);
                if (cacheManager.isPresent()) {
                    cacheManager.get().put(bean.get());
                }
                return object;
            }
        }
        if (!bean.isPresent()) {
            bean = Optional.of(Bean.create(BeanId.createSingleton(schema.getName())));
        }
        core.setSchema(bean.get(), schemas);
        setSingletonReferences(bean.get(), schemas);
        T object = conversion.convert(bean.get(), configurable);
        if (cacheManager.isPresent()) {
            cacheManager.get().put(bean.get());
        }
        return object;
    }

    @Override
    public <T> List<T> list(Class<T> configurable) {
        doLookup();
        Schema s = getSchema(configurable);
        initFile(configurable);
        Map<BeanId, Bean> beans = new HashMap<>();
        Map<String, Schema> schemas = schemaManager.getSchemas();
        Map<BeanId, Bean> found = beanManager.list(s.getName());
        // only fallback to file if no beans were found.
        // maybe good if file and storage beans were merged?
        // on the other hand, that would mean that file bean instances
        // never can be removed, which may be annoying?
        if (found.isEmpty()) {
            for (Bean bean : FILE_CONFIG.values()) {
                if (bean.getId().getSchemaName().equals(s.getName())) {
                    beans.put(bean.getId(), bean);
                }
            }
        }
        for (Bean foundBean : found.values()) {
            beans.put(foundBean.getId(), foundBean);
        }
        core.setSchema(schemas, beans);
        for (Bean bean : beans.values()) {
            setSingletonReferences(bean, schemas);
            if (cacheManager.isPresent()) {
                cacheManager.get().put(bean);
            }
        }
        ArrayList<T> objects = new ArrayList<>();
        for(T object : conversion.convert(beans.values(), configurable)) {
            objects.add(object);

        }
        return objects;
    }

    @Override
    public <T> Optional<T> get(String id, Class<T> configurable) {
        doLookup();
        Schema s = getSchema(configurable);
        Map<String, Schema> schemas = schemaManager.getSchemas();
        BeanId beanId = BeanId.create(id, s.getName());
        Optional<Bean> bean = beanManager.getEager(beanId);
        if (!bean.isPresent()) {
            initFile(configurable);
            Bean fileBean = FILE_CONFIG.get(beanId);
            if (fileBean != null) {
                bean = Optional.of(fileBean);
            } else {
                return Optional.absent();
            }
        }
        core.setSchema(bean.get(), schemas);
        setSingletonReferences(bean.get(), schemas);
        T object = conversion.convert(bean.get(), configurable);
        if (cacheManager.isPresent()) {
            cacheManager.get().put(bean.get());
        }
        return Optional.of(object);
    }

    @Override
    public <T> ConfigQuery<T> newQuery(Class<T> configurable) {
        doLookup();
        if(!cacheManager.isPresent()) {
            throw new IllegalStateException("Queries are not possible without a cache manager.");
        }
        Schema schema = getSchema(configurable);
        return cacheManager.get().newQuery(schema);
    }

    @Override
    public void registerObserver(Object observer) {
        doLookup();
        notificationManager.register(new Observer(observer));
    }

    private BeanId getSingletonId(Schema s, Class<?> configurable) {
        return BeanId.createSingleton(s.getName());
    }

    private void setSingletonReferences(Bean bean, Map<String, Schema> schemas) {
        Schema s = bean.getSchema();
        for (SchemaPropertyRef ref : s.get(SchemaPropertyRef.class)) {
            if (ref.isSingleton()) {
                Schema singletonSchema = schemas.get(ref.getSchemaName());
                Optional<Bean> singleton = beanManager.getSingleton(ref.getSchemaName());
                if (!singleton.isPresent()) {
                    initFile(null);
                    Bean fileBean = FILE_CONFIG.get(BeanId.createSingleton(ref.getSchemaName()));
                    if (fileBean != null) {
                        singleton = Optional.of(fileBean);
                    }
                }
                if (!singleton.isPresent()) {
                    singleton = Optional.of(Bean.create(BeanId.createSingleton(ref.getSchemaName())));
                }
                singleton.get().set(singletonSchema);
                BeanId singletonId = singleton.get().getId();
                singletonId.setBean(singleton.get());
                // recursive call.
                setSingletonReferences(singleton.get(), schemas);
                bean.setReference(ref.getName(), singletonId);
            }
        }
    }

    private synchronized void initFile(Class<?> configurable) {
        if (configurable == null) {
            return;
        }
        if (FILE_CONFIG == null) {
            FILE_CONFIG = new HashMap<>();
        }
        Schema schema = getSchema(configurable);
        for (Bean bean : properties.list(schema)) {
            FILE_CONFIG.put(bean.getId(), bean);
        }
    }

    public void doLookup() {
        if (LOOKUP_DONE.get()) {
            return;
        }
        // core would already be injected in a cdi environment
        if (core == null) {
            core = new ConfigCore();
        }
        schemaManager = lookup.lookup(SchemaManager.class, DefaultSchemaManager.class);
        beanManager = lookup.lookup(BeanManager.class, DefaultBeanManager.class);
        notificationManager = lookup.lookup(NotificationManager.class, DefaultNotificationManager.class);
        if (cacheManager == null) {
            cacheManager = core.lookupCacheManager();
            notificationManager.register(new Observer(cacheManager));
        }
        LOOKUP_DONE.set(true);
    }

    private Schema getSchema(Class<?> clazz) {
        Schema schema = schemas.get(clazz);
        if(schema != null) {
            return schema;
        }
        ClassIntrospector introspector = new ClassIntrospector(clazz);
        Config config = introspector.getAnnotation(Config.class);
        if (config == null) {
            throw CFG102_NOT_CONFIGURABLE(clazz);
        }
        String schemaName = config.name();
        if (schemaName == null || "".equals(schemaName)) {
            schemaName = clazz.getName();
        }
        schema = conversion.convert(clazz, Schema.class);
        for (Class<?> cls : schema.getReferenceSchemaTypes()) {
            if (schemas.get(clazz) != null) {
                getSchema(cls);
            }
        }
        properties.registerSchema(schema);
        schemas.put(schema.getName(), schema);
        schemaManager.registerSchema(schema);
        if (cacheManager.isPresent()) {
            cacheManager.get().registerSchema(schema);
        }
        return schema;
    }
}
