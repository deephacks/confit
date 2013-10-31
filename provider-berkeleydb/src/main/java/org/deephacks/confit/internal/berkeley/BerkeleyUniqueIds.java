package org.deephacks.confit.internal.berkeley;

import com.google.common.base.Optional;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import org.deephacks.confit.serialization.Bytes;
import org.deephacks.confit.serialization.UniqueIds;
import org.deephacks.confit.spi.Lookup;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BerkeleyUniqueIds extends UniqueIds {
    private static final String SCHEMAS = "schemas";
    private static final String INSTANCES = "instances";
    private final TxDatabase instances;
    private final TxDatabase schemas;
    private final byte NAME_PREFIX = 0;
    private final byte ID_PREFIX = 1;

    public BerkeleyUniqueIds() {
        super(true);
        Environment env = Lookup.get().lookup(Environment.class);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(false);
        this.schemas = new TxDatabase(env.openDatabase(null, SCHEMAS, dbConfig));
        this.instances = new TxDatabase(env.openDatabase(null, INSTANCES, dbConfig));
    }

    @Override
    protected String getSchemaNameFromStorage(int id) {
        Optional<byte[]> optionalName = schemas.get(getIdKey(id));
        if (optionalName.isPresent()) {
            return new String(optionalName.get(), UTF_8);
        }
        throw new IllegalArgumentException("Id does not exist in storage " + id);
    }

    @Override
    protected int getSchemaIdFromStorage(String name) {
        byte[] nameBytes = name.getBytes(UTF_8);
        Optional <byte[]> optionalId = schemas.get(getNameKey(nameBytes));
        if (optionalId.isPresent()) {
            return Bytes.getInt(optionalId.get());
        }
        byte[] key = SCHEMAS.getBytes(UTF_8);
        long id = schemas.increment(key);
        byte[] nameKey = getNameKey(nameBytes);
        byte[] idKey = getIdKey(id);
        schemas.put(nameKey, Bytes.fromLong(id));
        schemas.put(idKey, nameBytes);
        return (int) id;
    }

    @Override
    protected String getInstanceNameFromStorage(long id) {
        Optional<byte[]> optionalName = instances.get(getIdKey(id));
        if (optionalName.isPresent()) {
            return new String(optionalName.get(), UTF_8);
        }
        throw new IllegalArgumentException("Id does not exist in storage " + id);
    }

    @Override
    protected long getInstanceIdFromStorage(String name) {
        byte[] nameBytes = name.getBytes(UTF_8);
        Optional <byte[]> optionalId = instances.get(getNameKey(nameBytes));
        if (optionalId.isPresent()) {
            return Bytes.getLong(optionalId.get());
        }
        byte[] key = INSTANCES.getBytes(UTF_8);
        long id = instances.increment(key);
        byte[] nameKey = getNameKey(nameBytes);
        byte[] idKey = getIdKey(id);
        instances.put(nameKey, Bytes.fromLong(id));
        instances.put(idKey, nameBytes);
        return id;
    }
    private byte[] getIdKey(long id) {
        byte[] k = Bytes.fromLong(id);
        return new byte[] { ID_PREFIX, k[0], k[1], k[2], k[3], k[4], k[5], k[6], k[7]};
    }

    private byte[] getNameKey(byte[] name) {
        byte[] key = new byte[name.length + 1];
        key[0] = NAME_PREFIX;
        System.arraycopy(name, 0, key, 1, name.length);
        return key;
    }

}
