package org.deephacks.confit.internal.berkeley;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.sleepycat.je.Database;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.Lookup;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.spi.serialization.BeanSerialization;
import org.deephacks.confit.spi.serialization.UniqueIds;

import java.util.Collection;
import java.util.Map;

import static org.deephacks.confit.model.Events.CFG303_BEAN_ALREADY_EXIST;

public class BerkeleyBeanManager extends BeanManager {
    public static final String BERKELEY_STORE_NAME = "confit.berkeley";
    private final Database db;
    private final EntityStore store;
    private final PrimaryIndex<BBeanId, BBean> index;
    private final BeanSerialization serialization;
    private final UniqueIds uniqueIds;
    private static final SchemaManager schemaManager = SchemaManager.lookup();

    public BerkeleyBeanManager() {
        db = Lookup.get().lookup(Database.class);
        Preconditions.checkNotNull(db);
        StoreConfig conf = new StoreConfig();
        conf.setAllowCreate(true);
        store = new EntityStore(db.getEnvironment(), BERKELEY_STORE_NAME, conf);
        index = store.getPrimaryIndex(BBeanId.class, BBean.class);

        BerkeleyUniqueId instanceIds = new BerkeleyUniqueId(8, true, db);
        BerkeleyUniqueId propertyIds = new BerkeleyUniqueId(4, true, db);
        BerkeleyUniqueId schemaIds = new BerkeleyUniqueId(4, true, db);
        uniqueIds = new UniqueIds(instanceIds, schemaIds, propertyIds);
        serialization = new BeanSerialization(new BerkeleyUniqueId(4, true, db));
    }

    @Override
    public void create(Bean bean) throws AbortRuntimeException {
        BBeanId id = new BBeanId(bean.getId(), uniqueIds);
        BBean bbean = new BBean(id, bean, serialization);
        if (!index.putNoOverwrite(bbean)) {
            throw CFG303_BEAN_ALREADY_EXIST(bean.getId());
        }
    }

    @Override
    public void create(Collection<Bean> beans) throws AbortRuntimeException {
    }

    @Override
    public void createSingleton(BeanId singleton) {
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
        BBean bean = index.get(new BBeanId(id, uniqueIds));
        return Optional.fromNullable(serialization.read(bean.getData(), id, schemaManager.getSchema(id.getSchemaName())));
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
    public Map<BeanId, Bean> list(String schemaName) throws AbortRuntimeException {
        return null;
    }

    @Override
    public Map<BeanId, Bean> list(String schemaName, Collection<String> ids) throws AbortRuntimeException {
        return null;
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
}
