package org.deephacks.confit.internal.mapdb;

import org.deephacks.confit.spi.Lookup;
import org.mapdb.DBMaker;
import org.mapdb.TxMaker;

import java.io.File;
import java.io.IOException;

public class MapdbUtil {
    public static File MAPDB_TEMP_FILE;
    public static MapDB mapDB;

    public static void create() {
        try {
            MAPDB_TEMP_FILE = File.createTempFile("confit.mapdb", "tmp");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TxMaker db = DBMaker.newFileDB(MAPDB_TEMP_FILE)
                .makeTxMaker();
        Lookup.get().register(TxMaker.class, db);
        mapDB = new MapDB(db);
        mapDB.clear();
        mapDB.commit();
    }

    public static void delete() {
        mapDB.clear();
        mapDB.commit();
    }
}
