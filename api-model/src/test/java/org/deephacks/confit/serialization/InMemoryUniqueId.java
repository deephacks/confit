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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUniqueId extends UniqueIds {
    private final AtomicLong counter = new AtomicLong(0);
    private ConcurrentHashMap<String, Long> nameToId = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, String> idToName = new ConcurrentHashMap<>();

    public InMemoryUniqueId() {
        super(true);
    }

    @Override
    protected String getSchemaNameFromStorage(int id) {
        String name = idToName.get((long) id);
        if (name == null) {
            throw new IllegalArgumentException("Failed mapping id [" + id + "] to a name.");
        }
        return name;
    }

    @Override
    protected int getSchemaIdFromStorage(String name) {
        Long id = nameToId.get(name);
        if (id != null) {
            return id.intValue();
        }
        synchronized (counter) {
            id = counter.incrementAndGet();
            nameToId.put(name, id);
            idToName.put(id, name);
        }
        return id.intValue();
    }

    @Override
    protected String getInstanceNameFromStorage(long id) {
        String name = idToName.get(id);
        if (name == null) {
            throw new IllegalArgumentException("Failed mapping id [" + id + "] to a name.");
        }
        return name;
    }

    @Override
    protected long getInstanceIdFromStorage(String name) {
        Long id = nameToId.get(name);
        if (id != null) {
            return id;
        }
        synchronized (counter) {
            id = counter.incrementAndGet();
            nameToId.put(name, id);
            idToName.put(id, name);
        }
        return id;
    }
}