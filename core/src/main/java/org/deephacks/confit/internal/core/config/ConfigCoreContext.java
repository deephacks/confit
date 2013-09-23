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
package org.deephacks.confit.internal.core.config;

import com.google.common.base.Optional;
import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.ConfigObserver;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.query.ConfigQuery;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.CacheManager;
import org.deephacks.confit.spi.NotificationManager;
import org.deephacks.confit.spi.PropertyManager;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.spi.ValidationManager;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.deephacks.confit.model.Events.CFG303;

/**
 * ConfigCoreContext is responsible for separating the admin, config and spi
 * context so that no dependencies (compile nor config) exist between them.
 */
@Singleton
public final class ConfigCoreContext extends ConfigContext {
    private SchemaManager schemaManager;
    private BeanManager beanManager;
    private NotificationManager notificationManager;
    private PropertyManager propertyManager;
    private Optional<ValidationManager> validationManager;
    private Optional<CacheManager> cacheManager;
    private static HashMap<BeanId, Bean> FILE_CONFIG;
    private static final ThreadLocal<String> RECURSION_SHORTCIRCUIT = new ThreadLocal<>();
    private AtomicBoolean LOOKUP_DONE = new AtomicBoolean(false);

    @Override
    public void register(Class<?>... configurable) {
        doLookup();
        schemaManager.register(configurable);
        if (cacheManager.isPresent()) {
            for (Class<?> cls : configurable) {
                Schema schema = schemaManager.getSchema(cls);
                cacheManager.get().registerSchema(schema);
            }
        }
    }

    @Override
    public void unregister(Class<?>... configurable) {
        doLookup();
        for (Class<?> cls : configurable) {
            Schema schema = schemaManager.remove(cls);
            if (cacheManager.isPresent()) {
                cacheManager.get().removeSchema(schema);
            }
        }
    }

    @Override
    public <T> T get(Class<T> configurable) {
        doLookup();
        Schema schema = schemaManager.getSchema(configurable);
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
                T object = (T) schemaManager.convertBean(bean.get());
                if (cacheManager.isPresent()) {
                    cacheManager.get().put(bean.get());
                }
                return object;
            }
        }
        if (!bean.isPresent()) {
            bean = Optional.of(Bean.create(BeanId.createSingleton(schema.getName())));
        }
        schemaManager.setSchema(Arrays.asList(bean.get()));
        setSingletonReferences(bean.get());
        T object = (T) schemaManager.convertBean(bean.get());
        if (cacheManager.isPresent()) {
            cacheManager.get().put(bean.get());
        }
        return object;
    }

    @Override
    public <T> List<T> list(Class<T> configurable) {
        doLookup();
        Schema s = schemaManager.getSchema(configurable);
        initFile(configurable);
        Map<BeanId, Bean> beans = new HashMap<>();
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
        schemaManager.setSchema(beans.values());
        for (Bean bean : beans.values()) {
            setSingletonReferences(bean);
            if (cacheManager.isPresent()) {
                cacheManager.get().put(bean);
            }
        }
        ArrayList<T> objects = new ArrayList<>();
        for(Object object : schemaManager.convertBeans(beans.values())) {
            objects.add((T) object);

        }
        return objects;
    }

    @Override
    public <T> Optional<T> get(String id, Class<T> configurable) {
        doLookup();
        Schema s = schemaManager.getSchema(configurable);
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
        schemaManager.setSchema(Arrays.asList(bean.get()));
        setSingletonReferences(bean.get());
        T object = (T) schemaManager.convertBean(bean.get());

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
        Schema schema = schemaManager.getSchema(configurable);
        return cacheManager.get().newQuery(schema);
    }

    @Override
    public void registerObserver(ConfigObserver observer) {
        doLookup();
        notificationManager.register(observer);
    }

    private BeanId getSingletonId(Schema s, Class<?> configurable) {
        return BeanId.createSingleton(s.getName());
    }

    private void setSingletonReferences(Bean bean) {
        Schema s = bean.getSchema();
        for (SchemaPropertyRef ref : s.get(SchemaPropertyRef.class)) {
            if (ref.isSingleton()) {
                Schema singletonSchema = schemaManager.getSchema(ref.getSchemaName());
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
                setSingletonReferences(singleton.get());
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
        Schema schema = schemaManager.getSchema(configurable);
        for (Bean bean : propertyManager.list(schema, schemaManager.getSchemas())) {
            if (validationManager.isPresent()) {
                Object object = schemaManager.convertBean(bean);
                validationManager.get().validate(object);
            }
            FILE_CONFIG.put(bean.getId(), bean);
        }
    }

    public void doLookup() {
        if (LOOKUP_DONE.get()) {
            return;
        }
        propertyManager = PropertyManager.lookup();
        schemaManager = SchemaManager.lookup();
        beanManager = BeanManager.lookup();
        validationManager = ValidationManager.lookup();
        notificationManager = NotificationManager.lookup();
        cacheManager = CacheManager.lookup();
        LOOKUP_DONE.set(true);
    }
}
