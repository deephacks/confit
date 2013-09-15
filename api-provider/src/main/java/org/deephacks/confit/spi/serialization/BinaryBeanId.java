package org.deephacks.confit.spi.serialization;


import org.deephacks.confit.model.Bean.BeanId;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Binary representation of a BeanId that consist of 4 bytes schema id followed
 * by 8 bytes instance id. By prefixing each id with its schema makes it is possible
 * to store all bean ids in the same sorted collection, close to each other, making
 * querying much more efficient.
 *
 * This also make pagination trivial to implement since the last key always points
 * to the next set of entries.
 */
public class BinaryBeanId implements Comparable<BinaryBeanId>, Serializable {

    private byte[] key;

    public BinaryBeanId(BeanId beanId, UniqueIds uniqueIds) {
        int schemaId = (int) uniqueIds.getSchemaIds().getId(beanId.getSchemaName());
        long instanceId = uniqueIds.getSchemaIds().getId(beanId.getInstanceId());
        this.key = toKey(schemaId, instanceId);
    }

    private BinaryBeanId(byte[] key) {
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }

    private static byte[] toKey(int schemaId, long instanceId) {
        byte[] s = Bytes.fromInt(schemaId);
        byte[] i = Bytes.fromLong(instanceId);
        return new byte[] {s[0], s[1], s[2], s[3], i[0], i[1], i[2], i[3], i[4], i[5], i[6], i[7]};
    }

    public BeanId getBeanId(UniqueIds uniqueIds) {
        String schemaName = uniqueIds.getSchemaIds().getName(getSchemaId());
        String instanceId = uniqueIds.getInstanceIds().getName(getInstanceId());
        return BeanId.create(instanceId, schemaName);
    }

    public static BinaryBeanId getMinId(String schemaName, UniqueIds uniqueIds) {
        byte[] key = toKey((int) uniqueIds.getSchemaIds().getId(schemaName), 0);
        return new BinaryBeanId(key);
    }

    public static BinaryBeanId getMaxId(String schemaName, UniqueIds uniqueIds) {
        byte[] key = toKey((int) uniqueIds.getSchemaIds().getId(schemaName), -1);
        return new BinaryBeanId(key);
    }

    private byte[] getSchemaBytes() {
        return new byte[] {key[0], key[1], key[2], key[3] };
    }

    private int getSchemaId() {
        return Bytes.getInt(getSchemaBytes());
    }

    private byte[] getInstanceBytes() {
        return new byte[] {key[4], key[5], key[6], key[7], key[8], key[9], key[10], key[11] };
    }

    private long getInstanceId() {
        return Bytes.getLong(getInstanceBytes());
    }

    @Override
    public int compareTo(BinaryBeanId o) {
        return BytesUtils.compareTo(key, 0, key.length, o.getKey(), 0, o.getKey().length);
    }

    @Override
    public String toString() {
        return Arrays.toString(key);
    }
}
