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
import org.deephacks.confit.model.Schema;

import java.io.Serializable;
import java.util.Map;

/**
 * SchemaManager is responsible for management and storage of schemas.
 *
 * @author Kristoffer Sjogren
 */
public abstract class SchemaManager implements Serializable {
    private static final long serialVersionUID = 4888441728053297694L;

    public static final String PROPERTY = "config.schemamanager";

    /**
     * List name of all schemas managed by the manager.
     *
     * @return Map of Schemas indexed by name.
     */
    public abstract Map<String, Schema> getSchemas();

    /**
     * Return information that describing the schema for a particular type.
     *
     * @param schemaName of schema.
     * @return Schema identified by name.
     */
    public abstract Optional<Schema> getSchema(final String schemaName);

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
     * @param schema list of schemas to register.
     */
    public abstract void registerSchema(final Schema... schema);

    /**
     * Removes the schema and will not longer be managed by this
     * configuration manager.
     * <p>
     * This operation DOES NOT remove beans instances that are
     * associated with the schema that is to be removed.
     * </p>
     *
     * @param schemaName identifiying the schema.
     */
    public abstract void removeSchema(final String schemaName);

}
