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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.ThreadLocalManager;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Objects.equal;

@Entity
@Table(name = "CONFIG_BEAN_REF")
@NamedQueries({
        @NamedQuery(name = JpaRef.DELETE_REF_USING_BEANID_NAME,
                query = JpaRef.DELETE_REF_USING_BEANID),
        @NamedQuery(name = JpaRef.DELETE_REF_USING_PROPNAME_NAME,
                query = JpaRef.DELETE_REF_USING_PROPNAME),
        @NamedQuery(name = JpaRef.FIND_REFS_FOR_BEANS_HIBERNATE_NAME,
                query = JpaRef.FIND_REFS_FOR_BEANS_HIBERNATE),
        @NamedQuery(name = JpaRef.FIND_REFS_FOR_BEANS_DEFAULT_NAME,
                query = JpaRef.FIND_REFS_FOR_BEANS_DEFAULT),
        @NamedQuery(name = JpaRef.FIND_PREDECESSORS_FOR_BEAN_DEFAULT_NAME,
                query = JpaRef.FIND_PREDECESSORS_FOR_BEAN_DEFAULT),
        @NamedQuery(name = JpaRef.FIND_PREDECESSORS_FOR_BEAN_HIBERNATE_NAME,
                query = JpaRef.FIND_PREDECESSORS_FOR_BEAN_HIBERNATE)})
public class JpaRef implements Serializable {

    private static final long serialVersionUID = -3528959706883881047L;

    @Id
    @Column(name = "UUID")
    String id;

    @Column(name = "FK_SOURCE_BEAN_ID", nullable = false)
    protected String sourceId;

    @Column(name = "FK_SOURCE_BEAN_SCHEMA_NAME", nullable = false)
    protected String sourceSchemaName;

    @Column(name = "FK_TARGET_BEAN_ID", nullable = false)
    protected String targetId;

    @Column(name = "FK_TARGET_BEAN_SCHEMA_NAME", nullable = false)
    protected String targetSchemaName;

    @Column(name = "PROP_NAME")
    private String propertyName;

    protected static final String DELETE_REF_USING_BEANID = "DELETE FROM JpaRef e WHERE e.sourceId = ?1 AND e.sourceSchemaName= ?2";
    protected static final String DELETE_REF_USING_BEANID_NAME = "DELETE_REF_USING_BEANID_NAME";
    @Transient
    private JpaBean target;

    public static void deleteReferences(BeanId id) {
        Query query = getEm().createNamedQuery(DELETE_REF_USING_BEANID_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());
        query.executeUpdate();
    }

    protected static final String DELETE_REF_USING_PROPNAME = "DELETE FROM JpaRef e WHERE e.sourceId = ?1 AND e.sourceSchemaName= ?2 AND  e.propertyName= ?3";
    protected static final String DELETE_REF_USING_PROPNAME_NAME = "DELETE_REF_USING_PROPNAME_NAME";

    public static void deleteReference(BeanId id, String propName) {
        Query query = getEm().createNamedQuery(DELETE_REF_USING_PROPNAME_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());
        query.setParameter(3, propName);
        query.executeUpdate();
    }

    protected static final String FIND_REFS_FOR_BEANS_DEFAULT = "SELECT e FROM JpaRef e WHERE (e.sourceId IN :ids AND e.sourceSchemaName IN :schemaNames)";
    protected static final String FIND_REFS_FOR_BEANS_DEFAULT_NAME = "FIND_REFS_FOR_BEANS_DEFAULT_NAME";

    protected static final String FIND_REFS_FOR_BEANS_HIBERNATE = "SELECT e FROM JpaRef e WHERE (e.sourceId IN (:ids) AND e.sourceSchemaName IN (:schemaNames))";
    protected static final String FIND_REFS_FOR_BEANS_HIBERNATE_NAME = "FIND_REFS_FOR_BEANS_HIBERNATE_NAME";

    @SuppressWarnings("unchecked")
    public static Multimap<BeanId, JpaRef> findReferences(Set<BeanId> beanIds) {
        Multimap<BeanId, JpaRef> refs = ArrayListMultimap.create();
        if (beanIds.size() == 0) {
            return refs;
        }
        String namedQuery = FIND_REFS_FOR_BEANS_DEFAULT_NAME;
        if (getEm().getClass().getName().contains("hibernate")) {
            /**
             * Hibernate and EclipseLink treat IN queries differently.
             * EclipseLink mandates NO brackets, while hibernate mandates WITH brackets.
             * In order to support both, this ugly hack is needed.
             */
            namedQuery = FIND_REFS_FOR_BEANS_HIBERNATE_NAME;
        }
        Query query = getEm().createNamedQuery(namedQuery);
        Collection<String> ids = new ArrayList<String>();
        Collection<String> schemaNames = new ArrayList<String>();
        for (BeanId id : beanIds) {
            ids.add(id.getInstanceId());
            schemaNames.add(id.getSchemaName());
        }
        query.setParameter("ids", ids);
        query.setParameter("schemaNames", schemaNames);
        List<JpaRef> result = (List<JpaRef>) query.getResultList();
        filterUnwantedReferences(result, beanIds);
        for (JpaRef jpaRef : result) {
            refs.put(jpaRef.getSource(), jpaRef);
        }
        return refs;
    }

    /**
     * Beans with different schemaName may have same instance id.
     *
     * The IN search query is greedy, finding instances that
     * match any combination of instance id and schemaName. Hence,
     * the query may find references belonging to wrong schema
     * so filter those out.
     */
    static void filterUnwantedReferences(List<JpaRef> result, Collection<BeanId> query) {
        ListIterator<JpaRef> it = result.listIterator();
        while (it.hasNext()) {
            // remove reference from result that was not part of the query
            BeanId found = it.next().getSource();
            if (!query.contains(found)) {
                it.remove();
            }
        }
    }

    protected static final String FIND_PREDECESSORS_FOR_BEAN_DEFAULT = "SELECT e FROM JpaRef e WHERE (e.targetId IN :ids AND e.targetSchemaName IN :schemaNames)";
    protected static final String FIND_PREDECESSORS_FOR_BEAN_DEFAULT_NAME = "FIND_PREDECESSORS_FOR_BEAN_DEFAULT_NAME";

    protected static final String FIND_PREDECESSORS_FOR_BEAN_HIBERNATE = "SELECT e FROM JpaRef e WHERE (e.targetId IN (:ids) AND e.targetSchemaName IN (:schemaNames))";
    protected static final String FIND_PREDECESSORS_FOR_BEAN_HIBERNATE_NAME = "FIND_PREDECESSORS_FOR_BEAN_HIBERNATE_NAME";

    @SuppressWarnings("unchecked")
    public static List<JpaRef> getDirectPredecessors(Set<BeanId> beanIds) {
        if (beanIds.size() == 0) {
            return new ArrayList<JpaRef>();
        }
        String namedQuery = FIND_PREDECESSORS_FOR_BEAN_DEFAULT_NAME;
        if (getEm().getClass().getName().contains("hibernate")) {
            /**
             * Hibernate and EclipseLink treat IN queries differently.
             * EclipseLink mandates NO brackets, while hibernate mandates WITH brackets.
             * In order to support both, this ugly hack is needed.
             */
            namedQuery = FIND_PREDECESSORS_FOR_BEAN_HIBERNATE_NAME;
        }

        Query query = getEm().createNamedQuery(namedQuery);
        Collection<String> ids = new ArrayList<String>();
        Collection<String> schemaNames = new ArrayList<String>();
        for (BeanId id : beanIds) {
            ids.add(id.getInstanceId());
            schemaNames.add(id.getSchemaName());
        }
        query.setParameter("ids", ids);
        query.setParameter("schemaNames", schemaNames);
        List<JpaRef> result = (List<JpaRef>) query.getResultList();
        return result;
    }

    private static EntityManager getEm() {
        EntityManager em = ThreadLocalManager.peek(EntityManager.class);
        if (em == null) {
            throw JpaEvents.JPA202_MISSING_THREAD_EM();
        }
        return em;
    }

    public JpaRef() {

    }

    public JpaRef(JpaBean source, JpaBean target, String propName) {
        this.sourceId = source.getId().getInstanceId();
        this.sourceSchemaName = source.getId().getSchemaName();
        this.targetId = target.getId().getInstanceId();
        this.targetSchemaName = target.getId().getSchemaName();
        this.propertyName = propName;
        this.id = UUID.randomUUID().toString();
    }

    public JpaRef(BeanId source, BeanId target, String propName) {
        this.sourceId = source.getInstanceId();
        this.sourceSchemaName = source.getSchemaName();
        this.targetId = target.getInstanceId();
        this.targetSchemaName = target.getSchemaName();
        this.propertyName = propName;
        this.id = UUID.randomUUID().toString();
    }

    public String getPropertyName() {
        return propertyName;
    }

    public BeanId getSource() {
        return BeanId.create(sourceId, sourceSchemaName);
    }

    public BeanId getTarget() {
        return BeanId.create(targetId, targetSchemaName);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JpaRef)) {
            return false;
        }
        JpaRef o = (JpaRef) obj;
        return equal(id, o.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(JpaRef.class).add("propertyName", propertyName)
                .add("source", sourceId + "@" + sourceSchemaName)
                .add("target", targetId + "@" + targetSchemaName).toString();
    }

    public void setTargetBean(JpaBean target) {
        this.target = target;
    }

    public JpaBean getTargetBean() {
        return target;
    }

}
