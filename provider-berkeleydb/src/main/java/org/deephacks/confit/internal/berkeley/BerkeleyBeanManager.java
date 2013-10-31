package org.deephacks.confit.internal.berkeley;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.internal.berkeley.BerkeleyDb.ForEachBean;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.serialization.BytesUtils;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.Lookup;
import org.deephacks.confit.spi.SchemaManager;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public class BerkeleyBeanManager extends BeanManager {
    public static final String BERKELEY_DB_NAME = "confit.berkeley";
    public static final String BERKELEY_DB_REFERENCES = "confit.berkeleyRefs";
    private static final SchemaManager schemaManager = SchemaManager.lookup();
    private final BerkeleyDb db;

    public BerkeleyBeanManager() {
        Environment env = Lookup.get().lookup(Environment.class);
        Preconditions.checkNotNull(env);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(false);
        dbConfig.setBtreeComparator(new FastKeyComparator());
        // all keys have schema name prefix
        dbConfig.setKeyPrefixing(true);

        Database database = env.openDatabase(null, BERKELEY_DB_NAME, dbConfig);
        db = new BerkeleyDb(new TxDatabase(database));
    }

    @Override
    public void create(Bean bean) throws AbortRuntimeException {
        try {
            byte[] key = bean.getId().write();
            byte[] value = bean.write();
            if (!db.put(key, value)) {
                throw Events.CFG303_BEAN_ALREADY_EXIST(bean.getId());
            }
            db.commit();
        } catch (AbortRuntimeException e) {
            db.abort();
            throw e;
        } catch (Exception e) {
            db.abort();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(Collection <Bean> beans) throws AbortRuntimeException {
        for (Bean bean : beans) {
            byte[] key = bean.getId().write();
            byte[] value = bean.write();
            if (!db.put(key, value)) {
                throw Events.CFG303_BEAN_ALREADY_EXIST(bean.getId());
            }
        }
        db.commit();
    }

    @Override
    public void createSingleton(BeanId singleton) {
        try {
            byte[] key = singleton.write();
            db.put(key, new byte[0]);
            db.commit();
        } catch (AbortRuntimeException e) {
            db.abort();
            throw e;
        } catch (Exception e) {
            db.abort();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(Bean bean) {
    }

    @Override
    public void set(Collection<Bean> bean) throws AbortRuntimeException {
    }

    @Override
    public void merge(Bean bean) throws AbortRuntimeException {
    }

    @Override
    public void merge(Collection<Bean> bean) throws AbortRuntimeException {
    }

    @Override
    public Optional<Bean> getEager(BeanId id) throws AbortRuntimeException {
        byte[] key = id.write();
        Optional<byte[]> optionalValue = db.get(key);
        if (!optionalValue.isPresent()) {
            return Optional.absent();
        }
        Bean bean = Bean.read(id, optionalValue.get());
        return Optional.fromNullable(bean);
    }

    @Override
    public Optional<Bean> getLazy(BeanId id) throws AbortRuntimeException {
        return null;
    }

    @Override
    public Optional<Bean> getSingleton(String schemaName) throws IllegalArgumentException {
        return null;
    }

    @Override
    public Map<BeanId, Bean> list(final String schemaName) throws AbortRuntimeException {
        final Map<BeanId, Bean> beans = new LinkedHashMap<>();
        db.list(schemaName, new ForEachBean() {
            @Override
            public boolean match(BeanId id, byte[] data) {
                Bean bean = Bean.read(id, data);
                beans.put(id, bean);
                return true;
            }
        });
        return beans;
    }

    @Override
    public Map<BeanId, Bean> list(final String schemaName, final Collection<String> ids) throws AbortRuntimeException {
        final Map<BeanId, Bean> beans = new LinkedHashMap<>();
        db.list(schemaName, new ForEachBean() {
            @Override
            public boolean match(BeanId id, byte[] data) {
                if (ids.contains(id.getInstanceId())) {
                    Bean bean = Bean.read(id, data);
                    beans.put(id, bean);
                }
                return true;
            }
        });
        return beans;

    }

    @Override
    public Bean delete(BeanId id) throws AbortRuntimeException {
        return null;
    }

    @Override
    public Collection<Bean> delete(String schemaName, Collection<String> instanceIds) throws AbortRuntimeException {
        return null;
    }

    @Override
    public BeanQuery newQuery(Schema schema) {
        return null;
    }

    static class FastKeyComparator implements Comparator<byte[]>, Serializable {

        @Override
        public int compare(byte[] o1, byte[] o2) {
            return BytesUtils.compareTo(o1, 0, o1.length, o2, 0, o2.length);
        }
    }
}
