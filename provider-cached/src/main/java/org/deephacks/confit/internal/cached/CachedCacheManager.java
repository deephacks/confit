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
package org.deephacks.confit.internal.cached;

import com.google.common.base.Preconditions;
import org.deephacks.cached.Cache;
import org.deephacks.cached.CacheBuilder;
import org.deephacks.cached.buffer.util.internal.chmv8.ConcurrentHashMapV8;
import org.deephacks.confit.internal.cached.proxy.ConfigProxyGenerator;
import org.deephacks.confit.internal.cached.query.ConfigIndex;
import org.deephacks.confit.internal.cached.query.ConfigIndexedCollection;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.query.ConfigQuery;
import org.deephacks.confit.spi.CacheManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.deephacks.confit.internal.cached.proxy.ConfigProxyGenerator.PROXY_CLASS_SUFFIX;

/**
 * Stores proxies of configurable objects into an off-heap cache. Each schema have
 * a separate cache.
 *
 * Configurable objects are serialized to binary form into the off-heap cache
 * with references to other configurable objects, not actual objects.
 *
 * This is a de-duplicating measure to save memory and simplify cache consistency.
 * Avoiding cache serialization of instances that potentially come from different
 * configurable object hierarchies.
 */
public class CachedCacheManager extends CacheManager<Object> {

    /** SchemaName -> Cache */
    private static final ConcurrentHashMapV8<String, Cache<BeanId, Object>> caches = new ConcurrentHashMapV8<>();

    /** generate proxies that are stored in schema-specific caches */
    private static final ConfigProxyGenerator proxyGenerator = new ConfigProxyGenerator();

    /** proxies are serialized into binary ByteBuf form using this serializer */
    private static final DefaultCacheValueSerializer defaultSerializer = new DefaultCacheValueSerializer();

    /** schemaName -> index */
    private static final HashMap<String, ConfigIndex> configIndexes = new HashMap<>();

    /** schemaName -> indexed collection */
    private static final HashMap<String, ConfigIndexedCollection> indexCollections = new HashMap<>();

    @Override
    public void registerSchema(Schema schema) {
        proxyGenerator.put(schema);
        defaultSerializer.put(schema);
        putIndex(schema);
    }

    @Override
    public void removeSchema(Schema schema) {
        String schemaName = schema.getName();
        clear(schemaName);
        indexCollections.remove(schemaName);
        configIndexes.remove(schemaName);
    }


    public Object get(BeanId id) {
        Cache<BeanId, Object> cache = getCache(id.getSchemaName());
        Object proxy = cache.get(id);
        validateCacheObject(proxy);
        return proxy;
    }

    @Override
    public List<Object> get(String schemaName) {
        Cache<BeanId, Object> cache = getCache(schemaName);
        List<Object> objects = new ArrayList<>();
        for(BeanId id : cache.keySet()) {
            objects.add(cache.get(id));
        }
        return objects;
    }

    @Override
    public void put(Bean bean) {
        Schema schema = bean.getSchema();
        Preconditions.checkNotNull(schema, "Missing schema");
        ConfigIndexedCollection col = indexCollections.get(bean.getId().getSchemaName());
        col.add(bean);

        BeanId id = bean.getId();
        Set<Bean> beans = flattenReferences(bean);

        for (Bean b : beans) {
            Object proxy = proxyGenerator.generateConfigProxy(b);
            validateCacheObject(proxy);
            Cache<BeanId, Object> cache = getCache(b.getId().getSchemaName());
            cache.put(id, proxy);
        }
    }

    @Override
    public void putAll(Collection<Bean> configurables) {
        for (Bean bean : configurables) {
            put(bean);
        }
    }

    /**
     * Serializing anything but proxy objects into the cache will break the
     * API and invalidate the idea of de-duplicated cache to save memory and
     * simplify cache consistency
     */
    private void validateCacheObject(Object proxy) {
        if(!proxy.getClass().getName().endsWith(PROXY_CLASS_SUFFIX)) {
            throw new IllegalArgumentException("Do not try to serialize anything " +
                    "but proxied objects into the cache");
        }
    }

    /**
     * Bean may, but not necessarily, have deep hierarchies of references
     * to other beans. Since a cache store beans per schema we must
     * dig out this hierarchy and flatten it out,
     */
    private Set<Bean> flattenReferences(Bean bean) {
        Set<Bean> beans = new HashSet<>();
        for (String referenceName : bean.getReferenceNames()) {
            List<BeanId> ids = bean.getReference(referenceName);
            for (BeanId id : ids) {
                if (id.getBean() == null) {
                    continue;
                }
                beans.addAll(flattenReferences(id.getBean()));
            }
        }
        beans.add(bean);
        return beans;
    }

    @Override
    public void remove(BeanId beanId) {
        Cache<BeanId, Object> cache = getCache(beanId.getSchemaName());
        if(cache != null) {
            cache.remove(beanId);
        }
        ConfigIndexedCollection col = indexCollections.get(beanId.getSchemaName());
        col.remove(beanId);
    }

    @Override
    public void remove(String schemaName, Collection<String> instances) {
        for (String instanceId : instances) {
            remove(BeanId.create(instanceId, schemaName));
        }
    }

    @Override
    public void clear(String schemaName) {
        Cache<BeanId, Object> cache = getCache(schemaName);
        if(cache != null) {
            cache.clear();
        }
    }

    @Override
    public void clear() {
        for (String key : caches.keySet()) {
            caches.get(key).clear();
        }
    }

    @Override
    public ConfigQuery newQuery(Schema schema) {
        ConfigIndexedCollection collection = indexCollections.get(schema.getName());
        if(collection == null) {
            throw Events.CFG101_SCHEMA_NOT_EXIST(schema.getName());
        }
        return new org.deephacks.confit.internal.cached.query.ConfigQuery<>(collection, this);
    }

    private void putIndex(Schema schema) {
        if(configIndexes.get(schema.getName()) != null) {
            return;
        }
        ConfigIndex index = new ConfigIndex(schema);
        configIndexes.put(schema.getName(), index);
        indexCollections.put(schema.getName(), new ConfigIndexedCollection(index));
    }

    private Cache<BeanId, Object> getCache(String schemaName) {
        Cache<BeanId, Object> cache = caches.get(schemaName);
        if(cache == null) {
            synchronized (caches) {
                cache = CacheBuilder.<BeanId, Object>newBuilder()
                        .serializer(defaultSerializer).build();
                caches.put(schemaName, cache);
            }
        }
        return cache;
    }
}
