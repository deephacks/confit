package org.deephacks.confit.internal.mapdb;


import org.deephacks.confit.spi.serialization.UniqueId;

public class MapdbUniqueId extends UniqueId {
    private MapDB mapDB;

    public MapdbUniqueId(int width, boolean cache, MapDB mapDB) {
        super(width, cache);
        this.mapDB = mapDB;
    }

    @Override
    protected String getNameFromStorage(long id) {
        return mapDB.getNameFromStorage(id);
    }

    @Override
    protected Long getIdFromStorage(String name) {
        return mapDB.getIdFromStorage(name);
    }
}
