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