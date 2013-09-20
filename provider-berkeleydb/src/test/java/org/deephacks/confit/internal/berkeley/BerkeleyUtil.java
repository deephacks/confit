package org.deephacks.confit.internal.berkeley;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import org.deephacks.confit.spi.Lookup;

import java.io.File;

public class BerkeleyUtil {
    public static final String BERKELEY_STORE_NAME = "confit.berkeley";
    public static File BERKELEY_TEMP_FILE = new File(System.getProperty("java.io.tmpdir"), BERKELEY_STORE_NAME);
    public static Database db;
    public static EntityStore store;

    public static void create() {
        System.out.println("Using storage " + BERKELEY_TEMP_FILE);
        BERKELEY_TEMP_FILE.mkdirs();
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        Environment env = new Environment(BERKELEY_TEMP_FILE, envConfig);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(true);
        db = env.openDatabase(null, BERKELEY_STORE_NAME, dbConfig);
        Lookup.get().register(Database.class, db);
        StoreConfig conf = new StoreConfig();
        conf.setAllowCreate(true);
        conf.setTransactional(true);
        store = new EntityStore(db.getEnvironment(), BERKELEY_STORE_NAME, conf);

    }

    public static void delete() {
    }

    public static void main(String[] args) {
        create();
        final PrimaryIndex<String,TestData> primaryIndex = store.getPrimaryIndex(String.class, TestData.class);
        //primaryIndex.putNoOverwrite(new TestData("1", "1"));
        System.out.println(primaryIndex.get("1"));
    }
    @Entity
    public static class TestData {
        @PrimaryKey
        private String id;

        private String v;
        private int i;

        private TestData() {

        }
        public TestData(String id, String v) {
            this.id = id;
            this.v = v;
        }

        @Override
        public String toString() {
            return "TestData{" +
                    "id='" + id + '\'' +
                    ", v='" + v + '\'' +
                    '}';
        }
    }
}
