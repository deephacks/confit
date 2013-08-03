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
package org.deephacks.confit.internal.hbase;

import com.google.common.base.Strings;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RowMutations;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulate hbase byte manipulations of beans.
 *
 * Each unique rowkey follows the following format:
 * - 2 bytes schema short-id, sid
 * - 4 byte instance short-id, iid
 *
 * Short ids are given by the UniqueIds class.
 *
 * Each row contains the following columns:
 * - PROP_COLUMN_FAMILY, bean properties
 *    Qualifier: PROP_COLUMN_FAMILY.
 *    Value: A kryo serialized byte array consisting of list properties.
 *
 * - REF_COLUMN_FAMILY, bean references
 *    Qualifier: sid+pid of the bean
 *    Value: a compressed byte array of iids, each 4 bytes long.
 *           In order to reduce the amount of property data stored, we convert
 *           the properties into a string matrix. The first element is the
 *           property name followed by its values.
 *
 * - PRED_COLUMN_FAMILY, predecessor refering to this bean
 *    Qualifier: sid of the predecessor
 *    Value: a compressed byte array of iids, each 4 bytes long.
 *
 * The reason for splitting qualifier and value for references is effeciency,
 * save memory and processing time.
 *
 * Having a lot of references should not be a problem unless qualifier
 * reaches above 10MB, around 10000000 bytes / 4 bytes =~ 2 500 000 references.
 */
public final class HBeanRow {
    /** Hbase table that store beans. */
    public static final byte[] BEAN_TABLE = "bean".getBytes();
    /** number of bytes allocated for schema name and property ids */
    public static final int SID_WIDTH = 2;
    /** table storing short ids for schema */
    public static final byte[] SID_TABLE = "sid".getBytes();
    /** number of bytes allocated for instance id */
    public static final int IID_WIDTH = 4;
    /** table storing short ids for schema */
    public static final byte[] IID_TABLE = "iid".getBytes();
    /** number of bytes allocated for property ids */
    public static final int PID_WIDTH = 2;
    /** table storing short ids for properties */
    public static final byte[] PID_TABLE = "pid".getBytes();
    /** Properties for each bean is serialized into one column familiy. */
    public static final byte[] PROP_COLUMN_FAMILY = "p".getBytes();
    /** References for each bean are stored as compacted iid's, qualified by sid+pid */
    public static final byte[] REF_COLUMN_FAMILY = "r".getBytes();
    /** Predecessor beans are stored as compacted iid's, qualified by sid */
    public static final byte[] PRED_COLUMN_FAMILY = "pr".getBytes();
    /** Dummy column that enables empty rows/beans. */
    public static final byte[] SINGLETON_COLUMN_FAMILY = "s".getBytes();
    /** Dummy column that enables empty rows/beans. */
    public static final byte[] DUMMY_COLUMN_FAMILY = "d".getBytes();
    /** long value representation of the rowkey of a bean */
    private final long id;
    /** lookup name to id and vice verse */
    private final UniqueIds uids;
    /** KeyValue for storing list properties of a bean. */
    private HBeanProperties properties;
    /** Each KeyValue stores references per property name. */
    private HBeanReferences references;

    private HBeanPredecessors predecessors = new HBeanPredecessors();
    private boolean singleton;

    private HBean hBean;

    public HBeanRow(KeyValue[] kvs, UniqueIds uids) {
        references = new HBeanReferences(uids);
        properties = new HBeanProperties(uids);
        this.id = getId(kvs.length == 0 ? null : kvs[0].getRow());
        this.uids = uids;
        for (int i = 0; i < kvs.length; i++) {
            if (HBeanProperties.isProperty(kvs[i])) {
                properties = new HBeanProperties(kvs[i], uids);
            } else if (HBeanReferences.isReference(kvs[i])) {
                references.add(kvs[i]);
            } else if (HBeanPredecessors.isPredecessor(kvs[i])) {
                if (predecessors == null) {
                    predecessors = new HBeanPredecessors(kvs[i]);
                } else {
                    predecessors.addKeyValue(kvs[i]);
                }
            } else if (Bytes.equals(kvs[i].getFamily(), SINGLETON_COLUMN_FAMILY)) {
                singleton = true;
            } else if (Bytes.equals(kvs[i].getFamily(), HBeanKeyValue.BEAN_COLUMN_FAMILY)) {
                hBean = new HBean(kvs[i], uids);
            }
        }
    }

    /**
     * Construct a empty row from a row key.
     */
    public HBeanRow(byte[] rowkey, UniqueIds uids) {
        this.id = getId(rowkey);
        this.uids = uids;
    }

    /**
     * Construct a empty row from a bean id.
     */
    public HBeanRow(BeanId id, UniqueIds uids) {
        byte[] rowkey = getRowKey(id, uids);
        this.id = getId(rowkey);
        this.uids = uids;
    }

    /**
     * Construct a row with properties and references initalized from the bean.
     */
    public HBeanRow(Bean bean, UniqueIds uids) {
        byte[] rowkey = getRowKey(bean.getId(), uids);
        this.id = getId(rowkey);
        this.uids = uids;
        this.references = new HBeanReferences(bean, uids);
        this.properties = new HBeanProperties(bean, uids);
        this.hBean = new HBean(bean, uids);
    }

    /**
     * Convert the row key to a long id.
     *
     * @param rowkey 6 byte row key.
     * @return long representation of the row key.
     */
    public static long getId(final byte[] rowkey) {
        return (rowkey[0] & 0xFFL) << 40 | (rowkey[1] & 0xFFL) << 32 | (rowkey[2] & 0xFFL) << 24
                | (rowkey[3] & 0xFFL) << 16 | (rowkey[4] & 0xFFL) << 8 | (rowkey[5] & 0xFFL) << 0;
    }

    /**
     * This operation is functionally identical with setting one bean on the another, i.e.
     * replacement. The only difference is that the bean is set into the row binary
     * form (not the other way around).
     */
    public void set(Bean bean) {
        properties.set(bean);
        references.set(bean, getRowKey());
    }

    public byte[] getRowKey() {
        return getRowKey(id);
    }

    public void setSingleton() {
        singleton = true;
    }

    public boolean isSingleton() {
        return singleton;
    }

    /**
     * @return long reperesentation of the row key.
     */
    public long getId() {
        return id;
    }

    public byte[] getSid() {
        byte[] rowkey = getRowKey();
        return new byte[] { rowkey[0], rowkey[1] };
    }

    public byte[] getUid() {
        byte[] rowkey = getRowKey();
        return new byte[] { rowkey[2], rowkey[3], rowkey[4], rowkey[5] };
    }

    /**
     * The bean rowkey is stored as 6 bytes, but it is represented as a big-endian
     * 8-byte long.
     */
    private static byte[] getRowKey(long id) {
        final byte[] b = new byte[6];
        b[0] = (byte) (id >>> 40);
        b[1] = (byte) (id >>> 32);
        b[2] = (byte) (id >>> 24);
        b[3] = (byte) (id >>> 16);
        b[4] = (byte) (id >>> 8);
        b[5] = (byte) (id >>> 0);
        return b;
    }

    /**
     * Get the hbase rowkey of some bean id.
     */
    public static byte[] getRowKey(final BeanId id, final UniqueIds uids) {
        final byte[] iid = uids.getUiid().getId(id.getInstanceId());
        final byte[] sid = uids.getUsid().getId(id.getSchemaName());
        final byte[] rowkey = new byte[sid.length + iid.length];
        System.arraycopy(sid, 0, rowkey, 0, sid.length);
        System.arraycopy(iid, 0, rowkey, sid.length, iid.length);
        return rowkey;
    }

    /**
     * @return convert this row's references into a set of empty rows.
     */
    public Set<HBeanRow> getReferenceRows() {
        return references.getReferences();
    }

    /**
     * This operation is functionally identical with merging two beans. The only
     * difference is that the bean merges into the row binary form (not the other
     * way around).
     */
    public void merge(Bean bean, UniqueIds uids) {
        final byte[] rowkey = getRowKey();
        properties.merge(bean, rowkey);
        references.merge(bean, rowkey);
    }

    public Set<HBeanRow> merge(HBeanRow provided, RowMutations mutations) {
        properties.merge(provided.getProperties(), mutations);
        return references.merge(provided.getReferences(), mutations);
    }

    public Set<HBeanRow> set(HBeanRow provided, RowMutations mutations) {
        properties.set(provided.getProperties(), mutations);
        return references.set(provided.getReferences(), mutations);
    }

    public HBeanProperties getProperties() {
        return properties;
    }

    public KeyValue getHBeanKeyValue() {
        return new KeyValue(getRowKey(id), HBeanKeyValue.BEAN_COLUMN_FAMILY, HBeanKeyValue.BEAN_COLUMN_FAMILY, hBean.getBytes());
    }

    /**
     * The value must be deserialized with kryo.
     *
     * @return return this row's properties in KeyValue form.
     */
    public KeyValue getPropertiesKeyValue() {
        if (properties == null) {
            return null;
        }
        return properties.getPropertiesKeyValue();
    }

    public HBeanReferences getReferences() {
        return references;
    }

    /**
     * The key consists of sid+pid and value is a compressed byte array of iids.
     *
     * @return return this row's references in KeyValue form.
     */
    public Map<String, KeyValue> getReferencesKeyValue() {
        if (references == null) {
            return new HashMap<>();
        }
        return references.getReferencesKeyValue();
    }

    public List<KeyValue> getPredecessors() {
        return predecessors.getKeyValue(getRowKey());
    }

    public List<BeanId> getPredecessorsBeanIds() {
        return predecessors.getBeanId(getRowKey(), uids);
    }

    public void addPredecessor(HBeanRow add) {
        addPredecessor(Arrays.asList(add));
    }

    public void addPredecessor(Collection<HBeanRow> rows) {
        for (HBeanRow row : rows) {
            predecessors.addRow(row);
        }
    }

    public RowMutations removePredecessor(HBeanRow row) {
        RowMutations mutation = new RowMutations(getRowKey());
        predecessors.removeRow(row);
        List<KeyValue> kvs = predecessors.getKeyValue(getRowKey());
        for (KeyValue kv : kvs) {
            try {
                Put p = new Put(getRowKey());
                p.add(kv);
                mutation.add(p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return mutation;
    }

    public void removePredecessor(Collection<HBeanRow> rows) {
        for (HBeanRow row : rows) {
            predecessors.removeRow(row);
        }
    }

    /**
     * @return convert this row into a bean.
     */
    public Bean getBean() {
        final BeanId id = getBeanId();
        final Bean bean = Bean.create(id);

        properties.setPropertiesOn(bean);
        references.setReferencesOn(bean);
        // hBean.set(bean);
        return bean;
    }

    /**
     * Convert a long row key id into a bean id.
     */
    public BeanId getBeanId() {
        final byte[] rowkey = getRowKey();
        final byte[] sid = new byte[] { rowkey[0], rowkey[1] };
        final byte[] iid = new byte[] { rowkey[2], rowkey[3], rowkey[4], rowkey[5] };
        final String schemaName = uids.getUsid().getName(sid);
        final String instanceId = uids.getUiid().getName(iid);

        if (Strings.isNullOrEmpty(schemaName)) {
            throw new IllegalStateException("Could not lookup schema name from sid "
                    + Arrays.toString(sid));
        }
        if (Strings.isNullOrEmpty(instanceId)) {
            throw new IllegalStateException("Could not lookup instance id from iid "
                    + Arrays.toString(iid));
        }
        return BeanId.create(instanceId, schemaName);
    }

    /**
     * Convert a long row key id into a bean id.
     */
    public static BeanId getBeanId(final long id, UniqueIds uids) {
        final byte[] rowkey = getRowKey(id);
        final byte[] sid = new byte[] { rowkey[0], rowkey[1] };
        final byte[] iid = new byte[] { rowkey[2], rowkey[3], rowkey[4], rowkey[5] };
        final String schemaName = uids.getUsid().getName(sid);
        final String instanceId = uids.getUiid().getName(iid);
        return BeanId.create(instanceId, schemaName);
    }

    public static BeanId getBeanId(final byte[] rowkey, UniqueIds uids) {
        return getBeanId(getId(rowkey), uids);
    }

    /**
     * Add column filter based fetch type on operation Get or Scan.
     */
    public static void setColumnFilter(Object op, FetchType... column) {
        ArrayList<byte[]> columns = new ArrayList<>();
        columns.add(DUMMY_COLUMN_FAMILY);
        if (column.length == 0) {
            // default behaviour
            columns.add(PROP_COLUMN_FAMILY);
            columns.add(PRED_COLUMN_FAMILY);
            columns.add(REF_COLUMN_FAMILY);
            columns.add(SINGLETON_COLUMN_FAMILY);
            columns.add(HBeanKeyValue.BEAN_COLUMN_FAMILY);
        } else if (FetchType.KEY_ONLY == column[0]) {
            // only IID_FAMILY
            final FilterList list = new FilterList();
            list.addFilter(new FirstKeyOnlyFilter());
            if (op instanceof Scan) {
                ((Scan) op).setFilter(list);
            } else if (op instanceof Get) {
                ((Get) op).setFilter(list);
            }
        }
        for (byte[] familiy : columns) {
            if (op instanceof Scan) {
                ((Scan) op).addFamily(familiy);
            } else if (op instanceof Get) {
                ((Get) op).addFamily(familiy);
            }
        }
    }

    public void setProperties(HBeanProperties properties) {
        this.properties = properties;
    }

    public void setReferences(Map<String, KeyValue> kvs) {
        references = new HBeanReferences(kvs, uids);

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HBeanRow)) {
            return false;
        }
        HBeanRow other = (HBeanRow) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return getBean().toString();
    }


}