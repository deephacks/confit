package org.deephacks.confit.internal.jpa;

import org.deephacks.confit.test.JUnitUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

import static com.google.common.io.Files.readLines;

/**
 * The machine that runs the test must specify username, password and host
 * for the database.
 *
 * Port configuration is not supported at the moment in order to have
 * simple and unified configuration for list databases.
 */
public class Database {

    /**
     * Properties for test databases kept in confit for configuration.
     */
    public static final String DB_HOST_CONFIT_PROPERTY = "confit.testdb.host";

    private static final String INSTALL_DDL = "install_config_{0}.ddl";
    private static final String UNINSTALL_DDL = "uninstall_config_{0}.ddl";
    /**
     * Database providers. Derby is default.
     */
    public static final String DERBY = "derby";
    public static final String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    public static final String MYSQL = "mysql";
    public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";

    public static final String POSTGRESQL = "postgresql";
    public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

    // TO BE IMPLEMENTED LATER
    public static final String ORACLE = "oracle";
    public static final String DB2 = "db2";

    public static final String SQLITE = "sqlite";
    public static final String HSQL = "hsql";

    private String username;
    private String password;
    private String host;
    private String url;
    private String tablespace;
    private String driver;
    private String dbProvider;
    private List<String> installDdl;
    private List<String> uninstallDdl;

    private Database(String dbProvider, List<String> installDdl, List<String> uninstallDdl) {
        this.dbProvider = dbProvider;
        this.installDdl = installDdl;
        this.uninstallDdl = uninstallDdl;
        this.username = System.getProperty("user.name");
        this.password = System.getProperty("user.name");
        this.host = "localhost";
        this.tablespace = System.getProperty("user.name");
        if (DERBY.equals(dbProvider)) {
            driver = DERBY_DRIVER;
            forName(driver);
            url = "jdbc:derby:memory:" + tablespace + ";create=true";
        } else if (MYSQL.equals(dbProvider)) {
            driver = MYSQL_DRIVER;
            url = "jdbc:mysql://" + host + ":3306/" + tablespace;

        } else if (POSTGRESQL.equals(dbProvider)) {
            driver = POSTGRESQL_DRIVER;
            url = "jdbc:postgresql://" + host + ":5432/" + tablespace;

        } else {
            throw new UnsupportedOperationException("DB provider not supported [" + dbProvider
                    + "]");
        }
    }

    public static Database create(String dbProvider, File dbScriptDir) {
        try {
            List<String> installDdl = readLines(
                    new File(dbScriptDir, MessageFormat.format(INSTALL_DDL, dbProvider)),
                    Charset.defaultCharset());
            List<String> uninstallDdl = readLines(
                    new File(dbScriptDir, MessageFormat.format(UNINSTALL_DDL, dbProvider)),
                    Charset.defaultCharset());
            return new Database(dbProvider, installDdl, uninstallDdl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static Database create(String dbProvider, Class<?> clazz) {
        List<String> installDdl = JUnitUtils.readMetaInfResource(clazz,
                MessageFormat.format(INSTALL_DDL, dbProvider));
        List<String> uninstallDdl = JUnitUtils.readMetaInfResource(clazz,
                MessageFormat.format(UNINSTALL_DDL, dbProvider));
        return new Database(dbProvider, installDdl, uninstallDdl);
    }

    public String getDatabaseProvider() {
        return this.dbProvider;
    }

    public void initalize() {
        try {
            if (dbProvider == DERBY) {
                try {
                    /**
                     * Derby is a special case that, at the moment, does not support support "if exist".
                     * The only option is to ignore SQLException from dropping stuff.
                     */
                    DdlExec.execute(uninstallDdl, url, username, password, true);
                } catch (SQLException e) {
                    // ignore, probably the first DROP TABLE of a non-existing table
                }
                DdlExec.execute(installDdl, url, username, password, false);
            } else {
                DdlExec.execute(uninstallDdl, url, username, password, false);
                DdlExec.execute(installDdl, url, username, password, false);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public String getUrl() {
        return url;
    }

    public String getTablespace() {
        return tablespace;
    }

    public String getDriver() {
        return driver;
    }

    public String getDbProvider() {
        return dbProvider;
    }

    public static Class<?> forName(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}