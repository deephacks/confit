package org.deephacks.confit.internal.berkeley;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import org.deephacks.confit.spi.Lookup;

import java.io.File;

public class BerkeleyUtil {
    public static final String BERKELEY_STORE_NAME = "confit.berkeley";
    public static File BERKELEY_TEMP_FILE = new File(System.getProperty("java.io.tmpdir"), BERKELEY_STORE_NAME + System.currentTimeMillis());
    public static Database db;

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
    }

    public static void delete() {
    }
}
