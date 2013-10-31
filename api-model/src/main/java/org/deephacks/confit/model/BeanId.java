package org.deephacks.confit.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import org.deephacks.confit.serialization.Bytes;
import org.deephacks.confit.serialization.BytesUtils;
import org.deephacks.confit.serialization.UniqueIds;

import java.io.Serializable;
import java.util.Arrays;

import static org.deephacks.confit.model.Events.CFG107_MISSING_ID;

/**
 * Identifies bean instances of a particular schema. Instances are unique per id and schema.
 */
public class BeanId implements Comparable<BeanId> {

    private final String instanceId;
    private final String schemaName;
    private Bean bean;
    private transient Schema schema;
    private boolean isSingleton;

    private BeanId(final String instanceId, final String schemaName) {
        this.instanceId = Preconditions.checkNotNull(instanceId);
        this.schemaName = Preconditions.checkNotNull(schemaName);
        this.isSingleton = false;
    }

    private BeanId(final String instanceId, final String schemaName, final boolean isSingleton) {
        this.instanceId = Preconditions.checkNotNull(instanceId);
        this.schemaName = Preconditions.checkNotNull(schemaName);
        this.isSingleton = isSingleton;
    }

    /**
     * Create a bean identification.
     *
     * @param instanceId of this bean.
     * @param schemaName The bean schema name.
     * @return AdminBeanId
     */
    public static BeanId create(final String instanceId, final String schemaName) {
        if (instanceId == null || "".equals(instanceId)) {
            throw CFG107_MISSING_ID();
        }
        return new BeanId(instanceId, schemaName);
    }

    /**
     * Create a bean identification from its binary representation.
     *
     * @param data binary data
     * @return A BeanId
     */
    public static BeanId create(final byte[] data) {
        return new BinaryBeanId(data).getBeanId();
    }

    /**
     * This method should NOT be used by users.
     *
     * @param schemaName schema of bean.
     * @return a singleton id.
     */
    public static BeanId createSingleton(final String schemaName) {
        return new BeanId(schemaName, schemaName, true);
    }

    /**
     * @return the instance id of the bean.
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * @return the schema name of the bean.
     */
    public String getSchemaName() {
        return schemaName;
    }


    /**
     * Schema will only be present if the Bean was fetched or given from
     * a managed administrative context.
     *
     * @return the schema that belong to the bean instance.
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Set the schema that define content structure and constraints of this
     * the bean instance.
     *
     * @param schema schema
     */
    public void set(final Schema schema) {
        this.schema = schema;
    }

    /**
     * Check for singleton.
     *
     * @return true if singleton.
     */
    public boolean isSingleton() {
        return isSingleton;
    }

    /**
     * Return the bean that is identified by this BeanId. The actual
     * bean will only be available if the BeanId was initalized by the
     * from admin context.
     *
     * @return bean
     */
    public Bean getBean() {
        return bean;
    }

    /**
     * Do not use. Only used by the admin context.
     *
     * @param bean bean
     */
    public void setBean(Bean bean) {
        this.bean = bean;
    }

    /**
     * @return binary representation of a BeanId
     */
    public BinaryBeanId toBinary() {
        return new BinaryBeanId(this);
    }

    /**
     * @return bean id as a byte array
     */
    public byte[] write() {
        return new BinaryBeanId(this).getKey();
    }

    /**
     * Read BeanId from binary representation.
     *
     * @param data byte array
     * @return BeanId
     */
    public static BeanId read(byte[] data) {
        return new BinaryBeanId(data).getBeanId();
    }

    @Override
    public String toString() {
        return getSchemaName() + "@" + getInstanceId();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
        result = prime * result + ((schemaName == null) ? 0 : schemaName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BeanId other = (BeanId) obj;
        if (instanceId == null) {
            if (other.instanceId != null)
                return false;
        } else if (!instanceId.equals(other.instanceId))
            return false;
        if (schemaName == null) {
            if (other.schemaName != null)
                return false;
        } else if (!schemaName.equals(other.schemaName))
            return false;
        return true;
    }

    @Override
    public int compareTo(BeanId that) {
        return ComparisonChain.start()
                .compare(this.schemaName, that.schemaName)
                .compare(this.instanceId, that.instanceId)
                .result();
    }

    /**
     * Binary representation of a BeanId that consist of 4 bytes schema id followed
     * by 8 bytes instance id. By prefixing each id with its schema makes it is possible
     * to store all bean ids in the same sorted collection, close to each other, making
     * querying much more efficient.
     *
     * This also make pagination trivial to implement since the last key always points
     * to the next set of entries.
     */
    public static class BinaryBeanId implements Comparable<BinaryBeanId>, Serializable {
        /** lazy */
        private static UniqueIds ids;
        private byte[] key;

        public BinaryBeanId(BeanId beanId) {
            if (ids == null) {
                ids = UniqueIds.lookup();
            }

            int schemaId = ids.getSchemaId(beanId.getSchemaName());
            long instanceId = ids.getInstanceId(beanId.getInstanceId());
            this.key = toKey(schemaId, instanceId);
        }

        public BinaryBeanId(byte[] key) {
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

        public BeanId getBeanId() {
            String schemaName = ids.getSchemaName(getSchemaId());
            String instanceId = ids.getInstanceName(getInstanceId());
            return BeanId.create(instanceId, schemaName);
        }

        public static BinaryBeanId getMinId(String schemaName) {
            byte[] key = toKey(ids.getSchemaId(schemaName), 0);
            return new BinaryBeanId(key);
        }

        public static BinaryBeanId getMaxId(String schemaName) {
            byte[] key = toKey(ids.getSchemaId(schemaName), -1);
            return new BinaryBeanId(key);
        }

        private byte[] getSchemaBytes() {
            return new byte[] { key[0], key[1], key[2], key[3] };
        }

        private int getSchemaId() {
            return Bytes.getInt(getSchemaBytes());
        }

        private byte[] getInstanceBytes() {
            return new byte[] { key[4], key[5], key[6], key[7], key[8], key[9], key[10], key[11] };
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
}
