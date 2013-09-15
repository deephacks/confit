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
package org.deephacks.confit.internal.mapdb;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.admin.query.BeanQueryBuilder.BeanRestriction;
import org.deephacks.confit.admin.query.BeanQueryBuilder.LogicalRestriction;
import org.deephacks.confit.admin.query.BeanQueryBuilder.PropertyRestriction;
import org.deephacks.confit.admin.query.BeanQueryResult;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.Lookup;
import org.mapdb.TxMaker;

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

public class MapdbBeanManager extends BeanManager {
    private final MapDB mapDB;

    public MapdbBeanManager() {
        TxMaker txMaker = Lookup.get().lookup(TxMaker.class);
        Preconditions.checkNotNull(txMaker);
        mapDB = new MapDB(txMaker);
    }

    @Override
    public Optional<Bean> getEager(BeanId id) {
        try {
            Optional<Bean> bean = getEagerly(id);
            mapDB.commit();
            return bean;
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
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
        Bean bean = found.get(id) == null ? mapDB.get(id) : found.get(id);
        if (bean == null) {
            return;
        }
        found.put(id, bean);
        // bean found, fetch references.
        for (BeanId ref : bean.getReferences()) {
            if (ref.getBean() != null) {
                continue;
            }
            Bean refBean = found.get(ref) == null ? mapDB.get(ref) : found.get(ref);
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
        try {
            Bean bean = mapDB.get(id);
            if (bean == null) {
                return Optional.absent();
            }
            for (BeanId ref : bean.getReferences()) {
                Bean refBean = mapDB.get(ref);
                if (refBean == null) {
                    throw CFG301_MISSING_RUNTIME_REF(ref);
                }
                ref.setBean(refBean);
            }
            mapDB.commit();
            return Optional.of(bean);
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * The direct, but no further, successors that references this bean will also be
     * fetched and initialized with their direct, but no further, predecessors.
     */
    @Override
    public Map<BeanId, Bean> getBeanToValidate(Collection<Bean> beans) throws AbortRuntimeException {
        try {
            Map<BeanId, Bean> beansToValidate = new HashMap<>();
            for (Bean bean : beans) {
                Map<BeanId, Bean> predecessors = new HashMap<>();
                // beans read from beansStorage will only have their basic properties initialized...
                // ... but we also need set the direct references/predecessors for beans to validate
                Map<BeanId, Bean> beansToValidateSubset = getDirectSuccessors(bean);
                beansToValidateSubset.put(bean.getId(), bean);
                for (Bean toValidate : beansToValidateSubset.values()) {
                    predecessors.putAll(getDirectPredecessors(toValidate));
                }

                for (Bean predecessor : predecessors.values()) {
                    for (BeanId ref : predecessor.getReferences()) {
                        Bean b = mapDB.get(ref);
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
            mapDB.commit();
            return beansToValidate;
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }


    private Map<BeanId, Bean> getDirectPredecessors(Bean bean) {
        Map<BeanId, Bean> predecessors = new HashMap<>();
        for (BeanId ref : bean.getReferences()) {
            Bean predecessor = mapDB.get(ref);
            if (predecessor == null) {
                throw CFG304_BEAN_DOESNT_EXIST(ref);
            }
            predecessors.put(predecessor.getId(), predecessor);
        }
        return predecessors;
    }

    private Map<BeanId, Bean> getDirectSuccessors(Bean bean) {
        Map<BeanId, Bean> successors = new HashMap<>();
        for (BeanId id : bean.getReferences()) {
            Bean successor = mapDB.get(id);
            if (successor == null) {
                throw Events.CFG301_MISSING_RUNTIME_REF(id);
            }
            successors.put(successor.getId(), successor);
        }
        return successors;
    }

    @Override
    public Optional<Bean> getSingleton(String schemaName) throws IllegalArgumentException {
        try {
            Optional<Bean> bean = getEagerly(BeanId.createSingleton(schemaName));
            if (bean.isPresent()) {
                return bean;
            }
            mapDB.commit();
            return Optional.of(Bean.create(BeanId.createSingleton(schemaName)));
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<BeanId, Bean> list(String name) {
        try {
            Map<BeanId, Bean> result = new HashMap<>();
            for (Bean b : mapDB.list(name)) {
                if (b.getId().getSchemaName().equals(name)) {
                    Optional<Bean> bean = getEagerly(b.getId());
                    result.put(bean.get().getId(), bean.get());
                }
            }
            mapDB.commit();
            return result;
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<BeanId, Bean> list(String schemaName, Collection<String> ids)
            throws AbortRuntimeException {
        try {
            Map<BeanId, Bean> result = new HashMap<>();
            for (Bean bean : mapDB.list(schemaName)) {
                for (String id : ids) {
                    if (bean.getId().getInstanceId().equals(id)) {
                        result.put(bean.getId(), bean);
                    }
                }
            }
            mapDB.commit();
            return result;
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(Bean bean) {
        try {
            checkUniquness(bean);
            checkReferencesExist(bean, new ArrayList<Bean>());
            mapDB.put(bean);
            mapDB.commit();
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(Collection<Bean> set) {
        try {
            // first check uniqueness towards beansStorage
            for (Bean bean : set) {
                checkUniquness(bean);
            }
            // references may not exist in beansStorage, but are provided
            // as part of the transactions, so add them before validating references.
            for (Bean bean : set) {
                checkReferencesExist(bean, set);
            }
            for (Bean bean : set) {
                mapDB.put(bean);
            }
            mapDB.commit();
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createSingleton(BeanId singleton) {
        try {

            Bean bean = Bean.create(singleton);
            try {
                checkUniquness(bean);
            } catch (AbortRuntimeException e) {
                // ignore and return silently.
                return;
            }
            mapDB.put(bean);
            mapDB.commit();
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(final Bean bean) {
        try {

            Bean existing = mapDB.get(bean.getId());
            if (existing == null) {
                throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
            }
            checkReferencesExist(bean, new ArrayList<Bean>());
            mapDB.put(bean);
            mapDB.commit();
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(Collection<Bean> set) {
        try {
            // references may not exist in beansStorage, but are provided
            // as part of the transactions, so add them before validating references.
            for (Bean bean : set) {
                Bean existing = mapDB.get(bean.getId());
                if (existing == null) {
                    throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
                }
                mapDB.put(bean);
            }
            for (Bean bean : set) {
                checkReferencesExist(bean, set);
            }
            mapDB.commit();
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(Bean bean) {
        try {
            Bean b = mapDB.get(bean.getId());
            if (b == null) {
                throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
            }
            replace(b, bean);
            mapDB.commit();
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(Collection<Bean> bean) {
        try {
            for (Bean replace : bean) {
                Bean target = mapDB.get(replace.getId());
                if (target == null) {
                    throw Events.CFG304_BEAN_DOESNT_EXIST(replace.getId());
                }
                replace(target, replace);
                mapDB.put(target);
            }
            mapDB.commit();
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    private void replace(Bean target, Bean replace) {
        if (target == null) {
            // bean did not exist in beansStorage, create it.
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
        try {
            checkNoReferencesExist(id);
            checkDeleteDefault(mapDB.get(id));
            Bean bean = mapDB.remove(id);
            mapDB.commit();
            return bean;
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Bean> delete(String schemaName, Collection<String> instanceIds) {
        try {
            Collection<Bean> deleted = new ArrayList<>();
            for (String instance : instanceIds) {
                checkDeleteDefault(mapDB.get(BeanId.create(instance, schemaName)));
                checkNoReferencesExist(BeanId.create(instance, schemaName));
                BeanId id = BeanId.create(instance, schemaName);
                if (mapDB.get(id) == null) {
                    throw Events.CFG304_BEAN_DOESNT_EXIST(id);
                }
            }
            for (String instance : instanceIds) {
                BeanId id = BeanId.create(instance, schemaName);
                mapDB.remove(id);
            }
            mapDB.commit();
            return deleted;
        } catch (AbortRuntimeException e) {
            mapDB.rollback(e);
            throw e;
        } catch (Throwable e) {
            mapDB.rollback(e);
            throw new RuntimeException(e);
        }
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

    private void checkNoReferencesExist(BeanId deleted) {
        Collection<BeanId> hasReferences = new ArrayList<>();
        for (Bean b : mapDB.values()) {
            if (hasReferences(b, deleted)) {
                hasReferences.add(b.getId());
            }
        }
        if (hasReferences.size() > 0) {
            throw CFG302_CANNOT_DELETE_BEAN(Arrays.asList(deleted));
        }
    }

    private void checkReferencesExist(final Bean bean, Collection<Bean> inflight) {
        HashMap<BeanId, Bean> inflightBeans = new HashMap<>();
        for (Bean b : inflight) {
            inflightBeans.put(b.getId(), b);
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
            if (mapDB.get(beanId) == null && inflightBeans.get(beanId) == null) {
                missingReferences.add(beanId);
            }
        }
        if (missingReferences.size() > 0) {
            throw CFG301_MISSING_RUNTIME_REF(bean.getId(), missingReferences);
        }
    }

    private void checkUniquness(Bean bean) {
        Bean found = mapDB.get(bean.getId());
        if (found != null) {
            throw CFG303_BEAN_ALREADY_EXIST(bean.getId());
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

    public class DefaultBeanQuery implements BeanQuery {
        private final ArrayList<Bean> beans;
        private final Schema schema;
        private int maxResults = Integer.MAX_VALUE;
        private int firstResult;

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
        public BeanQuery setFirstResult(String firstResult) {
            try {
                this.firstResult = Integer.parseInt(firstResult);
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not parse firstResult into an integer.");
            }
            return this;
        }

        @Override
        public BeanQuery setMaxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        @Override
        public BeanQueryResult retrieve() {
            final ArrayList<Bean> result = new ArrayList<>();
            int firstResult = 0;
            for (int i = 0; i < beans.size(); i++) {
                firstResult = i + 1;
                if (i >= firstResult && result.size() < maxResults) {
                    result.add(beans.get(i));
                }
                if (result.size() > maxResults) {
                    break;
                }
            }
            final String nextFirstResult = Integer.toString(firstResult);
            return new BeanQueryResult() {
                @Override
                public List<Bean> get() {
                    return result;
                }

                @Override
                public String nextFirstResult() {
                    return nextFirstResult;
                }
            };
        }
    }
}
