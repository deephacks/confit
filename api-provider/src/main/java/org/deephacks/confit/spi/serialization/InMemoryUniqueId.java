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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUniqueId extends UniqueId {
        private final AtomicLong counter = new AtomicLong(0);
        private ConcurrentHashMap<String, Long> nameToId = new ConcurrentHashMap<>();
        private ConcurrentHashMap<Long, String> idToName = new ConcurrentHashMap<>();

        public InMemoryUniqueId() {
            super(4, true);
        }

        @Override
        protected String getNameFromStorage(long id) {
            String name = idToName.get(id);
            if (name == null) {
                throw new IllegalArgumentException("Failed mapping id [" + id + "] to a name.");
            }
            return name;
        }

        @Override
        protected Long getIdFromStorage(String name) {
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