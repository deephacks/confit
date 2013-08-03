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
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.query.ConfigQuery;

import java.util.Collection;
import java.util.List;

/**
 * CacheManager is responsible for maintain cache entries of configurables.
 * The cache can be queried for single or all configurables that exist
 * for a certain schema.
 *
 * @author Kristoffer Sjogren
 */
public abstract class CacheManager<V> {

    private static Lookup lookup = Lookup.get();
    private static PropertyManager propertyManager = PropertyManager.lookup();
    private static final String CONFIG_QUERY_FEATURE_ENABLED_PROPERTY = "confit.query.enabled";
    private static boolean configQueryFeatureEnabled = false;

    public static Optional<CacheManager> lookup() {
        Optional<String> optional = propertyManager.get(CONFIG_QUERY_FEATURE_ENABLED_PROPERTY);
        if(optional.isPresent()){
            configQueryFeatureEnabled = true;
        }
        if(!configQueryFeatureEnabled) {
            return Optional.absent();
        }
        CacheManager manager = lookup.lookup(CacheManager.class);
        if (manager != null) {
            return Optional.of(manager);
        } else {
            return Optional.absent();
        }
    }

    /**
     * Register a know schema with the cache.
     *
     * @param schema a known schema
     */
    public abstract void registerSchema(Schema schema);

    /**
     * Remove schema from the cache.
     *
     * @param schema a known schema
     */
    public abstract void removeSchema(Schema schema);

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
     * Associates configurables with BeanIds in this cache. If the cache previously
     * contained a configurable associated with BeanId, the old configurable is
     * replaced by configurable.
     *
     * @param configurables BeanId that identifies the configurable
     */
    public abstract void putAll(Collection<Bean> configurables);

    /**
     * Remove a certain bean from the cache.
     *
     * @param beanId id of bean to remove
     */
    public abstract void remove(BeanId beanId);

    /**
     * Remove a collection of beans from the cache.
     *
     * @param schemaName schemaName
     * @param instances instanceIds
     */
    public abstract void remove(String schemaName, Collection<String> instances);

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

    /**
     * Create a cache query.
     *
     * @param schema schema to query
     * @return a query object
     */
    public abstract ConfigQuery newQuery(Schema schema);



}
