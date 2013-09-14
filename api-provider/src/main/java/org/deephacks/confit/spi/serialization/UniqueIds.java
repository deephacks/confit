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
package org.deephacks.confit.spi.serialization;

/**
 * Holder class for lookup of instance ids, schema and property names.
 */
public class UniqueIds {
    /** maps bean instance id to unique number */
    private UniqueId instanceIds;

    /** maps bean schema name to unique number */
    private UniqueId schemaIds;

    /** maps bean property name to unique number */
    private UniqueId propertyIds;

    public UniqueIds(UniqueId instanceIds, UniqueId schemaIds, UniqueId propertyIds) {
        this.instanceIds = instanceIds;
        this.schemaIds = schemaIds;
        this.propertyIds = propertyIds;
    }

    /**
     * @return unique id for schema names.
     */
    public UniqueId getSchemaIds() {
        return schemaIds;
    }
    /**
     * @return unique id for bean instance ids.
     */
    public UniqueId getInstanceIds() {
        return instanceIds;
    }

    /**
     * @return unique id for bean property names.
     */
    public UniqueId getPropertyIds() {
        return propertyIds;
    }
}
