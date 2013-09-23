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

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.model.ThreadLocalManager;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static org.deephacks.confit.internal.jpa.JpaProperty.deletePropertiesAndMarker;

/**
 * JpaBean is a jpa entity that represent a Bean.
 *
 * @author Kristoffer Sjogren
 */
@Entity
@Table(name = "CONFIG_BEAN")
@NamedQueries({
        @NamedQuery(name = JpaBean.FIND_BEAN_FROM_BEANID_NAME,
                query = JpaBean.FIND_BEAN_FROM_BEANID),
        @NamedQuery(name = JpaBean.FIND_BEANS_FROM_SCHEMA_NAME,
                query = JpaBean.FIND_BEANS_FROM_SCHEMA),
        @NamedQuery(name = JpaBean.DELETE_BEAN_USING_BEANID_NAME,
                query = JpaBean.DELETE_BEAN_USING_BEANID)})
public class JpaBean implements Serializable {
    private static final long serialVersionUID = -4097243985344046349L;
    @EmbeddedId
    private JpaBeanPk pk;

    public static List<Bean> findEager(Set<BeanId> ids) {
        if (ids.size() == 0) {
            return new ArrayList<>();
        }
        JpaBeanQueryAssembler query = new JpaBeanQueryAssembler(ids);
        // collect references recursively
        collectRefs(ids, query, 10);

        // fetch properties for list beans at once
        List<JpaProperty> allProperties = JpaProperty.findProperties(query.getIds());
        query.addProperties(allProperties);

        return query.assembleBeans();
    }

    public static Bean findEager(BeanId id) {
        List<Bean> beans = findEager(Sets.newHashSet(id));
        if (beans == null || beans.size() == 0) {
            return null;
        }
        return beans.get(0);
    }

    public static List<Bean> findEager(List<BeanId> ids) {
        /**
         * TODO: Need to take care of BeanId  that may apear more than once in 'ids'.
         */
        return findEager(Sets.newHashSet(ids));
    }

    private static void collectRefs(Set<BeanId> predecessors, JpaBeanQueryAssembler query, int level) {
        if (--level < 0) {
            return;
        }
        Multimap<BeanId, JpaRef> successors = JpaRef.findReferences(predecessors);
        if (successors.size() > 0) {
            query.addRefs(predecessors);
        }
        // only recurse successors we havent already visited to break circular references
        Set<BeanId> unvisitedSuccessors = new HashSet<>();
        for (JpaRef successor : successors.values()) {
            if (!query.contains(successor.getTarget())) {
                unvisitedSuccessors.add(successor.getTarget());
            }
        }
        if (unvisitedSuccessors.size() != 0) {
            // we have reached the end and found list successors
            collectRefs(unvisitedSuccessors, query, level);
        }
        query.addRefs(successors);
    }

    /**
     * Finds the provided beans and initalize their properties and direct
     * references (but no further).
     */
    public static List<Bean> findLazy(Set<BeanId> ids) {
        if (ids.size() == 0) {
            return new ArrayList<>();
        }
        JpaBeanQueryAssembler query = new JpaBeanQueryAssembler(ids);
        List<JpaProperty> allProperties = JpaProperty.findProperties(query.getIds());
        query.addProperties(allProperties);
        Multimap<BeanId, JpaRef> refs = JpaRef.findReferences(ids);
        if (refs.size() > 0) {
            query.addRefs(refs);
        }
        return query.assembleBeans();
    }

    protected static final String FIND_BEANS_FROM_SCHEMA = "SELECT DISTINCT e FROM JpaBean e WHERE e.pk.schemaName= ?1 ORDER BY e.pk.id";
    protected static final String FIND_BEANS_FROM_SCHEMA_NAME = "FIND_BEANS_FROM_SCHEMA_NAME";

    @SuppressWarnings("unchecked")
    public static List<Bean> findEager(String schemaName) {
        Optional<EntityManager> em = getEm();
        if (!em.isPresent()) {
            return new ArrayList<>();
        }
        Query query = em.get().createNamedQuery(FIND_BEANS_FROM_SCHEMA_NAME);
        query.setParameter(1, schemaName);
        List<JpaBean> beans = (List<JpaBean>) query.getResultList();
        Set<BeanId> ids = new HashSet<>();
        for (JpaBean jpaBean : beans) {
            ids.add(jpaBean.getId());
        }
        return findEager(ids);
    }

    /**
     * Will return the target bean and its direct predecessors for validation
     */
    public static Set<Bean> getBeanToValidate(Set<BeanId> ids) {
        List<JpaRef> targetPredecessors = JpaRef.getDirectPredecessors(ids);
        Set<BeanId> beansToValidate = new HashSet<>();
        for (JpaRef ref : targetPredecessors) {
            beansToValidate.add(ref.getSource());
        }
        beansToValidate.addAll(ids);
        JpaBeanQueryAssembler query = new JpaBeanQueryAssembler(beansToValidate);
        collectRefs(beansToValidate, query, 2);
        List<JpaProperty> allProperties = JpaProperty.findProperties(query.getIds());
        query.addProperties(allProperties);

        return new HashSet<>(query.assembleBeans());
    }

    protected static final String FIND_BEAN_FROM_BEANID = "SELECT DISTINCT e FROM JpaBean e WHERE e.pk.id = ?1 AND e.pk.schemaName= ?2";
    protected static final String FIND_BEAN_FROM_BEANID_NAME = "FIND_BEAN_FROM_BEANID_NAME";

    public static boolean exists(BeanId id) {
        Query query = getEmOrFail().createNamedQuery(FIND_BEAN_FROM_BEANID_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());
        try {
            query.getSingleResult();
        } catch (NoResultException e) {
            return false;
        }
        return true;
    }

    /**
     * Need not consult the JpaBean table since the property marker
     * in JpaProperties table is an indicator that the JpaBean really
     * exist, even if it has no properties.
     */
    @SuppressWarnings("unused")
    private static JpaBean getJpaBeanAndProperties(BeanId id) {
        List<JpaProperty> props = JpaProperty.findProperties(id);
        if (props.size() == 0) {
            // no marker, bean does not exist
            return null;
        }
        JpaBean bean = new JpaBean(new JpaBeanPk(id));
        JpaProperty.filterMarkerProperty(props);
        bean.properties.addAll(props);
        return bean;
    }

    protected static final String DELETE_BEAN_USING_BEANID = "DELETE FROM JpaBean e WHERE e.pk.id = ?1 AND e.pk.schemaName= ?2";
    protected static final String DELETE_BEAN_USING_BEANID_NAME = "DELETE_BEAN_USING_BEANID_NAME";

    public static Bean deleteJpaBean(BeanId id) {
        Bean bean = findEager(id);
        deletePropertiesAndMarker(id);
        JpaRef.deleteReferences(id);
        Query query = getEmOrFail().createNamedQuery(DELETE_BEAN_USING_BEANID_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());
        query.executeUpdate();
        return bean;
    }

    private static EntityManager getEmOrFail() {
        Optional<EntityManager> em = getEm();
        if (!em.isPresent()) {
            throw JpaEvents.JPA202_MISSING_THREAD_EM();
        }
        return em.get();
    }

    private static Optional<EntityManager> getEm() {
        EntityManager em = ThreadLocalManager.peek(EntityManager.class);
        if (em == null) {
            return Optional.absent();
        }
        return Optional.of(em);
    }

    @Transient
    private Set<JpaRef> references = new HashSet<>();
    @Transient
    private Set<JpaProperty> properties = new HashSet<>();

    public JpaBean() {

    }

    JpaBean(Bean b) {
        this.pk = new JpaBeanPk(b.getId());
    }

    JpaBean(JpaBeanPk pk) {
        this.pk = pk;
    }

    public JpaBeanPk getPk() {
        return pk;
    }

    public BeanId getId() {
        return BeanId.create(pk.id, pk.schemaName);
    }

    public Set<JpaRef> getReferences() {
        return references;
    }

    public void addReference(JpaRef ref) {
        references.add(ref);
    }

    public Set<JpaProperty> getProperties() {
        return properties;
    }

    public void addProperty(String name, List<String> values) {
        for (String value : values) {
            properties.add(new JpaProperty(getId(), name, value));
        }
    }

    public void addProperty(JpaProperty prop) {
        properties.add(prop);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JpaBean)) {
            return false;
        }
        JpaBean o = (JpaBean) obj;
        return equal(pk, o.pk);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pk);
    }

    @Override
    public String toString() {
        return toStringHelper(JpaBean.class).add("pk", pk).toString();
    }

}
