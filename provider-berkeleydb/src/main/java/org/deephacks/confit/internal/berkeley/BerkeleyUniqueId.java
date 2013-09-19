package org.deephacks.confit.internal.berkeley;

import com.sleepycat.je.Database;
import com.sleepycat.je.Sequence;
import com.sleepycat.persist.EntityStore;
import org.deephacks.confit.spi.serialization.UniqueId;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BerkeleyUniqueId extends UniqueId {
    private EntityStore store;
    private Sequence sequence;
    private HashMap<Long, String> idToName = new HashMap<>();
    private HashMap<String, Long> nameToId = new HashMap<>();
    private AtomicLong counter = new AtomicLong();
    public BerkeleyUniqueId(int width, boolean cache, Database db) {
        super(width, cache);
    }

    @Override
    protected String getNameFromStorage(long id) {
        return idToName.get(id);
    }

    @Override
    protected Long getIdFromStorage(String name) {
        Long id = nameToId.get(name);
        if (id == null) {
            id = counter.incrementAndGet();
            nameToId.put(name, id);
            idToName.put(id, name);
        }
        return id;
    }
}
