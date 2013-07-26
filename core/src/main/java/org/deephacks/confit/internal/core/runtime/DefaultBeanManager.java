package org.deephacks.confit.internal.core.runtime;

import com.google.common.base.Optional;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.admin.query.BeanQueryBuilder.BeanRestriction;
import org.deephacks.confit.admin.query.BeanQueryBuilder.LogicalRestriction;
import org.deephacks.confit.admin.query.BeanQueryBuilder.PropertyRestriction;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.BeanManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.deephacks.confit.model.Events.*;

/**
 * In memory BeanManager.
 */
public class DefaultBeanManager extends BeanManager {
    private static final InMemoryStorage storage = new InMemoryStorage();

    @Override
    public Optional<Bean> getEager(BeanId id) {
        return getEagerly(id);
    }

    private Optional<Bean> getEagerly(BeanId id) {
        HashMap<BeanId, Bean> found = new HashMap<>();
        getEagerly(id, found, new HashSet<BeanId>());
        Bean bean = found.get(id);
        if (bean == null) {
            return Optional.absent();
        }
        return Optional.of(bean);
    }

    private void getEagerly(BeanId id, HashMap<BeanId, Bean> found, Set<BeanId> seen) {
        if (seen.contains(id)) {
            return;
        }
        seen.add(id);
        Bean bean = found.get(id) == null ? storage.get(id) : found.get(id);
        if (bean == null) {
            return;
        }
        found.put(id, bean);
        // bean found, fetch references.
        for (BeanId ref : bean.getReferences()) {
            if (ref.getBean() != null) {
                continue;
            }
            Bean refBean = found.get(ref) == null ? storage.get(ref) : found.get(ref);
            if (refBean == null) {
                throw CFG301_MISSING_RUNTIME_REF(bean.getId(), ref);
            }
            ref.setBean(refBean);
            found.put(ref, refBean);
            getEagerly(ref, found, seen);
        }

    }

    @Override
    public Optional<Bean> getLazy(BeanId id) throws AbortRuntimeException {
        Bean bean = storage.get(id);
        if (bean == null) {
            return Optional.absent();
        }
        for (BeanId ref : bean.getReferences()) {
            Bean refBean = storage.get(ref);
            if (refBean == null) {
                throw CFG301_MISSING_RUNTIME_REF(ref);
            }
            ref.setBean(refBean);
        }
        return Optional.of(bean);
    }

    /**
     * The direct, but no further, successors that references this bean will also be
     * fetched and initialized with their direct, but no further, predecessors.
     */
    @Override
    public Map<BeanId, Bean> getBeanToValidate(Collection<Bean> beans) throws AbortRuntimeException {
        Map<BeanId, Bean> beansToValidate = new HashMap<>();
        for (Bean bean : beans) {
            Map<BeanId, Bean> predecessors = new HashMap<>();
            // beans read from xml storage will only have their basic properties initialized...
            // ... but we also need set the direct references/predecessors for beans to validate
            Map<BeanId, Bean> beansToValidateSubset = getDirectSuccessors(bean);
            beansToValidateSubset.put(bean.getId(), bean);
            for (Bean toValidate : beansToValidateSubset.values()) {
                predecessors.putAll(getDirectPredecessors(toValidate));
            }

            for (Bean predecessor : predecessors.values()) {
                for (BeanId ref : predecessor.getReferences()) {
                    Bean b = storage.get(ref);
                    if (b == null) {
                        throw CFG301_MISSING_RUNTIME_REF(predecessor.getId());
                    }
                    ref.setBean(b);
                }
            }
            for (Bean toValidate : beansToValidateSubset.values()) {
                // list references of beansToValidate should now
                // be available in predecessors.
                for (BeanId ref : toValidate.getReferences()) {
                    Bean predecessor = predecessors.get(ref);
                    if (predecessor == null) {
                        throw new IllegalStateException("Bug in algorithm. Reference [" + ref
                                + "] of [" + toValidate.getId()
                                + "] should be available in predecessors.");
                    }
                    ref.setBean(predecessor);
                }
            }
            beansToValidate.putAll(predecessors);
        }
        return beansToValidate;
    }

    private Map<BeanId, Bean> getDirectPredecessors(Bean bean) {
        Map<BeanId, Bean> predecessors = new HashMap<>();
        for (BeanId ref : bean.getReferences()) {
            Bean predecessor = storage.get(ref);
            if (predecessor == null) {
                throw CFG304_BEAN_DOESNT_EXIST(ref);
            }
            predecessors.put(predecessor.getId(), predecessor);
        }
        return predecessors;
    }

    private Map<BeanId, Bean> getDirectSuccessors(Bean bean) {
        Map<BeanId, Bean> successors = new HashMap<>();
        for (Bean b : storage.all()) {
            List<BeanId> refs = b.getReferences();
            if (refs.contains(bean.getId())) {
                successors.put(b.getId(), b);
            }
        }
        return successors;
    }

    @Override
    public Optional<Bean> getSingleton(String schemaName) throws IllegalArgumentException {
        for (Bean bean : storage.all()) {
            if (bean.getId().getSchemaName().equals(schemaName)) {
                if (!bean.getId().isSingleton()) {
                    throw new IllegalArgumentException("Schema [" + schemaName
                            + "] is not a get.");
                }
                BeanId singletonId = bean.getId();
                return getEagerly(singletonId);
            }
        }
        return Optional.of(Bean.create(BeanId.createSingleton(schemaName)));
    }

    @Override
    public Map<BeanId, Bean> list(String name) {
        Map<BeanId, Bean> result = new HashMap<>();
        for (Bean b : storage.all()) {
            if (b.getId().getSchemaName().equals(name)) {
                Optional<Bean> bean = getEagerly(b.getId());
                result.put(bean.get().getId(), bean.get());
            }
        }
        return result;
    }

    @Override
    public Map<BeanId, Bean> list(String schemaName, Collection<String> ids)
            throws AbortRuntimeException {
        Map<BeanId, Bean> result = new HashMap<>();
        for (Bean bean : storage.all()) {
            String schema = bean.getId().getSchemaName();
            if (!schema.equals(schemaName)) {
                continue;
            }
            for (String id : ids) {
                if (bean.getId().getInstanceId().equals(id)) {
                    result.put(bean.getId(), bean);
                }
            }
        }
        return result;
    }

    @Override
    public void create(Bean bean) {
        checkReferencesExist(bean, new ArrayList<Bean>());
        if (!bean.isDefault()) {
            checkUniquness(bean);
            storage.put(bean);
        } else {
            Bean stored = storage.get(bean.getId());
            if (stored == null) {
                storage.put(bean);
            }
        }
    }

    @Override
    public void create(Collection<Bean> set) {
        // first check uniqueness towards storage
        for (Bean bean : set) {
            checkUniquness(bean);
        }
        // references may not exist in storage, but are provided
        // as part of the transactions, so add them before validating references.



        for (Bean bean : set) {
            checkReferencesExist(bean, set);
        }
        for (Bean bean : set) {
            storage.put(bean);
        }
    }

    @Override
    public void createSingleton(BeanId singleton) {
        Bean bean = Bean.create(singleton);
        try {
            checkUniquness(bean);
        } catch (AbortRuntimeException e) {
            // ignore and return silently.
            return;
        }
        storage.put(bean);
    }

    @Override
    public void set(Bean bean) {
        Bean existing = storage.get(bean.getId());
        if (existing == null) {
            throw CFG304_BEAN_DOESNT_EXIST(bean.getId());

        }
        checkReferencesExist(bean, new ArrayList<Bean>());
        checkInstanceExist(bean);
        storage.put(bean);
    }

    @Override
    public void set(Collection<Bean> set) {
        // TODO: check that provided beans are unique among themselves.

        // references may not exist in storage, but are provided
        // as part of the transactions, so add them before validating references.
        for (Bean bean : set) {
            Bean existing = storage.get(bean.getId());
            if (existing == null) {
                throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
            }
            storage.put(bean);
        }
        for (Bean bean : set) {
            checkReferencesExist(bean, set);
        }
    }

    @Override
    public void merge(Bean bean) {
        Bean b = storage.get(bean.getId());
        if (b == null) {
            throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
        }
        replace(b, bean);
    }

    @Override
    public void merge(Collection<Bean> bean) {
        for (Bean replace : bean) {
            Bean target = storage.get(replace.getId());
            if (target == null) {
                throw Events.CFG304_BEAN_DOESNT_EXIST(replace.getId());
            }
            replace(target, replace);
            storage.put(target);
        }
    }

    private void replace(Bean target, Bean replace) {
        if (target == null) {
            // bean did not exist in storage, create it.
            target = replace;
        }
        checkReferencesExist(replace, new ArrayList<Bean>());
        for (String name : replace.getPropertyNames()) {
            List<String> values = replace.getValues(name);
            if (values == null || values.size() == 0) {
                // null/empty indicates a remove/reset-to-default op
                target.remove(name);
            } else {
                target.setProperty(name, replace.getValues(name));
            }
        }

        for (String name : replace.getReferenceNames()) {
            List<BeanId> values = replace.getReference(name);
            if (values == null || values.size() == 0) {
                // null/empty indicates a remove/reset-to-default op
                target.remove(name);
            } else {
                target.setReferences(name, values);
            }
        }
    }

    @Override
    public Bean delete(BeanId id) {
        checkNoReferencesExist(id);
        checkDeleteDefault(storage.get(id));
        Bean bean = storage.remove(id);
        return bean;
    }

    @Override
    public Collection<Bean> delete(String schemaName, Collection<String> instanceIds) {
        Collection<Bean> deleted = new ArrayList<>();
        for (String instance : instanceIds) {
            checkDeleteDefault(storage.get(BeanId.create(instance, schemaName)));
            checkNoReferencesExist(BeanId.create(instance, schemaName));
            BeanId id = BeanId.create(instance, schemaName);
            if (storage.get(id) == null) {
                throw Events.CFG304_BEAN_DOESNT_EXIST(id);
            }
        }
        for (String instance : instanceIds) {
            BeanId id = BeanId.create(instance, schemaName);
            storage.remove(id);
        }
        return deleted;
    }

    @Override
    public BeanQuery newQuery(Schema schema) {
        Map<BeanId, Bean> beans = list(schema.getName());
        ArrayList<Bean> sorted = new ArrayList<>(beans.values());
        Collections.sort(sorted, new Comparator<Bean>() {
            @Override
            public int compare(Bean b1, Bean b2) {
                return b1.getId().getInstanceId().compareTo(b2.getId().getInstanceId());
            }
        });
        return new DefaultBeanQuery(schema, sorted);
    }

    private static void checkNoReferencesExist(BeanId deleted) {
        Collection<BeanId> hasReferences = new ArrayList<>();
        for (Bean b : storage.all()) {
            if (hasReferences(b, deleted)) {
                hasReferences.add(b.getId());
            }
        }
        if (hasReferences.size() > 0) {
            throw CFG302_CANNOT_DELETE_BEAN(Arrays.asList(deleted));
        }
    }

    private static void checkReferencesExist(final Bean bean, Collection<Bean> additional) {
        HashMap<BeanId, Bean> additionalMap = new HashMap<>();
        for (Bean b : additional) {
            additionalMap.put(b.getId(), b);
        }
        ArrayList<BeanId> allRefs = new ArrayList<>();
        for (String name : bean.getReferenceNames()) {
            if (bean.getReference(name) == null) {
                // the reference is about to be removed.
                continue;
            }
            for (BeanId beanId : bean.getReference(name)) {
                allRefs.add(beanId);
            }
        }

        Collection<BeanId> missingReferences = new ArrayList<>();

        for (BeanId beanId : allRefs) {
            if (beanId.getInstanceId() == null) {
                continue;
            }
            if (storage.get(beanId) == null && additionalMap.get(beanId) == null) {
                missingReferences.add(beanId);
            }
        }
        if (missingReferences.size() > 0) {
            throw CFG301_MISSING_RUNTIME_REF(bean.getId(), missingReferences);
        }
    }

    private static void checkInstanceExist(Bean bean) {
        Collection<Bean> beans = storage.all();
        for (Bean existingBean : beans) {
            if (existingBean.getId().equals(bean.getId())) {
                return;
            }
        }
        throw CFG304_BEAN_DOESNT_EXIST(bean.getId());

    }

    private static void checkUniquness(Bean bean) {
        Collection<Bean> beans = storage.all();

        for (Bean existing : beans) {
            if (bean.getId().equals(existing.getId())) {
                throw CFG303_BEAN_ALREADY_EXIST(bean.getId());
            }
        }
    }

    private static void checkDeleteDefault(Bean bean) {
        if (bean == null) {
            return;
        }
        if (bean.isDefault()) {
            throw CFG311_DEFAULT_REMOVAL(bean.getId());
        }
    }

    /**
     * Returns the a list of property names of the target bean that have
     * references to the bean id.
     */
    private static boolean hasReferences(Bean target, BeanId reference) {
        for (String name : target.getReferenceNames()) {
            List<BeanId> refs = target.getReference(name);
            if (refs != null) {
                for (BeanId ref : target.getReference(name)) {
                    if (ref.equals(reference)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void clear() {
        storage.clear();
    }

    private static final class InMemoryStorage {
        private static final HashMap<BeanId, Bean> beans = new HashMap<>();

        public void put(Bean bean) {
            // disconnect any references that have been set
            for (BeanId id : bean.getReferences()) {
                id.setBean(null);
            }
            beans.put(bean.getId(), bean);
        }

        public Bean get(BeanId id){
            Bean found = beans.get(id);
            return copy(found);
        }

        public Collection<Bean> all() {
            return beans.values();
        }

        public void clear() {
            beans.clear();
        }

        public Bean remove(BeanId id) {
            return beans.remove(id);
        }

        private Bean copy(Bean bean) {
            if (bean == null) {
                return null;
            }
            Bean copy = Bean.create(bean.getId());
            for (String property : bean.getPropertyNames()) {
                copy.setProperty(property, bean.getValues(property));
            }
            for (String property : bean.getReferenceNames()) {
                List<BeanId> ids = bean.getReference(property);
                for (BeanId id : ids) {
                    copy.addReference(property, BeanId.create(id.getInstanceId(), id.getSchemaName()));
                }
            }
            return copy;
        }
    }

    public class DefaultBeanQuery implements BeanQuery {
        private final ArrayList<Bean> beans;
        private final Schema schema;
        private int maxResults = Integer.MAX_VALUE;
        private int firstResult = 0;

        public DefaultBeanQuery(Schema schema, ArrayList<Bean> beans) {
            this.beans = beans;
            this.schema = schema;
        }

        @Override
        public BeanQuery add(BeanRestriction restriction) {
            if (restriction instanceof PropertyRestriction) {
                throw new UnsupportedOperationException("Not implemented yet");
            } else if (restriction instanceof LogicalRestriction) {
                throw new UnsupportedOperationException("Not implemented yet");
            }
            throw new UnsupportedOperationException("Could not identify restriction: " + restriction);
        }

        @Override
        public BeanQuery setFirstResult(int firstResult) {
            this.firstResult = firstResult;
            return this;
        }

        @Override
        public BeanQuery setMaxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }
        @Override
        public List<Bean> retrieve() {
            ArrayList<Bean> result = new ArrayList<>();
            for (int i = 0; i < beans.size(); i++) {
                if (i >= firstResult && result.size() < maxResults) {
                    result.add(beans.get(i));
                }
                if (result.size() > maxResults) {
                    break;
                }
            }
            return result;
        }
    }

}
