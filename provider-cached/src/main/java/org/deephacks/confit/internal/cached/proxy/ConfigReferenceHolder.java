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
package org.deephacks.confit.internal.cached.proxy;


import org.deephacks.confit.internal.cached.CachedCacheManager;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for holding references (not the actual objects) to other
 * configurable classes from a particular instance.
 *
 * The proxy class will be attached with field of this reference holder class.
 */
public class ConfigReferenceHolder {

    /** used by proxies to retrieve real objects from references (instance ids) */
    private static CachedCacheManager cache = new CachedCacheManager();

    /** propertyName -> instance ids */
    private Map<String, List<String>> references = new HashMap<>();

    /** cached instances that have already been generated */
    private ConcurrentHashMap<BeanId, Object> instances = new ConcurrentHashMap<>();

    public ConfigReferenceHolder(Map<String, List<String>> references) {
        this.references = references;
    }

    public ConfigReferenceHolder(Bean bean) {
        for (String propertyName : bean.getReferenceNames()){
            ArrayList<String> instanceIds = new ArrayList<>();
            for (BeanId id : bean.getReference(propertyName)) {
                instanceIds.add(id.getInstanceId());
            }
            references.put(propertyName, instanceIds);
        }
    }

    public Map<String, List<String>> getReferences() {
        return references;
    }

    /**'
     * Called by a proxy to lookup single object reference. The proxy knows
     * the schema name so the hold does not need to bother storing it.
     */
    public Object getObjectReference(String field, String schemaName) {
        List<String> instanceIds = references.get(field);
        if(instanceIds == null || instanceIds.size() == 0) {
            return null;
        }
        String instanceId = instanceIds.get(0);
        if(instanceId == null) {
            return null;
        }
        BeanId id = BeanId.create(instanceId, schemaName);
        Object instance = instances.get(id);
        if(instance != null) {
            return instance;
        }
        instance = cache.get(id);
        instances.put(id, instance);
        return instance;
    }

    /**'
     * Called by a proxy to lookup a list of object references. The proxy knows
     * the schema name so the hold does not need to bother storing it.
     */
    public Collection<Object> getObjectReferenceList(String field, String schemaName) {
        List<String> instanceIds = references.get(field);
        if(instanceIds == null || instanceIds.size() == 0) {
            return null;
        }
        List<Object> objects = new ArrayList<>();
        for (String instanceId : instanceIds) {
            BeanId id = BeanId.create(instanceId, schemaName);
            Object instance = instances.get(id);
            if(instance != null) {
                objects.add(instance);
            } else {
                instance = cache.get(id);
                instances.put(id, instance);
                objects.add(instance);
            }
        }
        return objects;
    }

    /**'
     * Called by a proxy to lookup a map of object references. The proxy knows
     * the schema name so the hold does not need to bother storing it.
     */
    public Map<String, Object> getObjectReferenceMap(String field, String schemaName) {
        List<String> instanceIds = references.get(field);
        if(instanceIds == null || instanceIds.size() == 0) {
            return null;
        }
        Map<String, Object> objects = new HashMap<>();
        for (String instanceId : instanceIds) {
            BeanId id = BeanId.create(instanceId, schemaName);
            Object instance = instances.get(id);
            if(instance != null) {
                objects.put(instanceId, instance);
            } else {
                instance = cache.get(id);
                instances.put(id, instance);
                objects.put(instanceId, instance);
            }
        }
        return objects;
    }

}
