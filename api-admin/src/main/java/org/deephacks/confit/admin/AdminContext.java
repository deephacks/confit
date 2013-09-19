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
package org.deephacks.confit.admin;

import com.google.common.base.Optional;

import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.admin.query.BeanQueryBuilder;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.ClassLoaderHolder;
import org.deephacks.confit.model.Schema;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Central interface for provisioning configuration to applications.
 * <p>
 * Configuration is read-only from an application runtime perspective and can only be
 * changed using this interface. Configuration changes will be reloaded automatically by
 * applications at runtime.
 * </p>
 * <p>
 * Read operations will return {@link Bean} that will always have schema initialized, including
 * properties and references traversed and fetched eagerly.
 * <p>
 * Provisioning operations are relieved from having {@link Bean} schema initalized, nor
 * must references be set recusivley, {@link BeanId} is enough to indicate references.
 * </p>
 *
 * @author Kristoffer Sjogren
 */
public abstract class AdminContext {
    private static final String CORE_IMPL = "org.deephacks.confit.internal.core.admin.AdminCoreContext";
    private static final String JAXRS_IMPL = "org.deephacks.confit.jaxrs.AdminContextJaxrsProxy";

    private static AdminContext ctx;

    protected AdminContext() {
        // only core should implement this class
        if (!getClass().getName().equals(CORE_IMPL) && !getClass().getName().equals(JAXRS_IMPL)) {
            throw new IllegalArgumentException("Only AdminCoreContext is allowed to"
                    + " implement this interface.");
        }
    }
    /**
     * @return the admin context.
     */
    public static synchronized AdminContext lookup() {
        if (ctx != null) {
            return ctx;
        }
        try {
            ctx = (AdminContext) Class.forName(CORE_IMPL).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ctx;
    }
    /**
     * Get a single bean as identified by the id.
     *
     * @param beanId id of the bean to be fetched.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract Optional<Bean> get(final BeanId beanId) throws AbortRuntimeException;

    /**
     * Get a singleton instance.
     *
     * @param configurable configurable class of instances to be fetched.
     * @param <T> type of configurable
     * @return instance, if found.
     * @throws AbortRuntimeException
     */
    public abstract <T> Optional<T> get(final Class<T> configurable) throws AbortRuntimeException;

    /**
     * Get an instance.
     *
     * @param configurable configurable class of instances to be fetched.
     * @param <T> type of configurable
     * @param instanceId instance id of the instance
     * @return instance, if found.
     * @throws AbortRuntimeException
     */
    public abstract <T> Optional<T> get(final Class<T> configurable, String instanceId) throws AbortRuntimeException;

    /**
     * List all bean instances of particular schema.
     *
     * @param schemaName of beans to be listed.
     * @return beans matching the schema.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract List<Bean> list(final String schemaName) throws AbortRuntimeException;

    /**
     * List all bean instances of particular class.
     *
     * @param configurable class of instances to be listed
     * @param <T> type of class
     * @return list of found instances
     * @throws AbortRuntimeException
     */
    public abstract <T> Collection<T> list(final Class<T> configurable) throws AbortRuntimeException;

    /**
     * List a sepecific set of bean instances of particular schema.
     *
     * @param schemaName of beans to be listed.
     * @param instanceIds the ids that should be listed.
     * @return bean of matching type.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract List<Bean> list(final String schemaName, final Collection<String> instanceIds)
            throws AbortRuntimeException;

    /**
     * Create a bean.
     *
     * @param bean to be created.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void create(final Bean bean) throws AbortRuntimeException;

    /**
     * Convenience method for {@link AdminContext#create(Bean)}.
     *
     * <p>
     * Should generally not be used in production since it generates a bit of
     * overhead in terms of performance.
     * </p>
     *
     * @param object to be created.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     */
    public abstract void createObject(final Object object) throws AbortRuntimeException;

    /**
     * Collection variant of {@link AdminContext#create(Bean)}.
     *
     * @param beans to be created.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void create(final Collection<Bean> beans) throws AbortRuntimeException;

    /**
     * Collection variant of {@link AdminContext#createObject(Object)}.
     *
     * <p>
     * Should generally not be used in production since it generates a bit of
     * overhead in terms of performance.
     * </p>
     *
     * @param objects to be created.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     */
    public abstract void createObjects(final Collection<?> objects) throws AbortRuntimeException;

    /**
     * Overwrite/set existing bean instances with provided data.
     *
     * <p>
     * Already persisted properties associated with the instance
     * will be removed if they are missing from the provided bean instances.
     * </p>
     *
     * @param bean with values to be written.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void set(final Bean bean) throws AbortRuntimeException;

    /**
     * Convenience method for {@link AdminContext#set(Bean)}.
     *
     * <p>
     * Should generally not be used in production since it generates a bit of
     * overhead in terms of performance.
     * </p>
     *
     * @param object with values to be written.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void setObject(final Object object) throws AbortRuntimeException;

    /**
     * Collection variant of {@link AdminContext#set(Bean)}.
     *
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void set(final Collection<Bean> beans) throws AbortRuntimeException;

    /**
     * Collection variant of {@link AdminContext#setObject(Object)}.
     *
     * <p>
     * Should generally not be used in production since it generates a bit of
     * overhead in terms of performance.
     * </p>
     *
     * @param objects with values to be written.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void setObjects(final Collection<?> objects) throws AbortRuntimeException;

    /**
     * Convenience method for {@link AdminContext#merge(Bean)}.
     *
     * <p>
     * Should generally not be used in production since it generates a bit of
     * overhead in terms of performance.
     * </p>
     *
     * @param bean to be merged.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void merge(final Bean bean) throws AbortRuntimeException;

    /**
     * <p>
     * Merges the provided bean properties with an already existing instance.
     * <p>
     * Properties not provided will remain untouched in storage, hence this method can
     * be used to set or delete a single property.
     * </p>
     *
     * @param object to be merged.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void mergeObject(final Object object) throws AbortRuntimeException;

    /**
     * Collection variant of {@link AdminContext#merge(Bean)}.
     *
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void merge(final Collection<Bean> beans) throws AbortRuntimeException;

    /**
     * Collection variant of {@link AdminContext#mergeObject(Object)}.
     *
     * <p>
     * Should generally not be used in production since it generates a bit of
     * overhead in terms of performance.
     * </p>
     *
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void mergeObjects(final Collection<?> objects) throws AbortRuntimeException;

    /**
     * Delete a bean.
     * <p>
     * Beans are only allowed to be deleted if they are not referenced by other beans,
     * in order to enforce referential integrity.
     * </p>
     * <p>
     * Delete operations are not cascading, which means that a bean's references
     * are not deleted along with the bean itself.
     * </p>.
     *
     * @param bean to be deleted
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void delete(final BeanId bean) throws AbortRuntimeException;

    /**
     * Delete a particular instance. The instance must have its Id set.
     *
     * @param instance instance to delete.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void deleteObject(final Object instance) throws AbortRuntimeException;

    /**
     * This is the collection variant of {@link AdminContext#delete(BeanId)}.
     *
     * @param schemaName the name of the schema that covers all instance ids.
     * @param instanceIds instance ids to be deleted.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract void delete(final String schemaName, final Collection<String> instanceIds)
            throws AbortRuntimeException;

    /**
     * Delete a set of instances.
     *
     * @param configurable configurable class
     * @param instanceIds instance ids to removes
     * @throws AbortRuntimeException
     */
    public abstract void deleteObjects(final Class<?> configurable, final Collection<String> instanceIds)
            throws AbortRuntimeException;

    /**
     * Get all schemas available in the system. The keys of the map is the name
     * of the schema. This method can be useful for dynamic schema discovery and
     * display.
     *
     * @return a map of schemas indexed on schema name.
     * @throws AbortRuntimeException is thrown when the system itself cannot
     * recover from a certain event and must therefore abort execution, see
     * {@link org.deephacks.confit.model.Events}.
     */
    public abstract Map<String, Schema> getSchemas();

    /**
     * Get a schema available in the system.
     *
     * @param schemaName name of the schema
     * @return the schema
     */
    public abstract Optional<Schema> getSchema(String schemaName);

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
     * @param schemaName schemaName to query
     * @return Used for composing a query in conjuction with {@link BeanQueryBuilder}.
     */
    public abstract BeanQuery newQuery(String schemaName);
    
    /**
     * Sets the class loader in {@link ClassLoaderHolder}.
     *
     * @param classLoader the new class loader
     */
    public static void setClassLoader(ClassLoader classLoader) {
    	ClassLoaderHolder.setClassLoader(classLoader);
    }

}
