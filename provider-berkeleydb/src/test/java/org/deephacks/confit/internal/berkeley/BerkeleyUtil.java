package org.deephacks.confit.internal.berkeley;

import com.sleepycat.je.Database;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import org.deephacks.confit.spi.Lookup;

import java.io.File;

public class BerkeleyUtil {
    public static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    public static final String BERKELEY_DIR_NAME = "confit.berkeley";
    public static File BERKELEY_ENV_FILE = new File(TMP_DIR, BERKELEY_DIR_NAME);
    public static Database db;
    public static Environment env;

    public static void create() {
        System.out.println("Using storage " + BERKELEY_ENV_FILE);
        BERKELEY_ENV_FILE.mkdirs();
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        Environment env = new Environment(BERKELEY_ENV_FILE, envConfig);
        Lookup.get().register(Environment.class, env);
    }

    public static void delete() {
        env.close();
        BERKELEY_ENV_FILE.renameTo(new File(TMP_DIR, "deleted-" + System.currentTimeMillis() + BERKELEY_ENV_FILE.getName()));
    }
}
