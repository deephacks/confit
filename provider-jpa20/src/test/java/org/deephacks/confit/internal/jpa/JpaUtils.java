package org.deephacks.confit.internal.jpa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.common.io.Closeables;

public class JpaUtils {
    /**
     * JPA providers
     */
    public static final String HIBERNATE = "hibernate";
    public static final String ECLIPSELINK = "eclipselink";
    // TO BE IMPLEMENTED LATER
    public static final String OPENJPA = "openjpa";
    public static final String DATANUCLEUS = "datanucleus";
    public static final String OBJECTDB = "objectdb";

    /**
     * Generates a property file for initalizing the EntityManagerFactory.
     */
    public static abstract class Jpaprovider {
        private static final String PROVIDER = "javax.persistence.provider";
        private static final String URL = "javax.persistence.jdbc.url";
        private static final String DRIVER = "javax.persistence.jdbc.driver";
        private static final String USER = "javax.persistence.jdbc.user";
        private static final String PASSWORD = "javax.persistence.jdbc.password";
        private static final String TRANSACTION_TYPE = "javax.persistence.transactionType";

        protected String provider;
        protected String url;
        protected String driver;
        protected String username;
        protected String password;
        protected String transactionType = "RESOURCE_LOCAL";
        protected HashMap<String, String> providerSpecific = new HashMap<String, String>();

        public Jpaprovider(Database database) {
            url = database.getUrl();
            username = database.getUsername();
            password = database.getPassword();
        }

        public static Jpaprovider create(String jpaProvider, Database database) {
            if (HIBERNATE.equals(jpaProvider)) {
                return new Hibernate(database);
            } else if (ECLIPSELINK.equals(jpaProvider)) {
                return new EclipseLink(database);
            } else {
                throw new UnsupportedOperationException("JPA provider not supported ["
                        + jpaProvider + "]");
            }
        }

        public void write(File file) {
            List<String> contents = new ArrayList<String>();
            contents.add(PROVIDER + "=" + provider);
            contents.add(URL + "=" + url);
            contents.add(DRIVER + "=" + driver);
            contents.add(USER + "=" + username);
            contents.add(PASSWORD + "=" + password);
            contents.add(TRANSACTION_TYPE + "=" + transactionType);
            for (String key : providerSpecific.keySet()) {
                contents.add(key + "=" + providerSpecific.get(key));
            }
            writeFile(contents, file);
        }
    }

    public static class Hibernate extends Jpaprovider {
        private static final String DERBY_DIALECT = "org.hibernate.dialect.DerbyDialect";
        private static final String MYSQL_DIALECT = "org.hibernate.dialect.MySQLDialect";
        private static final String POSTGRESQL_DIALECT = "org.hibernate.dialect.PostgreSQLDialect";

        public Hibernate(Database database) {
            super(database);
            provider = "org.hibernate.ejb.HibernatePersistence";

            if (Database.DERBY.equals(database.getDatabaseProvider())) {
                driver = Database.DERBY_DRIVER;
                providerSpecific.put("hibernate.dialect", DERBY_DIALECT);
            } else if (Database.MYSQL.equals(database.getDatabaseProvider())) {
                driver = Database.MYSQL_DRIVER;
                providerSpecific.put("hibernate.dialect", MYSQL_DIALECT);
            } else if (Database.POSTGRESQL.equals(database.getDatabaseProvider())) {
                driver = Database.POSTGRESQL_DRIVER;
                providerSpecific.put("hibernate.dialect", POSTGRESQL_DIALECT);
            } else {
                throw new UnsupportedOperationException("DB provider not supported ["
                        + database.getDatabaseProvider() + "]");
            }
            providerSpecific.put("hibernate.show_sql", "false");
            providerSpecific.put("hibernate.hbm2ddl.auto", "validate");

        }

    }

    public static class EclipseLink extends Jpaprovider {

        public EclipseLink(Database database) {
            super(database);
            provider = "org.eclipse.persistence.jpa.PersistenceProvider";
            if (Database.DERBY.equals(database.getDatabaseProvider())) {
                driver = Database.DERBY_DRIVER;
            } else if (Database.MYSQL.equals(database.getDatabaseProvider())) {
                driver = Database.MYSQL_DRIVER;
            } else if (Database.POSTGRESQL.equals(database.getDatabaseProvider())) {
                driver = Database.POSTGRESQL_DRIVER;
            } else {
                throw new UnsupportedOperationException("DB provider not supported ["
                        + database.getDatabaseProvider() + "]");

            }
            providerSpecific.put("eclipselink.persistence-context.flush-mode", "COMMIT");
        }
    }

    public static void writeFile(String[] lines, File file) {
        writeFile(Arrays.asList(lines), file);
    }

    public static void writeFile(List<String> lines, File file) {
        try {
            File parent = file.getParentFile();
            if ((parent != null) && !parent.exists()) {
                parent.mkdirs();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unxpected exception when trying to create parent folders for file ["
                            + file.getAbsolutePath() + "].");
        }

        try {
            writeFile(lines, new FileOutputStream(file));
        } catch (IOException e) {
            throw new IllegalArgumentException("File [" + file.getAbsolutePath()
                    + "] cant write to file.", e);
        }
    }

    public static void writeFile(List<String> lines, OutputStream stream) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(stream));
            for (String line : lines) {
                bw.write(line + "\n");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Closeables.closeQuietly(bw);
        }
    }
}