package org.deephacks.confit.internal.mapdb;

import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.spi.Lookup;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.concurrent.ConcurrentNavigableMap;

public class MapdbUtil {
    public static final File MAPDB_TEMP_FILE = new File(System.getProperty("java.io.tmpdir"), "config.mapdb");
    private static ConcurrentNavigableMap<BeanId, String> storage;
    public static DB db;


    public static void create() {
        db = DBMaker.newFileDB(MAPDB_TEMP_FILE)
                .asyncWriteDisable()
                .cacheDisable()
                .checksumEnable()
                .make();
        Lookup.get().register(DB.class, db);
        storage = db.getTreeMap(MapdbBeanManager.TREEMAP_NAME);
        storage.clear();
    }

    public static void delete() {
        storage.clear();
    }
}
