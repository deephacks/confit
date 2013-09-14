package org.deephacks.confit.internal.mapdb;


import org.deephacks.confit.spi.serialization.UniqueId;
import org.mapdb.Atomic;
import org.mapdb.DB;

import java.util.concurrent.ConcurrentNavigableMap;

public class MapdbUniqueId extends UniqueId {
    static final String PROPERTY_COUNTER_NAME = "config.propertyIdsCounter";
    static final String NAME_TO_ID = "config.nameToId";
    static final String ID_TO_NAME = "config.idToName";
    private final ConcurrentNavigableMap<Long, String> idToName;
    private final ConcurrentNavigableMap<String, Long> nameToId;
    private final Atomic.Long counter;

    public MapdbUniqueId(int width, boolean cache, DB db) {
        super(width, cache);
        this.counter = db.createAtomicLong(PROPERTY_COUNTER_NAME, 0);
        nameToId = db.getTreeMap(NAME_TO_ID);
        idToName = db.getTreeMap(ID_TO_NAME);
    }

    @Override
    protected String getNameFromStorage(long id) {
        String name = idToName.get(id);
        if (name != null) {
            return name;
        }
        throw new IllegalArgumentException("Id not found " + id);
    }

    @Override
    protected Long getIdFromStorage(String name) {
        Long id = nameToId.get(name);
        if (id != null) {
            return id;
        }
        id = counter.incrementAndGet();
        nameToId.put(name, id);
        idToName.put(id, name);
        return id;
    }
}
