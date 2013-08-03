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

import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Schema;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * SchemaManager is responsible for conversion, validation and storage of configurable schemas.
 *
 */
public abstract class SchemaManager implements Serializable {
    private static final long serialVersionUID = 4888441728053297694L;
    private static Lookup lookup = Lookup.get();

    /**
     * Lookup the most suitable SchemaManager available.
     *
     * @return SchemaManager.
     */
    public static SchemaManager lookup() {
        return lookup.lookup(SchemaManager.class);
    }

    /**
     * List name of all schemas managed by the manager.
     *
     * @return Map of Schemas indexed by name.
     */
    public abstract Map<String, Schema> getSchemas();

    /**
     * Set schema on provided beans.
     *
     * @param beans to set schema on.
     */
    public abstract void setSchema(Collection<Bean> beans);

    /**
     * Return information that describing the schema for a particular type.
     *
     * @param schemaName of schema.
     * @return Schema identified by name.
     */
    public abstract Schema getSchema(final String schemaName);

    /**
     * Return information that describing the schema for a particular type.
     *
     * @param cls Configurable class.
     * @return Schema identified by name.
     */
    public abstract Schema getSchema(final Class<?> cls);

    /**
     * In cluster deployments every server runs same application and
     * will most probably call this method with exact same name and schema.
     * Therefore if a schema already exist, it should be overwritten
     * by the provided schema.
     * <p>
     * This method also binds together interaction between different
     * manager implementations through specific properties.
     * </p>
     *
     * @param classes list of classes to register.
     */
    public abstract void register(final Class<?>... classes);

    /**
     * Removes a class and will not longer be managed.
     * <p>
     * This operation DOES NOT remove beans instances that are
     * associated with the schema that is to be removed.
     * </p>
     *
     * @param cls identifiying the schema.
     */
    public abstract Schema remove(final Class<?> cls);

    /**
     * Validate beans according to their schema, like valid data types.
     *
     * @param beans to validate
     */
    public abstract void validateSchema(Collection<Bean> beans);

    /**
     * Convert a bean into an object as specified by its schema.
     *
     * @param bean to convert
     * @return object
     */
    public abstract Object convertBean(Bean bean);

    /**
     * Convert a collection of beans into an objects as specified by their schema.
     *
     * @param beans to convert
     * @return collection of objects
     */
    public abstract Collection<Object> convertBeans(Collection<Bean> beans);

    /**
     * Convert a collection of objects into their bean representation.
     *
     * @param objects to convert
     * @return collection of beans
     */
    public abstract Collection<Bean> convertObjects(Collection<Object> objects);

    /**
     * Convert an object into its bean representation.
     *
     * @param object to convert
     * @return a bean
     */
    public abstract Bean convertObject(Object object);

}
