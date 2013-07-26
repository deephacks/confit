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
import org.deephacks.confit.model.Bean;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;

/**
 *
 * @author Kristoffer Sjogren
 */
@Entity
@Table(name = "CONFIG_PROPERTY")
@NamedQueries({
        @NamedQuery(name = JpaProperty.DELETE_ALL_PROPERTIES_FOR_BEANID_NAME,
                query = JpaProperty.DELETE_ALL_PROPERTIES_FOR_BEANID),
        @NamedQuery(name = JpaProperty.DELETE_PROPERTY_FOR_BEANID_NAME,
                query = JpaProperty.DELETE_PROPERTY_FOR_BEANID),
        @NamedQuery(name = JpaProperty.FIND_PROPERTIES_FOR_BEANS_DEFAULT_NAME,
                query = JpaProperty.FIND_PROPERTIES_FOR_BEANS_DEFAULT),
        @NamedQuery(name = JpaProperty.FIND_PROPERTIES_FOR_BEANS_HIBERNATE_NAME,
                query = JpaProperty.FIND_PROPERTIES_FOR_BEANS_HIBERNATE),
        @NamedQuery(name = JpaProperty.FIND_PROPERTIES_FOR_SCHEMA_DEFAULT_NAME,
                query = JpaProperty.FIND_PROPERTIES_FOR_SCHEMA_DEFAULT),
        @NamedQuery(name = JpaProperty.FIND_PROPERTIES_FOR_SCHEMA_HIBERNATE_NAME,
                query = JpaProperty.FIND_PROPERTIES_FOR_SCHEMA_HIBERNATE),
        @NamedQuery(name = JpaProperty.FIND_PROPERTIES_FOR_BEAN_NAME,
                query = JpaProperty.FIND_PROPERTIES_FOR_BEAN) })
public class JpaProperty implements Serializable {
    private static final long serialVersionUID = -8467786505761160478L;
    /**
     * This property ensure that there will be one row in the
     * properties table for every bean.
     *
     * The purpose is to reduce number of SQL calls and increase database
     * performance. With this special property, fetching beans and properties
     * need only consult one table and no JOINS are needed.
     */
    public static final String BEAN_MARKER_PROPERTY_NAME = "#BEAN_MARKER_PROPERTY#";

    @Id
    @Column(name = "UUID")
    private String uuid;

    @Column(name = "FK_BEAN_ID", nullable = false)
    private String id;

    @Column(name = "FK_BEAN_SCHEMA_NAME", nullable = false)
    private String schemaName;

    @Column(name = "PROP_NAME", nullable = false)
    private String propName;

    @Column(name = "PROP_VALUE")
    private String value;

    protected static final String DELETE_ALL_PROPERTIES_FOR_BEANID = "DELETE FROM JpaProperty e WHERE e.id = ?1 AND e.schemaName= ?2 AND NOT (e.propName = ?3)";
    protected static final String DELETE_ALL_PROPERTIES_FOR_BEANID_NAME = "DELETE_ALL_PROPERTIES_FOR_BEANID";

    /**
     * Delete list properties EXCEPT the marker. This is useful for 'set' operations
     * that need to clear existing properties.
     */
    public static void deleteProperties(BeanId id) {
        Query query = getEmOrFail().createNamedQuery(DELETE_ALL_PROPERTIES_FOR_BEANID_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());

        query.setParameter(3, BEAN_MARKER_PROPERTY_NAME);
        query.executeUpdate();

    }

    /**
     * Deletes the JpaBean and list its properties and the marker.
     */
    public static void deletePropertiesAndMarker(BeanId id) {
        Query query = getEmOrFail().createNamedQuery(DELETE_ALL_PROPERTIES_FOR_BEANID_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());
        // empty will marker will delete the marker aswell
        query.setParameter(3, "");
        query.executeUpdate();

    }

    protected static final String DELETE_PROPERTY_FOR_BEANID = "DELETE FROM JpaProperty e WHERE e.id = ?1 AND e.schemaName= ?2 AND e.propName = ?3";
    protected static final String DELETE_PROPERTY_FOR_BEANID_NAME = "DELETE_PROPERTY_FOR_BEANID_NAME";

    public static void deleteProperty(BeanId id, String propName) {
        Query query = getEmOrFail().createNamedQuery(DELETE_PROPERTY_FOR_BEANID_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());
        query.setParameter(3, propName);
        query.executeUpdate();
    }

    protected static final String FIND_PROPERTIES_FOR_BEAN = "SELECT e FROM JpaProperty e WHERE e.id= ?1 AND e.schemaName= ?2";
    protected static final String FIND_PROPERTIES_FOR_BEAN_NAME = "FIND_PROPERTIES_FOR_BEAN_NAME";

    @SuppressWarnings("unchecked")
    public static List<JpaProperty> findProperties(BeanId id) {
        Optional<EntityManager> optional = getEm();
        if (!optional.isPresent()) {
            return new ArrayList<>();
        }
        EntityManager em = optional.get();
        Query query = em.createNamedQuery(FIND_PROPERTIES_FOR_BEAN_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());
        List<JpaProperty> properties = (List<JpaProperty>) query.getResultList();
        return properties;
    }

    protected static final String FIND_PROPERTIES_FOR_BEANS_DEFAULT = "SELECT e FROM JpaProperty e WHERE (e.id IN :ids AND e.schemaName IN :schemaNames)";
    protected static final String FIND_PROPERTIES_FOR_BEANS_DEFAULT_NAME = "FIND_PROPERTIES_FOR_BEANS_DEFAULT_NAME";

    protected static final String FIND_PROPERTIES_FOR_BEANS_HIBERNATE = "SELECT e FROM JpaProperty e WHERE (e.id IN (:ids) AND e.schemaName IN (:schemaNames))";
    protected static final String FIND_PROPERTIES_FOR_BEANS_HIBERNATE_NAME = "FIND_PROPERTIES_FOR_BEANS_HIBERNATE_NAME";

    @SuppressWarnings("unchecked")
    public static List<JpaProperty> findProperties(Set<BeanId> beanIds) {
        Optional<EntityManager> optional = getEm();
        if (!optional.isPresent() || beanIds.size() == 0) {
            return new ArrayList<>();
        }
        EntityManager em = optional.get();
        String namedQuery = FIND_PROPERTIES_FOR_BEANS_DEFAULT_NAME;
        if (em.getClass().getName().contains("hibernate")) {
            /**
             * Hibernate and EclipseLink treat IN queries differently.
             * EclipseLink mandates NO brackets, while hibernate mandates WITH brackets.
             * In order to support both, this ugly hack is needed.
             */
            namedQuery = FIND_PROPERTIES_FOR_BEANS_HIBERNATE_NAME;
        }
        Query query = em.createNamedQuery(namedQuery);
        Collection<String> ids = new ArrayList<>();
        Collection<String> schemaNames = new ArrayList<>();
        for (BeanId id : beanIds) {
            ids.add(id.getInstanceId());
            schemaNames.add(id.getSchemaName());
        }
        query.setParameter("ids", ids);
        query.setParameter("schemaNames", schemaNames);

        List<JpaProperty> properties = (List<JpaProperty>) query.getResultList();
        filterUnwantedReferences(properties, beanIds);
        return properties;
    }

    /**
     * Beans with different schemaName may have same instance id.
     *
     * The IN search query is greedy, finding instances that
     * match any combination of instance id and schemaName. Hence,
     * the query may find references belonging to wrong schema
     * so filter those out.
     */
    static void filterUnwantedReferences(List<JpaProperty> result, Set<BeanId> query) {
        ListIterator<JpaProperty> it = result.listIterator();
        while (it.hasNext()) {
            // remove property from result that was not part of the query
            JpaProperty found = it.next();
            if (!query.contains(found.getId())) {
                it.remove();
            }
        }
    }

    protected static final String FIND_PROPERTIES_FOR_SCHEMA_DEFAULT = "SELECT e FROM JpaProperty e WHERE (e.schemaName IN :schemaNames)";
    protected static final String FIND_PROPERTIES_FOR_SCHEMA_DEFAULT_NAME = "FIND_PROPERTIES_FOR_SCHEMA_DEFAULT_NAME";

    protected static final String FIND_PROPERTIES_FOR_SCHEMA_HIBERNATE = "SELECT e FROM JpaProperty e WHERE (e.schemaName IN (:schemaNames))";
    protected static final String FIND_PROPERTIES_FOR_SCHEMA_HIBERNATE_NAME = "FIND_PROPERTIES_FOR_SCHEMA_HIBERNATE_NAME";

    @SuppressWarnings("unchecked")
    public static List<JpaProperty> findProperties(String schemaName) {
        Optional<EntityManager> optional = getEm();
        if (!optional.isPresent()) {
            return new ArrayList<>();
        }
        String namedQuery = FIND_PROPERTIES_FOR_SCHEMA_DEFAULT_NAME;
        EntityManager em = optional.get();
        if (em.getClass().getName().contains("hibernate")) {
            /**
             * Hibernate and EclipseLink treat IN queries differently.
             * EclipseLink mandates NO brackets, while hibernate mandates WITH brackets.
             * In order to support both, this ugly hack is needed.
             */
            namedQuery = FIND_PROPERTIES_FOR_SCHEMA_HIBERNATE_NAME;
        }
        Query query = em.createNamedQuery(namedQuery);
        Collection<String> schemaNames = new ArrayList<>();
        schemaNames.add(schemaName);
        query.setParameter("schemaNames", schemaNames);
        List<JpaProperty> properties = (List<JpaProperty>) query.getResultList();
        return properties;
    }

    private static EntityManager getEmOrFail() {
        Optional<EntityManager> em = getEm();
        if (em.isPresent()) {
            return em.get();
        }
        throw JpaEvents.JPA202_MISSING_THREAD_EM();
    }

    private static Optional<EntityManager> getEm() {
        EntityManager em = ThreadLocalManager.peek(EntityManager.class);
        if (em == null) {
            return Optional.absent();
        }
        return Optional.of(em);
    }

    public JpaProperty() {

    }

    JpaProperty(BeanId owner, String name, String value) {
        this.uuid = UUID.randomUUID().toString();
        this.id = owner.getInstanceId();
        this.schemaName = owner.getSchemaName();
        this.propName = name;
        this.value = value;
    }

    public BeanId getId() {
        return BeanId.create(id, schemaName);
    }

    public String getPropertyName() {
        return propName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * This property ensure that there will be one row in the
     * properties table for every bean.
     *
     * The purpose of creating a special property for a JpaBean is to
     * reduce number of SQL calls and increase database performance.
     * With the marker, we can fetch beans directly from the properties
     * table immediatly without first consulting the beans table.
     */
    public static void markBeanWithProperty(Bean bean) {
        bean.addProperty(BEAN_MARKER_PROPERTY_NAME, "");
    }

    public static void unmarkBeanWithProperty(Bean bean) {
        bean.remove(BEAN_MARKER_PROPERTY_NAME);
    }

    /**
     * This property has no other meaning than knowing that a bean
     * exits only by looking at the properties table.
     *
     * So remove the marker property from result of a fetch operation.
     */
    public static void filterMarkerProperty(List<JpaProperty> properties) {
        ListIterator<JpaProperty> propIt = properties.listIterator();
        while (propIt.hasNext()) {
            if (BEAN_MARKER_PROPERTY_NAME.equals(propIt.next().getPropertyName())) {
                propIt.remove();
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JpaProperty)) {
            return false;
        }
        JpaProperty o = (JpaProperty) obj;
        return equal(uuid, o.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    @Override
    public String toString() {
        return toStringHelper(JpaProperty.class).add("propertyName", getPropertyName())
                .add("value", getValue()).toString();
    }
}
