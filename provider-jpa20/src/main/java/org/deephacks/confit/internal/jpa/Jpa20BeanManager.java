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
package org.deephacks.confit.internal.jpa;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.internal.core.Lookup;
import org.deephacks.confit.internal.core.SystemProperties;
import org.deephacks.confit.internal.jpa.query.JpaBeanQuery;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.ThreadLocalManager;
import org.deephacks.confit.spi.BeanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.deephacks.confit.internal.jpa.JpaBean.*;
import static org.deephacks.confit.internal.jpa.JpaProperty.deleteProperties;
import static org.deephacks.confit.internal.jpa.JpaProperty.deleteProperty;
import static org.deephacks.confit.internal.jpa.JpaRef.deleteReference;
import static org.deephacks.confit.internal.jpa.JpaRef.deleteReferences;
import static org.deephacks.confit.model.BeanUtils.uniqueIndex;
import static org.deephacks.confit.model.Events.*;

/**
 * JpaBeanManager is responsible for driving transactions, joining/starting and rolling them back.
 *
 * At the moment this Bean Manager is compatible with EclipseLink+Hibernate and MySQL+Postgresql and can
 * be configured to run in any such combination.
 *
 * Because confit file fallback mechanism is (probably) used to configure the
 * EntityManagerFactory, this BeanManager is very careful not to throw errors when
 * fetching configuration if the EntityManagerFactory is not initalized yet (looked-up).
 *
 *  TODO: Mention container-managed vs standalone deployment. Datasource integration and JTA setups.
 */
@Singleton
public final class Jpa20BeanManager extends BeanManager {
    public static final String AUTO_COMMIT_PROP = "beanmanager.jpa.autocommit";
    private static final SystemProperties properties = SystemProperties.instance();
    private static final Logger log = LoggerFactory.getLogger(Jpa20BeanManager.class);
    private static final long serialVersionUID = -1356093069248894779L;
    private boolean autoCommit = true;
    /** we do not own the emf, it MUST be configured by the client and we always
     * Lookup to find it */
    private EntityManagerFactory emf;

    /**
     * Inserting many instances requires some type of batchConfigCo
     * to perform well. Use along with either hibernate.jdbc.batch_size
     * or eclipselink.jdbc.batch-writing.
     */
    private static final int BATCH_SIZE = 20;

    public Jpa20BeanManager() {
        Optional<String> value = properties.get(AUTO_COMMIT_PROP);
        if (value.isPresent()) {
            autoCommit = false;
        }
    }

    @Override
    public void create(Bean bean) {
        create(Arrays.asList(bean));
    }

    @Override
    public void create(Collection <Bean> beans) {
        try {
            if(!begin()) {
                throw JpaEvents.JPA202_MISSING_THREAD_EM();
            }
            int i = 0;
            for (Bean bean : beans) {
                createJpaBean(bean);
                if (i++ % BATCH_SIZE == 0) {
                    getEmOrFail().flush();
                    getEmOrFail().clear();
                }
            }
            getEmOrFail().flush();
            createJpaRefs(beans);
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createSingleton(BeanId singleton) {
        try {
            if(!begin()) {
                throw JpaEvents.JPA202_MISSING_THREAD_EM();
            }
            if (JpaBeanSingleton.isJpaBeanSingleton(singleton.getSchemaName())) {
                // return silently.
                return;
            }
            JpaBeanSingleton jpaBeanSingleton = new JpaBeanSingleton(singleton.getSchemaName());
            getEmOrFail().persist(jpaBeanSingleton);

            createJpaBean(Bean.create(singleton));
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Bean> getSingleton(String schemaName) throws IllegalArgumentException {
        try {
            if (!begin()) {
                return Optional.absent();
            }
            List<Bean> singleton = findEager(schemaName);
            if (singleton.isEmpty()) {
                return Optional.absent();
            }
            if (singleton.size() > 1) {
                throw new IllegalArgumentException(
                        "There are several get instances, which is not allowed.");
            }
            commit();
            return Optional.of(singleton.get(0));
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    private JpaBean createJpaBean(Bean bean) {
        if (exists(bean.getId())) {
            throw CFG303_BEAN_ALREADY_EXIST(bean.getId());
        }
        JpaBean jpaBean = new JpaBean(bean);
        getEmOrFail().persist(jpaBean);
        JpaProperty.markBeanWithProperty(bean);
        createJpaProperties(bean);
        // This is a by-reference call. Clean the property
        // to avoid side-effects in client.
        JpaProperty.unmarkBeanWithProperty(bean);
        return jpaBean;
    }

    private void createJpaRefs(Collection<Bean> beans) {
        int i = 0;
        for (Bean bean : beans) {
            for (String name : bean.getReferenceNames()) {
                List<BeanId> refs = bean.getReference(name);
                if (refs == null) {
                    continue;
                }
                for (BeanId id : refs) {
                    if (!exists(id)) {
                        throw CFG301_MISSING_RUNTIME_REF(bean.getId(), id);
                    }
                    getEmOrFail().persist(new JpaRef(bean.getId(), id, name));
                    if (i++ % BATCH_SIZE == 0) {
                        getEmOrFail().flush();
                        getEmOrFail().clear();
                    }
                }
            }
        }
    }

    private void createJpaProperties(Bean bean) {
        for (String name : bean.getPropertyNames()) {
            List<String> values = bean.getValues(name);
            if (values == null) {
                continue;
            }
            for (String value : values) {
                getEmOrFail().persist(new JpaProperty(bean.getId(), name, value));
            }
        }
    }

    @Override
    public Bean delete(BeanId id) {
        Bean bean = null;
        try {
            if (!begin()) {
                throw JpaEvents.JPA202_MISSING_THREAD_EM();
            }
            bean = deleteJpaBean(id);
            commit();
            return bean;
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            try {
                rollback();
            } catch (NullPointerException e1) {
                /**
                 * Postgres+EclipseLink 2.2.0 have bug that fails with a NPE when trying to rolback an
                 * exception when a constraint is violated.
                 *
                 * Probably idiotic thing to do but this is only for making TCK test pass until
                 * bug have been fixed.
                 *
                 * TODO: https://bugs.eclipse.org/bugs/show_bug.cgi?id=289191
                 */
                throw CFG302_CANNOT_DELETE_BEAN(Arrays.asList(id));
            }
            ExceptionTranslator.translateDelete(Arrays.asList(id), e);
        }
        return bean;
    }

    @Override
    public Collection<Bean> delete(String schemaName, Collection<String> ids) {
        BeanId beanId = null;
        Collection<Bean> deleted = new ArrayList<>();
        try {
            if(!begin()) {
                throw JpaEvents.JPA202_MISSING_THREAD_EM();
            }
            for (String id : ids) {
                beanId = BeanId.create(id, schemaName);
                Bean bean = deleteJpaBean(beanId);
                deleted.add(bean);
            }
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            try {
                rollback();
            } catch (NullPointerException e1) {
                /**
                 * Postgres+EclipseLink 2.2.0 have bug that fails with a NPE when trying to rolback an
                 * exception when a constraint is violated.
                 *
                 * Probably idiotic thing to do but this is only for making TCK test pass until
                 * bug have been fixed.
                 *
                 * TODO: https://bugs.eclipse.org/bugs/show_bug.cgi?id=289191
                 */
                throw CFG302_CANNOT_DELETE_BEAN(Arrays.asList(beanId));
            }
            ExceptionTranslator.translateDelete(ids, schemaName, e);
        }
        return deleted;
    }

    @Override
    public Optional<Bean> getEager(BeanId id) {
        try {
            if (!begin()) {
                return Optional.absent();
            }
            List<Bean> beans = findEager(Sets.newHashSet(id));
            if (beans.size() == 0) {
                return Optional.absent();
            }
            commit();
            return Optional.of(beans.get(0));
        } catch (AbortRuntimeException e) {
            rollback();
            if (id.isSingleton()) {
                return Optional.of(Bean.create(id));
            } else {
                throw e;
            }
        } catch (Throwable e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Bean> getLazy(BeanId id) throws AbortRuntimeException {
        try {
            if (!begin()) {
                return Optional.absent();
            }
            List<Bean> bean = findLazy(Sets.newHashSet(id));
            if (bean.size() == 0) {
                if (id.isSingleton()) {
                    return Optional.of(Bean.create(id));
                }
                return Optional.absent();
            }
            commit();
            return Optional.of(bean.get(0));
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Get a specific instance of a particular schema type with its basic properties initialized.
     * </p>
     *
     * <p>
     * The direct, but no further, successors that references this bean will also be
     * fetched and initalized with their direct, but no further, predecessors.
     * </p>
     * Fetching 1.1.2 in the example below will also fetch its successors 1.1.2.1, 1.1.2.2, but
     * not 1.1.2.1.1, 1.1.2.1.2. Also its predecessor 1.1 and its direct successors 1.1.1, 1.1.3, but not
     * 1.1.3.1.
     * <pre>
     *
     * 1
     * |-- 1.1
     * |   |-- 1.1.1
     * |   |-- 1.1.2
     * |   |   |-- 1.1.2.1
     * |   |   |   |-- 1.1.2.1.1
     * |   |   |   `-- 1.1.2.1.2
     * |   |   `-- 1.1.2.2
     * |   `-- 1.1.3
     * |       `-- 1.1.3.1
     * |-- 1.2
     * |   |-- 1.2.1
     * |   |   |-- 1.2.1.1
     * |   |   `-- 1.2.1.2
     * |   `-- 1.2.1
     * `-- 1.3
     * `-- 1.4
     *  </pre>
     * @see <a href="http://en.wikipedia.org/wiki/Graph_%28mathematics%29#Directed_graph">Directed graph</a>
     * @param beans targeted bean.
     */
    @Override
    public Map<BeanId, Bean> getBeanToValidate(Collection<Bean> beans) throws AbortRuntimeException {
        try {
            if (!begin()) {
                return new HashMap<>();
            }
            Set<BeanId> ids = new HashSet<>();
            for (Bean bean : beans) {
                ids.add(bean.getId());
            }
            Set<Bean> beansToValidate = JpaBean.getBeanToValidate(ids);
            commit();
            return uniqueIndex(beansToValidate);
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;

        } catch (Throwable e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<BeanId, Bean> list(String schemaName) {
        try {
            if (!begin()) {
                return new HashMap<>();
            }
            List<Bean> beans = findEager(schemaName);
            commit();
            return uniqueIndex(beans);
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<BeanId, Bean> list(String schemaName, Collection<String> ids)
            throws AbortRuntimeException {
        List<BeanId> beanIds = new ArrayList<>();
        for (String id : ids) {
            beanIds.add(BeanId.create(id, schemaName));
        }
        List<Bean> beans = findEager(beanIds);
        return uniqueIndex(beans);
    }

    @Override
    public void merge(Bean bean) {
        try {
            if(!begin()) {
                throw JpaEvents.JPA202_MISSING_THREAD_EM();
            }
            mergeJpaBeans(Arrays.asList(bean));
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            ExceptionTranslator.translateMerge(bean.getId(), e);
        }
    }

    @Override
    public void merge(Collection<Bean> beans) {
        try {
            if(!begin()) {
                throw JpaEvents.JPA202_MISSING_THREAD_EM();
            }
            mergeJpaBeans(beans);
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            ExceptionTranslator.translateMerge(e);
        }

    }

    private void mergeJpaBeans(Collection<Bean> beans) {
        Map<BeanId, Bean> mapBeans = uniqueIndex(beans);
        List<Bean> stored = findEager(mapBeans.keySet());
        for (Bean bean : beans) {
            if (!stored.contains(bean)) {
                throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
            }
        }
        mergeProperties(mapBeans, stored);
        mergeReferences(mapBeans, stored);
    }

    private void mergeReferences(Map<BeanId, Bean> mergeBeans, List<Bean> stored) {
        for (Bean store : stored) {
            Bean mergeBean = mergeBeans.get(store.getId());
            for (String name : mergeBean.getReferenceNames()) {
                deleteReference(store.getId(), name);
                List<BeanId> beanIds = mergeBean.getReference(name);
                if (beanIds == null) {
                    continue;
                }
                for (BeanId ref : beanIds) {
                    getEmOrFail().persist(new JpaRef(mergeBean.getId(), ref, name));
                }
            }
        }
    }

    private void mergeProperties(Map<BeanId, Bean> mergeBeans, List<Bean> stored) {
        for (Bean store : stored) {
            Bean mergeBean = mergeBeans.get(store.getId());
            for (String name : mergeBean.getPropertyNames()) {
                deleteProperty(store.getId(), name);
                List<String> values = mergeBean.getValues(name);
                if (values == null) {
                    continue;
                }
                for (String value : values) {
                    getEmOrFail().persist(new JpaProperty(mergeBean.getId(), name, value));
                }
            }
        }
    }

    @Override
    public void set(Bean bean) {
        try {
            if(!begin()) {
                throw JpaEvents.JPA202_MISSING_THREAD_EM();
            }
            setJpaBean(Arrays.asList(bean));
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            throw new RuntimeException(e);
        }

    }

    @Override
    public void set(Collection<Bean> beans) {
        try {
            if(!begin()) {
                throw JpaEvents.JPA202_MISSING_THREAD_EM();
            }
            setJpaBean(beans);
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    private void setJpaBean(Collection<Bean> beans) {
        Map<BeanId, Bean> mapBeans = uniqueIndex(beans);
        List<Bean> stored = findEager(mapBeans.keySet());
        for (Bean bean : beans) {
            if (!stored.contains(bean)) {
                // ignore
            }
        }
        for (Bean bean : beans) {
            deleteProperties(bean.getId());
            deleteReferences(bean.getId());
            createJpaProperties(bean);
        }
        createJpaRefs(beans);
    }

    private EntityManager getEmOrFail()  {
        Optional<EntityManager> optional = getEm();
        if (optional.isPresent()) {
            return optional.get();
        }
        throw JpaEvents.JPA202_MISSING_THREAD_EM();
    }

    private Optional<EntityManager> getEm()  {
        EntityManager em = ThreadLocalManager.peek(EntityManager.class);
        if (em == null) {
            if (emf == null) {
                emf = Lookup.get().lookup(EntityManagerFactory.class);
                if (emf == null) {
                    return Optional.absent();
                }
            }
            em = emf.createEntityManager();
            ThreadLocalManager.push(EntityManager.class, em);
        }
        return Optional.of(em);
    }

    public boolean begin() {
        Optional<EntityManager> optional = getEm();
        if (!optional.isPresent()) {
            return false;
        }
        EntityManager em = optional.get();
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
        }
        return true;
    }

    public void commit() {
        Optional<EntityManager> optional = getEm();
        if (!optional.isPresent()) {
            return;
        }
        EntityManager em  = optional.get();
        if (autoCommit && em.getTransaction().isActive()) {
            em.getTransaction().commit();
            em.clear();
            closeEntityManager();
        } else {
            log.warn("Cannot rollback tx, no transaction is active.");
        }
    }

    public void rollback() {
        EntityManager em = getEmOrFail();
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
            em.clear();
            closeEntityManager();
        } else {
            log.warn("Cannot rollback tx, no transaction is active.");
        }
    }

    public void closeEntityManager() {
        EntityManager manager = ThreadLocalManager.pop(EntityManager.class);
        if (manager == null) {
            log.warn("Cannot close, no EntityManager was found in thread local.");
            return;
        }
        if (!manager.isOpen()) {
            log.warn("Cannot close, EntityManager has already been closed.");
            return;
        }
        manager.close();
    }

    @Override
    public BeanQuery newQuery(Schema schema) {
        return new JpaBeanQuery(schema, getEmOrFail(), this, autoCommit);
    }

}
