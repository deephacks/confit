package org.deephacks.confit.internal.mapdb;

import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.spi.serialization.BeanSerialization;
import org.mapdb.DB;
import org.mapdb.TxMaker;
import org.mapdb.TxRollbackException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentNavigableMap;

public class MapDB {
    private static final SchemaManager schemaManager = SchemaManager.lookup();
    public static final String BEANS = "confit.beans";
    public static final String ID_TO_NAME = "confit.id_to_name";
    public static final String NAME_TO_ID = "confit.name_to_id";
    public static final String PROPERTY_COUNTER = "confit.property_counter";
    private final TxMaker txMaker;
    private static final ThreadLocal<DB> tx = new ThreadLocal<>();
    private final BeanSerialization serialization;

    public MapDB(TxMaker txMaker) {
        this.txMaker = txMaker;
        serialization = new BeanSerialization(new MapdbUniqueId(4, true, this));
    }

    private ConcurrentNavigableMap<BeanId, byte[]> getBeanStorage() {
        DB db = getDb();
        return db.getTreeMap(BEANS);
    }

    private ConcurrentNavigableMap<Long, String> getIdToName() {
        DB db = getDb();
        return db.getTreeMap(ID_TO_NAME);
    }

    private ConcurrentNavigableMap<String, Long> getNameToId() {
        DB db = getDb();
        return db.getTreeMap(NAME_TO_ID);
    }

    private Long incrementPropertyCounter() {
        DB db = getDb();
        ConcurrentNavigableMap<String, Long> counters = db.getTreeMap(PROPERTY_COUNTER);
        Long value = counters.get(PROPERTY_COUNTER);
        if (value == null) {
            value = 0L;
        }
        value++;
        counters.put(PROPERTY_COUNTER, value);
        return value;
    }

    public String getNameFromStorage(long id) {
        String name = getIdToName().get(id);
        if (name != null) {
            return name;
        }
        throw new IllegalArgumentException("Id not found " + id);
    }

    public Long getIdFromStorage(String name) {
        Long id = getNameToId().get(name);
        if (id != null) {
            return id;
        }
        id = incrementPropertyCounter();
        getNameToId().put(name, id);
        getIdToName().put(id, name);
        return id;
    }

    public void commit() {
        try {
            DB db = getDb();
            db.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            tx.set(null);
        }
    }

    public void rollback(Throwable e) {
        try {
            if (e instanceof TxRollbackException) {
                e.printStackTrace();
                return;
            }
            DB db = getDb();
            db.rollback();
        } finally {
            tx.set(null);
        }
    }

    private DB getDb() {
        DB db = tx.get();
        if (db == null) {
            db = txMaker.makeTx();
            tx.set(db);
        }
        return db;
    }

    public Bean get(BeanId id) {
        byte[] data = getBeanStorage().get(id);
        if (data == null) {
            return null;
        }
        Schema schema = schemaManager.getSchema(id.getSchemaName());
        return serialization.read(data, id, schema);
    }

    public Collection<Bean> values() {
        ArrayList<Bean> beans = new ArrayList<>();
        for (BeanId id : getBeanStorage().keySet()) {
            Bean bean = toBean(id);
            beans.add(bean);
        }
        return beans;
    }

    private Bean toBean(BeanId id) {
        Schema schema = schemaManager.getSchema(id.getSchemaName());
        byte[] data = getBeanStorage().get(id);
        if (data == null) {
            return null;
        }
        return serialization.read(data, id, schema);
    }

    public Bean remove(BeanId id) {
        Bean bean = get(id);
        getBeanStorage().remove(id);
        return bean;
    }

    public void put(Bean bean) {
        getBeanStorage().put(bean.getId(), serialization.write(bean));
    }

    public void clear() {
        getIdToName().clear();
        getNameToId().clear();
        getBeanStorage().clear();
    }
}
