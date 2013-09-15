/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.confit.internal.mapdb;

import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.spi.serialization.BeanSerialization;
import org.deephacks.confit.spi.serialization.BinaryBeanId;
import org.deephacks.confit.spi.serialization.UniqueIds;
import org.mapdb.DB;
import org.mapdb.TxMaker;
import org.mapdb.TxRollbackException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
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
    private final UniqueIds uniqueIds;

    public MapDB(TxMaker txMaker) {
        this.txMaker = txMaker;
        MapdbUniqueId instanceIds = new MapdbUniqueId(8, true, this);
        MapdbUniqueId propertyIds = new MapdbUniqueId(4, true, this);
        MapdbUniqueId schemaIds = new MapdbUniqueId(4, true, this);
        uniqueIds = new UniqueIds(instanceIds, schemaIds, propertyIds);
        serialization = new BeanSerialization(propertyIds);
    }

    private ConcurrentNavigableMap<BinaryBeanId, byte[]> getBeanStorage() {
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
        byte[] data = getBeanStorage().get(new BinaryBeanId(id, uniqueIds));
        if (data == null) {
            return null;
        }
        Schema schema = schemaManager.getSchema(id.getSchemaName());
        return serialization.read(data, id, schema);
    }

    public Collection<Bean> values() {
        ArrayList<Bean> beans = new ArrayList<>();
        for (BinaryBeanId id : getBeanStorage().keySet()) {
            Bean bean = toBean(id.getBeanId(uniqueIds));
            beans.add(bean);
        }
        return beans;
    }

    public Collection<Bean> list(String schemaName) {
        ArrayList<Bean> beans = new ArrayList<>();
        BinaryBeanId min = BinaryBeanId.getMinId(schemaName, uniqueIds);
        BinaryBeanId max = BinaryBeanId.getMaxId(schemaName, uniqueIds);
        final ConcurrentNavigableMap<BinaryBeanId, byte[]> schemaInstances
                = getBeanStorage().subMap(min, true, max, true);
        for (BinaryBeanId id : schemaInstances.keySet()) {
            Bean bean = toBean(id.getBeanId(uniqueIds));
            beans.add(bean);
        }
        return beans;
    }

    public LinkedHashMap<BeanId, byte[]> listBinary(String schemaName) {
        LinkedHashMap<BeanId, byte[]> beans = new LinkedHashMap<>();
        BinaryBeanId min = BinaryBeanId.getMinId(schemaName, uniqueIds);
        BinaryBeanId max = BinaryBeanId.getMaxId(schemaName, uniqueIds);

        final ConcurrentNavigableMap<BinaryBeanId, byte[]> schemaInstances
                = getBeanStorage().subMap(min, true, max, true);
        for (BinaryBeanId id : schemaInstances.keySet()) {
            beans.put(id.getBeanId(uniqueIds), schemaInstances.get(id));
        }
        return beans;
    }


    private Bean toBean(BeanId id) {
        Schema schema = schemaManager.getSchema(id.getSchemaName());
        byte[] data = getBeanStorage().get(new BinaryBeanId(id, uniqueIds));
        if (data == null) {
            return null;
        }
        return serialization.read(data, id, schema);
    }

    public Bean remove(BeanId id) {
        Bean bean = get(id);
        getBeanStorage().remove(new BinaryBeanId(id, uniqueIds));
        return bean;
    }

    public void put(Bean bean) {
        getBeanStorage().put(new BinaryBeanId(bean.getId(), uniqueIds), serialization.write(bean));
    }

    public void clear() {
        getBeanStorage().clear();
    }

    public UniqueIds getUniqueIds() {
        return uniqueIds;
    }

}
