package org.deephacks.confit.internal.berkeley;

import com.google.common.base.Optional;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;
import com.sleepycat.je.Transaction;
import org.deephacks.confit.serialization.BytesUtils;

public class TxDatabase {
    private final Database db;
    private final Environment env;
    private final SequenceConfig conf;
    private static final ThreadLocal<Transaction> TX = new ThreadLocal<>();

    public TxDatabase(Database db) {
        this.db = db;
        this.env = db.getEnvironment();
        this.conf = new SequenceConfig();
        conf.setAllowCreate(true);

    }

    public Database getDb() {
        return db;
    }

    public Environment getEnv() {
        return env;
    }

    public Cursor openCursor() {
        return db.openCursor(getTx(), null);
    }

    public boolean put(byte[] key, byte[] value) {
        Transaction tx = getTx();
        DatabaseEntry dbKey = new DatabaseEntry(key);
        DatabaseEntry dbValue = new DatabaseEntry(value);
        if (OperationStatus.KEYEXIST == db.putNoOverwrite(tx, dbKey, dbValue)) {
            return false;
        }
        return true;
    }

    public Optional<byte[]> get(byte[] key) {
        Transaction tx = getTx();
        DatabaseEntry dbKey = new DatabaseEntry(key);
        DatabaseEntry dbValue = new DatabaseEntry();
        if (OperationStatus.NOTFOUND == db.get(tx, dbKey, dbValue, LockMode.READ_COMMITTED)) {
            return Optional.absent();
        }
        return Optional.fromNullable(dbValue.getData());
    }

    public void list(byte[] min, byte[] max, ForEachKey forEach) {
        DatabaseEntry dbKey = new DatabaseEntry(min);
        DatabaseEntry dbValue = new DatabaseEntry();
        try (Cursor cursor = openCursor()) {
            if (cursor.getSearchKeyRange(dbKey, dbValue, LockMode.DEFAULT) != OperationStatus.SUCCESS) {
                return;
            }
            byte[] key  = dbKey.getData();
            if (BytesUtils.compareTo(key, 0, key.length, min, 0, min.length) >= 0) {
                forEach.match(key, dbValue.getData());
            }
            while (cursor.getNextNoDup(dbKey, dbValue, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                key  = dbKey.getData();
                if (BytesUtils.compareTo(key, 0, key.length, max, 0, max.length) > 0) {
                    return;
                }
                if (!forEach.match(key, dbValue.getData())) {
                    return;
                }
            }
        }
    }

    public Transaction getTx() {
        Transaction tx = TX.get();
        if (tx == null) {
            tx = env.beginTransaction(null, null);
            TX.set(tx);
        }
        return tx;
    }

    public void commit() {
        Transaction tx = TX.get();
        if (tx == null) {
            return;
        }
        TX.set(null);
        tx.commit();
    }

    public void abort() {
        Transaction tx = TX.get();
        if (tx == null) {
            return;
        }
        TX.set(null);
        tx.abort();
    }

    public long increment(byte[] key) {
        Transaction tx = getTx();
        Sequence sequence = db.openSequence(tx, new DatabaseEntry(key), conf);
        return sequence.get(tx, 1);
    }

    public static interface ForEachKey {
        public boolean match(byte[] key, byte[] data);
    }

}
