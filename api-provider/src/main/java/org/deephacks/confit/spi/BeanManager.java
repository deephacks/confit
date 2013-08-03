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
package org.deephacks.confit.spi;

import com.google.common.base.Optional;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.admin.query.BeanQueryBuilder;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Bean Manager is used to create and remove bean instances, to find
 * beans by their name, and to query over beans.
 * </p>
 * <p>
 * How, when and with what gurantees data are stored (consistency, reliability etc)
 * in the underlying storage and published to applications are coordinated by this
 * manager.
 * </p>
 * <p>
 * Bean Manager is also responsible for enforcing referential integrity between
 * beans and making sure that links arent brooken. This is the only type of
 * constraint that is checked, a bean manager is not concerned with schema
 * or application validation of any kind.
 * </p>
 * <p>
 * A bean manager should assume that its users already are authorized users,
 * maybe authorized through an external security mechanism.
 * </p>
 * <p>
 * Bean managers are free (and encouraged, but not forced) to implement support
 * for participating in JTA transactions.
 * </p>
 *
 * @author Kristoffer Sjogren
 */
public abstract class BeanManager implements Serializable {

    private static final long serialVersionUID = -246410305338556633L;
    private static Lookup lookup = Lookup.get();

    /**
     * Lookup the most suitable BeanManager available.
     *
     * @return BeanManager.
     */
    public static BeanManager lookup() {
        return lookup.lookup(BeanManager.class);
    }

    /**
     * Creates a new bean.
     *
     * <p>
     * If the bean have references to other beans, the bean manager
     * make sure that referential integrity is satisfied (that provided
     * references exist).
     * </p>
     * <p>
     * Initialized bean references will not be traversed and created
     * recursively/eagerly.
     * </p>
     * @param bean to create
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void create(final Bean bean) throws AbortRuntimeException;

    /**
     * This is the collection variant of {@link BeanManager#create(Bean)}.
     *
     * @see #create(Bean)
     * @param beans create
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void create(final Collection<Bean> beans) throws AbortRuntimeException;

    /**
     * Create a singleton instance. This method will return silently if the instance
     * already exist.
     *
     * Bean Manager must guarantee that no other instances of this BeanId are created,
     * nor that this singleton is removed.
     *
     * @param singleton the singleton.
     */
    public abstract void createSingleton(final BeanId singleton);

    /**
     * Replace (set) an existing bean instance with provided data.
     * <p>
     * Already persisted properties and bean references associated with the instance
     * will be removed if they are missing from provided bean instances.
     * </p>
     * If the bean have references to other beans, the bean manager make sure that
     * referential integrity is satisfied (that provided references exist).
     * <p>
     * Initialized bean references not will be traversed and set recursively/eagerly.
     * </p>
     *
     * @param bean to set
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void set(final Bean bean);

    /**
     * This is the collection variant of {@link BeanManager#set(Bean)}.
     *
     * @see #set(Bean)
     * @param bean to set
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void set(final Collection<Bean> bean) throws AbortRuntimeException;

    /**
     * Merges the provided properties with an already existing
     * instance.
     * <p>
     * Already persisted properties and bean references associated  with the
     * instance will be overwritten if they also exist on the provided bean.
     * This is true for collection properties aswell.
     * </p>
     * <p>
     * Values not provided will remain untouched in storage, hence this method
     * can be used to set or delete a single property.
     * </p>
     * <p>
     * Initialized bean references not will be traversed and merged
     * recursively/eagerly.
     * </p>
     *
     * @param bean The Bean does not have to have its Bean Info
     *            provided with it.
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void merge(final Bean bean) throws AbortRuntimeException;

    /**
     * This is the collection variant of {@link BeanManager#merge(Bean)}.
     *
     * @see #merge(Bean)
     * @param bean to merge
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void merge(final Collection<Bean> bean) throws AbortRuntimeException;

    /**
     * <p>
     * Get a specific instance of a particular schema type.
     * </p>
     *
     * <p>
     * Beans will have their basic properties initialized and all
     * references traversed and fetched eagerly.
     * </p>
     *
     * @param id to lookup
     * @return A set of beans
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract Optional<Bean> getEager(final BeanId id) throws AbortRuntimeException;

    /**
     * <p>
     * Get a specific instance of a particular schema type.
     * </p>
     * <p>
     * Beans will have their basic properties and all
     * references initialized, but not traversed and fetched eagerly.
     * </p>
     * @param id to lookup
     * @return Bean
     * @throws AbortRuntimeException
     */
    public abstract Optional<Bean> getLazy(final BeanId id) throws AbortRuntimeException;

    /**
     * <p>
     * Bean Manager is responsible for fetching the beans that are to validated when changes
     * are made to specific bean instance. The list should contain all the beans that are to
     * be validated. The bean manager is free to return all or a subset of beans.
     * </p>
     * <p>
     * This method is optional for bean managers to implement.
     * </p>
     *
     * @param beans targeted bean.
     * @return Instances to validate.
     * @throws AbortRuntimeException
     */
    public Map<BeanId, Bean> getBeanToValidate(final Collection<Bean> beans)
            throws AbortRuntimeException {
        return new HashMap<>();
    }

    /**
     * Get a singleton instance for a particular schema.
     *
     * @param schemaName The singleton schema.
     * @return The singleton bean.
     * @throws IllegalArgumentException if the schema name is not a singleton.
     */
    public abstract Optional<Bean> getSingleton(final String schemaName) throws IllegalArgumentException;

    /**
     * <p>
     * List all instances of a specific schema type.
     * </p>
     * <p>
     * Beans will have their basic properties initialized and all references traversed
     * and fetched eagerly.
     * </p>
     *
     * @param schemaName the type of beans to match.
     * @return A set of beans.
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract Map<BeanId, Bean> list(final String schemaName) throws AbortRuntimeException;

    /**
     * <p>
     * List specific instances of a specific schema type.
     * </p>
     * <p>
     * Beans will have their basic properties initialized and all references traversed
     * and fetched eagerly.
     * </p>
     *
     * @param schemaName the type of beans to match.
     * @param ids ids of the beans.
     * @return A set of beans.
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract Map<BeanId, Bean> list(final String schemaName, Collection<String> ids)
            throws AbortRuntimeException;

    /**
     * Delete a bean accoring to id.
     * <p>
     * Delete operations are not cascading, which means that a bean's references
     * are not deleted along with the bean itself.
     * </p>
     * <p>
     * A bean may be referenced by other beans and the bean manager
     * make sure that referential integrity is not violated when the bean
     * is deleted.
     * </p>

     *
     * @param id delete this bean
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract Bean delete(final BeanId id) throws AbortRuntimeException;

    /**
     * This method deletes multiple instances of the same schema type.
     *
     * @see #delete(BeanId)
     * @param schemaName -
     * @param instanceIds -
     * @exception AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract Collection<Bean> delete(final String schemaName, final Collection<String> instanceIds)
            throws AbortRuntimeException;

    /**
     * Create a new query used for retrieving bean instances using flexible composition
     * of <tt>Criterion</tt> objects, used to narrow number of beans in the result set.
     *
     * A bean query can also be used to paginate through result sets.
     *
     * Fields do NOT need to be indexed.
     *
     * @see {@link BeanQueryBuilder}
     *
     * @param schema schema to query
     * @return Used for composing a query in conjuction with {@link BeanQueryBuilder}.
     */
    public abstract BeanQuery newQuery(Schema schema);
}
