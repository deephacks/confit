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
package org.deephacks.confit.serialization;

import com.google.common.base.Strings;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UniqueIds are used when beans are serialized into binary form. Every persistent bean manager implementation
 * should provide an implementation that is registered using the standard java ServiceLoader mechanism.
 *
 * Instance ids and schema/property names are mapped to a unique id numbers in order to save space and
 * decrease serialization latency. Schema and property names are always cached in memory to speedup lookup.
 * Instance ids can also be cached but this decision is taken by the implementation.
 */
public abstract class UniqueIds {

    protected boolean shouldCacheInstance;

    private final ConcurrentHashMap<String, Long> instanceIdCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, String> instanceNameCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Integer> schemaIdCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, String> schemaNameCache = new ConcurrentHashMap<>();

    protected UniqueIds(boolean shouldCacheInstance) {
        this.shouldCacheInstance = shouldCacheInstance;
    }

    public static UniqueIds lookup() {
        Iterator<UniqueIds> loader = ServiceLoader.load(UniqueIds.class).iterator();
        if (loader.hasNext()) {
            return loader.next();
        }
        throw new IllegalStateException("Could not find a UniqueIds service in META-INF/services.");
    }

    public byte[] getMaxSchemaWidth() {
        return Bytes.fromInt(-1);
    }

    public byte[] getMinSchemaWidth() {
        return Bytes.fromInt(0);
    }

    public byte[] getMaxInstanceWidth() {
        return Bytes.fromLong(-1);
    }

    public byte[] getMinInstanceWidth() {
        return Bytes.fromLong(0);
    }

    public int getSchemaId(final String name) {
        Integer id = schemaIdCache.get(name);
        if (id != null) {
            return id;
        } else {
            id = getSchemaIdFromStorage(name);
            schemaIdCache.put(name, id);
            schemaNameCache.put(id, name);
        }
        return id;
    }

    public String getSchemaName(final int id) {
        String name = schemaNameCache.get(id);
        if (!Strings.isNullOrEmpty(name)) {
            return name;
        } else {
            name = getSchemaNameFromStorage(id);
            if (Strings.isNullOrEmpty(name)) {
                throw new IllegalStateException("Could not map id " + id + " to a name");
            }
            schemaIdCache.put(name, id);
            schemaNameCache.put(id, name);
        }
        return name;
    }

    public long getInstanceId(final String name) {
        Long id = instanceIdCache.get(name);
        if (id != null) {
            return id;
        } else {
            id = getInstanceIdFromStorage(name);
            if (shouldCacheInstance) {
                instanceIdCache.put(name, id);
                instanceNameCache.put(id, name);
            }
        }
        return id;
    }

    public String getInstanceName(final long id) {
        String name = instanceNameCache.get(id);
        if (!Strings.isNullOrEmpty(name)) {
            return name;
        } else {
            name = getInstanceNameFromStorage(id);
            if (Strings.isNullOrEmpty(name)) {
                throw new IllegalStateException("Could not map id " + id + " to a name");
            }
            if (shouldCacheInstance) {
                instanceIdCache.put(name, id);
                instanceNameCache.put(id, name);
            }
        }
        return name;
    }

    protected abstract String getSchemaNameFromStorage(int id);

    protected abstract int getSchemaIdFromStorage(String name);

    protected abstract String getInstanceNameFromStorage(long id);

    protected abstract long getInstanceIdFromStorage(String name);

}
