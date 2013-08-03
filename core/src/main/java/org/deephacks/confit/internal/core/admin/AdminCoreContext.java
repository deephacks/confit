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
package org.deephacks.confit.internal.core.admin;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.internal.core.config.DefaultBeanManager;
import org.deephacks.confit.internal.core.notification.DefaultNotificationManager;
import org.deephacks.confit.internal.core.schema.DefaultSchemaManager;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.BeanUtils;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.CacheManager;
import org.deephacks.confit.spi.Lookup;
import org.deephacks.confit.spi.NotificationManager;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.spi.ValidationManager;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.deephacks.confit.model.Events.CFG301_MISSING_RUNTIME_REF;

/**
 * AdminCoreContext is responsible for separating the admin and config
 * context so that no dependencies (compile nor config) exist between them.
 */
@Singleton
public final class AdminCoreContext extends AdminContext {
    private AtomicBoolean LOOKUP_DONE = new AtomicBoolean(false);
    private static final Lookup lookup = Lookup.get();
    private BeanManager beanManager;
    private SchemaManager schemaManager;
    private NotificationManager notificationManager;
    private Optional<CacheManager> cacheManager;
    private Optional<ValidationManager> validationManager;

    @Override
    public List<Bean> list(String schemaName) {
        Preconditions.checkNotNull(schemaName);
        doLookup();
        schemaManager.getSchema(schemaName);
        Map<BeanId, Bean> beans = beanManager.list(schemaName);
        schemaManager.setSchema(beans.values());
        return new ArrayList<>(beans.values());
    }

    @Override
    public <T> Collection<T> list(Class<T> configurable) throws AbortRuntimeException {
        final Schema schema = schemaManager.getSchema(configurable);
        Collection<Bean> beans = list(schema.getName());
        return (Collection<T>) schemaManager.convertBeans(beans);
    }

    @Override
    public List<Bean> list(String schemaName, Collection <String> instanceIds) {
        Preconditions.checkNotNull(schemaName);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return new ArrayList<>();
        }
        doLookup();
        schemaManager.getSchema(schemaName);
        Map<BeanId, Bean> beans = beanManager.list(schemaName, instanceIds);
        schemaManager.setSchema(beans.values());
        return new ArrayList<>(beans.values());
    }

    @Override
    public Optional<Bean> get(BeanId beanId) {
        Preconditions.checkNotNull(beanId);
        doLookup();
        Optional<Bean> bean = beanManager.getEager(beanId);
        if (!bean.isPresent()) {
            return bean;
        }
        schemaManager.setSchema(Arrays.asList(bean.get()));
        setSingletonReferences(bean.get());
        return bean;
    }

    @Override
    public <T> Optional<T> get(Class<T> configurable) throws AbortRuntimeException {
        Schema schema = schemaManager.getSchema(configurable);
        Optional<Bean> bean = get(BeanId.createSingleton(schema.getName()));
        if (!bean.isPresent()) {
            return Optional.absent();
        }
        return (Optional<T>) Optional.of(schemaManager.convertBean(bean.get()));
    }

    @Override
    public <T> Optional<T> get(Class<T> configurable, String instanceId) throws AbortRuntimeException {
        Schema schema = schemaManager.getSchema(configurable);
        Optional<Bean> bean = get(BeanId.create(instanceId, schema.getName()));
        if (!bean.isPresent()) {
            return Optional.absent();
        }
        Object object = schemaManager.convertBean(bean.get());
        return Optional.of((T) object);
    }

    @Override
    public void create(Bean bean) {
        Preconditions.checkNotNull(bean);
        create(Arrays.asList(bean));
        if (cacheManager.isPresent()) {
            cacheManager.get().put(bean);
        }
    }

    @Override
    public void createObject(Object object) throws AbortRuntimeException {
        createObjects(Arrays.asList(object));
    }

    @Override
    public void create(Collection<Bean> beans) {
        if (beans == null || beans.isEmpty()) {
            return;
        }
        doLookup();
        schemaManager.setSchema(beans);
        schemaManager.validateSchema(beans);
        if (validationManager.isPresent()) {
            initReferences(beans);
            // ready to validate
            Collection<Object> objects = schemaManager.convertBeans(beans);
            validationManager.get().validate(objects);
        }
        beanManager.create(beans);
        notificationManager.fireCreate(beans);
        if (cacheManager.isPresent()) {
            cacheManager.get().putAll(beans);
        }
    }

    @Override
    public void createObjects(Collection <?> objects) throws AbortRuntimeException {
        doLookup();
        if (objects == null || objects.isEmpty()) {
            return;
        }
        Collection<Bean> beans = schemaManager.convertObjects((Collection<Object>) objects);
        create(beans);
    }

    @Override
    public void set(Bean bean) {
        doLookup();
        Preconditions.checkNotNull(bean);
        set(Arrays.asList(bean));
    }

    @Override
    public void setObject(Object object) throws AbortRuntimeException {
        setObjects(Arrays.asList(object));
    }

    @Override
    public void set(Collection<Bean> beans) {
        if (beans == null || beans.isEmpty()) {
            return;
        }
        doLookup();
        schemaManager.setSchema(beans);
        schemaManager.validateSchema(beans);

        if (validationManager.isPresent()) {
            initReferences(beans);
            validateSet(beans);
        }
        beanManager.set(beans);
        notificationManager.fireUpdated(beans);
        if (cacheManager.isPresent()) {
            cacheManager.get().putAll(beans);
        }
    }

    @Override
    public void setObjects(Collection<?> objects) throws AbortRuntimeException {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        Collection<Bean> beans = schemaManager.convertObjects((Collection<Object>) objects);
        set(beans);
    }

    @Override
    public void merge(Bean bean) {
        Preconditions.checkNotNull(bean);
        merge(Arrays.asList(bean));
    }

    @Override
    public void mergeObject(Object object) throws AbortRuntimeException {
        mergeObjects(Arrays.asList(object));
    }

    @Override
    public void merge(Collection<Bean> beans) {
        if (beans == null || beans.isEmpty()) {
            return;
        }
        doLookup();
        schemaManager.setSchema(beans);
        schemaManager.validateSchema(beans);
        // ok to not have validation manager available
        if (validationManager.isPresent()) {
            validateMerge(beans);
        }
        beanManager.merge(beans);
        notificationManager.fireUpdated(beans);
        if (cacheManager.isPresent()) {
            for (Bean bean : beans) {
                // must refresh the bean from storage since it is merged.
                Optional<Bean> refreshed = beanManager.getEager(bean.getId());
                if (refreshed.isPresent()) {
                    cacheManager.get().put(refreshed.get());
                }
            }
        }
    }

    @Override
    public void mergeObjects(Collection<?> objects) throws AbortRuntimeException {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        Collection<Bean> beans = schemaManager.convertObjects((Collection<Object>) objects);
        merge(beans);
    }

    @Override
    public void delete(BeanId beanId) {
        Preconditions.checkNotNull(beanId);
        doLookup();
        Bean bean = beanManager.delete(beanId);
        if (bean == null) {
            throw Events.CFG304_BEAN_DOESNT_EXIST(beanId);
        }
        schemaManager.setSchema(Arrays.asList(bean));
        notificationManager.fireDelete(Lists.newArrayList(bean));
        if (cacheManager.isPresent()) {
            cacheManager.get().remove(beanId);
        }
    }

    @Override
    public void deleteObject(Object instance) throws AbortRuntimeException {
        Bean bean = schemaManager.convertObject(instance);
        delete(bean.getId());
    }

    @Override
    public void delete(String name, Collection<String> instances) {
        Preconditions.checkNotNull(name);
        if (instances == null || instances.isEmpty()) {
            return;
        }
        doLookup();
        Collection<Bean> beans = beanManager.delete(name, instances);
        schemaManager.setSchema(beans);
        notificationManager.fireDelete(beans);

        if (cacheManager.isPresent()) {
            cacheManager.get().remove(name, instances);
        }
    }

    @Override
    public void deleteObjects(Class<?> configurable, Collection<String> instanceIds) throws AbortRuntimeException {
        Schema schema = schemaManager.getSchema(configurable);
        for (String instanceId : instanceIds) {
            delete(BeanId.create(instanceId, schema.getName()));
        }
    }

    @Override
    public Map<String, Schema> getSchemas() {
        doLookup();
        return schemaManager.getSchemas();
    }

    @Override
    public Optional<Schema> getSchema(String schemaName) {
        try {
            Schema schema = schemaManager.getSchema(schemaName);
            return Optional.of(schema);
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    @Override
    public BeanQuery newQuery(String schemaName) {
        doLookup();
        Schema schema = schemaManager.getSchema(schemaName);
        return beanManager.newQuery(schema);
    }

    private void initReferences(Collection<Bean> beans) {
        Map<BeanId, Bean> indexed = BeanUtils.uniqueIndex(beans);
        for (Bean bean : beans) {
            for (String name : bean.getReferenceNames()) {
                List<BeanId> ids = bean.getReference(name);
                if (ids == null) {
                    continue;
                }
                for (BeanId id : ids) {
                    Bean ref = indexed.get(id);
                    if (ref == null) {
                        // TODO: investigate if eager is really needed
                        Optional<Bean> optionalRef = beanManager.getEager(id);
                        if (optionalRef.isPresent()) {
                            schemaManager.setSchema(Arrays.asList(optionalRef.get()));
                            ref = optionalRef.get();
                        }
                    }
                    id.setBean(ref);
                }
            }
        }
    }

    private void validateMerge(Collection<Bean> mergebeans) {
        Map<BeanId, Bean> beansToValidate = beanManager.getBeanToValidate(mergebeans);
        schemaManager.setSchema(beansToValidate.values());
        // since we are validating mergebean predecessors, we need to make sure
        // that they see a merged reference (not unmerged reference currently in storage)
        // before validation can proceed.
        for (Bean mergebean : mergebeans) {
            ArrayList<Bean> mergeBeanReferences = new ArrayList<>();
            ArrayList<Bean> checked = new ArrayList<>();
            findReferences(mergebean.getId(), beansToValidate.values(), mergeBeanReferences,
                    checked);
            // merge list references
            merge(mergeBeanReferences, mergebean);
        }
        // ready to validate
        Collection<Object> objects = schemaManager.convertBeans(beansToValidate.values());
        validationManager.get().validate(objects);
    }

    private void validateSet(Collection<Bean> setbeans) {
        Map<BeanId, Bean> beansToValidate = beanManager.getBeanToValidate(setbeans);
        schemaManager.setSchema(beansToValidate.values());
        // since we are validating setbean predecessors, we need to make sure
        // that they see a replaced/set reference (not old reference currently in storage)
        // before validation can proceed.
        for (Bean setbean : setbeans) {
            ArrayList<Bean> setBeanReferences = new ArrayList<>();
            ArrayList<Bean> checked = new ArrayList<>();
            findReferences(setbean.getId(), beansToValidate.values(), setBeanReferences, checked);
            for (Bean ref : setBeanReferences) {
                // clearing and then merging have same
                // effect as a 'set' operation
                ref.clear();
            }
            merge(setBeanReferences, setbean);
        }
        // ready to validate
        Collection<Object> objects = schemaManager.convertBeans(beansToValidate.values());
        validationManager.get().validate(objects);
    }

    /**
     * Does a recursive check if predecessor have a particular reference and if
     * so return those predecessor references.
     */
    private List<Bean> findReferences(BeanId reference, Collection<Bean> predecessors,
            ArrayList<Bean> matches, ArrayList<Bean> checked) {

        for (Bean predecessor : predecessors) {
            findReferences(reference, predecessor, matches, checked);
        }
        return matches;
    }

    private void findReferences(BeanId reference, Bean predecessor, ArrayList<Bean> matches,
            ArrayList<Bean> checked) {
        if (checked.contains(predecessor)) {
            return;
        }
        checked.add(predecessor);
        if (reference.equals(predecessor.getId()) && !matches.contains(predecessor)) {
            matches.add(predecessor);
        }
        for (BeanId ref : predecessor.getReferences()) {
            if (ref.getBean() == null) {
                continue;
            }
            if (matches.contains(ref.getBean())) {
                continue;
            }
            if (ref.equals(reference)) {
                matches.add(ref.getBean());
            }

            findReferences(reference, ref.getBean(), matches, checked);
        }

    }

    private void merge(List<Bean> sources, Bean mergeBean) {
        HashMap<BeanId, Bean> cache = new HashMap<>();
        for (Bean source : sources) {
            for (String name : mergeBean.getPropertyNames()) {
                List<String> values = mergeBean.getValues(name);
                if (values == null || values.size() == 0) {
                    continue;
                }
                source.setProperty(name, values);
            }
            for (String name : mergeBean.getReferenceNames()) {
                List<BeanId> refs = mergeBean.getReference(name);
                if (refs == null || refs.size() == 0) {
                    source.setReferences(name, refs);
                    continue;
                }
                for (BeanId beanId : refs) {
                    Bean bean = cache.get(beanId);
                    if (bean == null) {
                        Optional<Bean> optional = beanManager.getLazy(beanId);
                        if (!optional.isPresent()) {
                            throw CFG301_MISSING_RUNTIME_REF(beanId);
                        }
                        bean = optional.get();
                        schemaManager.setSchema(Arrays.asList(bean));
                        cache.put(beanId, bean);
                    }
                    beanId.setBean(bean);
                }
                source.setReferences(name, refs);
            }

        }
    }

    /**
     * Used for setting or creating a single bean.
     */
    @SuppressWarnings("unused")
    private void initalizeReferences(Bean bean) {
        for (String name : bean.getReferenceNames()) {
            List<BeanId> values = bean.getReference(name);
            if (values == null) {
                continue;
            }
            for (BeanId beanId : values) {
                Optional<Bean> ref = beanManager.getEager(beanId);
                if (ref.isPresent()) {
                    beanId.setBean(ref.get());
                }
                schemaManager.setSchema(Arrays.asList(beanId.getBean()));
            }
        }
    }

    /**
     * Used for setting or creating a multiple beans.
     *
     * We must consider that the operation may include beans that have
     * references betewen eachother. User provided beans are
     * prioritized and the storage is secondary for looking up references.
     */
    @SuppressWarnings("unused")
    private void initalizeReferences(Collection<Bean> beans) {
        Map<BeanId, Bean> userProvided = BeanUtils.uniqueIndex(beans);
        for (Bean bean : beans) {
            for (String name : bean.getReferenceNames()) {
                List<BeanId> values = bean.getReference(name);
                if (values == null) {
                    continue;
                }
                for (BeanId beanId : values) {
                    // the does not exist in storage, but may exist in the
                    // set of beans provided by the user.
                    Bean ref = userProvided.get(beanId);
                    if (ref == null) {
                        Optional<Bean> optional = beanManager.getEager(beanId);
                        if (optional.isPresent()) {
                            ref = optional.get();
                        }
                    }
                    beanId.setBean(ref);
                    schemaManager.setSchema(Arrays.asList(beanId.getBean()));
                }
            }
        }
    }

    private void setSingletonReferences(Bean bean) {
        Schema s = bean.getSchema();
        for (SchemaPropertyRef ref : s.get(SchemaPropertyRef.class)) {
            if (ref.isSingleton()) {
                Schema singletonSchema = schemaManager.getSchema(ref.getSchemaName());
                Optional<Bean> singleton = beanManager.getSingleton(ref.getSchemaName());
                if (singleton.isPresent()) {
                    singleton.get().set(singletonSchema);
                    BeanId singletonId = singleton.get().getId();
                    singletonId.setBean(singleton.get());
                    // recursive call.
                    setSingletonReferences(singleton.get());
                    bean.setReference(ref.getName(), singletonId);
                }
            }
        }
    }

    private void doLookup() {
        if (LOOKUP_DONE.get()) {
            return;
        }
        beanManager = lookup.lookup(BeanManager.class, DefaultBeanManager.class);
        schemaManager = lookup.lookup(SchemaManager.class, DefaultSchemaManager.class);
        notificationManager = lookup.lookup(NotificationManager.class, DefaultNotificationManager.class);
        cacheManager = CacheManager.lookup();
        validationManager = ValidationManager.lookup();
        LOOKUP_DONE.set(true);
    }
}
