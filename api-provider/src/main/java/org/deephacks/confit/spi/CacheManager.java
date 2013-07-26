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
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;

import java.util.List;

/**
 * CacheManager is responsible for maintain cache entries of configurables.
 * The cache can be queried for single or all configurables that exist
 * for a certain schema.
 *
 * @author Kristoffer Sjogren
 */
public abstract class CacheManager<V> {

    /**
     * Register a know schema with the cache.
     *
     * @param schema a known schema
     */
    public abstract void registerSchema(Schema schema);

    /**
     * Returns the configurable associated with BeanId in this cache.
     *
     * @param id BeanId that identifies the configurable.
     * @return the configurable, or null if absent.
     */
    public abstract V get(BeanId id);

    /**
     * Returns the configurable associated with schemaName in this cache.
     *
     * @param schemaName name that identifies as certain type of configurable.
     * @return A list of beans, or null if absent.
     */
    public abstract List<V> get(String schemaName);

    /**
     * Associates configurable with BeanId in this cache. If the cache previously
     * contained a configurable associated with BeanId, the old configurable is
     * replaced by configurable.
     *
     * @param configurable BeanId that identifies the configurable
     */
    public abstract void put(Bean configurable);

    /**
     * Remove a certain bean from the cache.
     * @param beanId id of bean to remove
     */
    public abstract void remove(BeanId beanId);

    /**
     * Clear the cache for a particular schema
     *
     * @param schemaName schema to clear
     */
    public abstract void clear(String schemaName);

    /**
     * Clear the whole cache for all instances.
     */
    public abstract void clear();

}
