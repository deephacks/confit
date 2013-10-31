package org.deephacks.confit.internal.berkeley;

import com.google.common.base.Optional;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.ForeignKeyDeleteAction;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import com.sleepycat.je.Transaction;
import org.deephacks.confit.internal.berkeley.BerkeleyBeanManager.FastKeyComparator;
import org.deephacks.confit.internal.berkeley.TxDatabase.ForEachKey;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.model.BeanId.BinaryBeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.serialization.UniqueIds;
import org.deephacks.confit.serialization.ValueSerialization;
import org.deephacks.confit.serialization.ValueSerialization.ValueReader;
import org.deephacks.confit.spi.SchemaManager;

import java.util.ArrayList;
import java.util.Collection;

import static org.deephacks.confit.internal.berkeley.BerkeleyBeanManager.BERKELEY_DB_REFERENCES;

public class BerkeleyDb {
    private final TxDatabase db;
    private final SecondaryDatabase secondaryDatabase;
    private static final ThreadLocal<Transaction> TX = new ThreadLocal<>();

    public BerkeleyDb(TxDatabase db) {
        this.db  = db;

        SecondaryConfig secondaryConfig = new SecondaryConfig();
        secondaryConfig.setAllowCreate(true);
        secondaryConfig.setTransactional(true);
        secondaryConfig.setKeyPrefixing(true);
        secondaryConfig.setBtreeComparator(new FastKeyComparator());
        secondaryConfig.setForeignKeyDeleteAction(ForeignKeyDeleteAction.ABORT);

        secondaryConfig.setKeyCreator(new KeyCreator());
        secondaryDatabase = db.getEnv().openSecondaryDatabase(null, BERKELEY_DB_REFERENCES, db.getDb(), secondaryConfig);
    }

    public Environment getEnv() {
        return db.getEnv();
    }

    public boolean put(byte[] key, byte[] value) {
        return db.put(key, value);
    }

    public Optional<byte[]> get(byte[] key) {
        return db.get(key);
    }

    public void list(final String schemaName, final ForEachBean forEach) {
        byte[] min = BinaryBeanId.getMinId(schemaName).getKey();
        byte[] max = BinaryBeanId.getMaxId(schemaName).getKey();
        db.list(min, max, new ForEachKey() {
            @Override
            public boolean match(byte[] key, byte[] data) {
                return forEach.match(BeanId.create(key), data);
            }
        });
     }

    public void commit() {
        db.commit();
    }

    public void abort() {
        db.abort();
    }

    public static interface ForEachBean {
        public boolean match(BeanId id, byte[] data);
    }

    public static class KeyCreator implements SecondaryKeyCreator {
        public static SchemaManager schemaManager = SchemaManager.lookup();
        private final ValueSerialization serialization;
        private static final UniqueIds uniqueIds = UniqueIds.lookup();

        public KeyCreator() {
            this.serialization = new ValueSerialization();
        }

        @Override
        public boolean createSecondaryKey(SecondaryDatabase secondary, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) {
            BeanId beanId = BeanId.read(key.getData());
            Schema schema = schemaManager.getSchema(beanId.getSchemaName());
            ValueReader reader = new ValueReader(data.getData());
            for (String referenceName : schema.getReferenceNames()) {
                long refId = uniqueIds.getSchemaId(referenceName);
                Object value = reader.getValue((int) refId);
                if (value != null) {
                    ArrayList<String> values = new ArrayList<>();
                    if (value instanceof Collection) {
                        values.addAll((Collection<String>) value);
                    } else {
                        values.add(value.toString());
                    }
                    System.out.println(value);
                }

            }
            System.out.println(beanId + " " + schema);
            System.out.println(key + " " + data);
            return false;
        }
    }
}
