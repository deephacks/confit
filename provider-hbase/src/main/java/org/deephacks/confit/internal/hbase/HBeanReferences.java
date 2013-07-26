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

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RowMutations;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;

import java.io.IOException;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.deephacks.confit.internal.hbase.HBeanRow.IID_WIDTH;
import static org.deephacks.confit.internal.hbase.HBeanRow.REF_COLUMN_FAMILY;

public class HBeanReferences {
    private Map<String, KeyValue> references = new HashMap<>();
    private final UniqueIds uids;

    public HBeanReferences(UniqueIds uids) {
        this.uids = uids;
    }

    public HBeanReferences(final Bean bean, final UniqueIds uids) {
        this.uids = uids;
        byte[] rowkey = HBeanRow.getRowKey(bean.getId(), uids);
        Map<String, KeyValue> references = new HashMap<>();

        for (String propertyName : bean.getReferenceNames()) {
            List<BeanId> refs = bean.getReference(propertyName);
            String schemaName = bean.getSchema().getReferenceSchemaName(propertyName);
            KeyValue kv = getReferenceKeyValue(rowkey, propertyName, schemaName, refs, uids);
            references.put(propertyName, kv);
        }
        this.references = references;
    }

    public HBeanReferences(Map<String, KeyValue> kvs, UniqueIds uids) {
        this.references = kvs;
        this.uids = uids;
    }

    public Map<String, KeyValue> getReferences(final byte[] rowkey, final Bean bean,
            final UniqueIds uids) {
        Map<String, KeyValue> references = new HashMap<>();

        for (String propertyName : bean.getReferenceNames()) {
            List<BeanId> refs = bean.getReference(propertyName);
            if (refs == null || refs.size() == 0) {
                continue;
            }
            String schemaName = bean.getSchema().getReferenceSchemaName(propertyName);
            KeyValue kv = getReferenceKeyValue(rowkey, propertyName, schemaName, refs, uids);
            references.put(propertyName, kv);
        }
        return references;
    }

    /**
     *  Get a particular type of references identified by propertyName into key value
     *  form.
     * @param schemaName
     */
    public static KeyValue getReferenceKeyValue(byte[] rowkey, String propertyName,
            String schemaName, List<BeanId> refs, UniqueIds uids) {
        final byte[] pid = uids.getUsid().getId(propertyName);
        final byte[] sid = uids.getUsid().getId(schemaName);
        final byte[] qual = new byte[] { sid[0], sid[1], pid[0], pid[1] };
        final byte[] iids = getIids2(refs, uids);
        return new KeyValue(rowkey, REF_COLUMN_FAMILY, qual, iids);
    }

    public void merge(Bean bean, byte[] rowkey) {
        // merge references
        Map<String, KeyValue> mergedRefs = new HashMap<>();
        for (String propertyName : references.keySet()) {
            String schemaName = bean.getSchema().getReferenceSchemaName(propertyName);
            KeyValue kv = references.get(propertyName);
            final List<BeanId> refs = bean.getReference(propertyName);
            KeyValue mergekv = null;
            if (refs == null) {
                mergekv = kv.deepCopy();
            } else {
                mergekv = HBeanReferences.getReferenceKeyValue(rowkey, propertyName, schemaName,
                        refs, uids);
            }
            mergedRefs.put(propertyName, mergekv);
        }
        references = mergedRefs;

    }

    public Set<HBeanRow> merge(HBeanReferences provided, RowMutations mutations) {
        if (provided == null) {
            return new HashSet<>();
        }
        // merge references
        Set<HBeanRow> removedRowKeys = new HashSet<>();
        Map<String, KeyValue> providedRefs = provided.getReferencesKeyValue();
        for (String propertyName : providedRefs.keySet()) {
            KeyValue providedkv = providedRefs.get(propertyName);
            KeyValue existingkv = references.get(propertyName);
            if (existingkv != null) {
                Map<String, HBeanRow> providedRowKeys = getRowKeys(providedkv);
                Map<String, HBeanRow> existingRowKeys = getRowKeys(existingkv);
                removedRowKeys = getDeletedKeys(providedRowKeys, existingRowKeys);
            }

            try {
                if (providedkv != null) {
                    Put p = new Put(mutations.getRow());
                    p.add(providedkv);
                    mutations.add(p);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return removedRowKeys;
    }

    public Set<HBeanRow> set(HBeanReferences provided, RowMutations mutations) {
        if (provided == null) {
            return new HashSet<>();
        }
        Set<HBeanRow> removedRowKeys = merge(provided, mutations);
        Map<String, KeyValue> providedRefs = provided.getReferencesKeyValue();
        for (String propertyName : references.keySet()) {
            KeyValue providedkv = providedRefs.get(propertyName);
            if (providedkv == null) {
                KeyValue existingkv = references.get(propertyName);
                try {
                    Delete d = new Delete(mutations.getRow());
                    d.deleteColumns(existingkv.getFamily(), existingkv.getQualifier());
                    mutations.add(d);

                    Map<String, HBeanRow> existingRowKeys = getRowKeys(existingkv);
                    removedRowKeys.addAll(existingRowKeys.values());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        return removedRowKeys;
    }

    private Set<HBeanRow> getDeletedKeys(Map<String, HBeanRow> providedRowKeys,
            Map<String, HBeanRow> existingRowKeys) {
        HashSet<HBeanRow> deleted = new HashSet<>();
        for (String instanceId : existingRowKeys.keySet()) {
            if (providedRowKeys.get(instanceId) == null) {
                deleted.add(existingRowKeys.get(instanceId));
            }
        }
        return deleted;
    }

    private Map<String, HBeanRow> getRowKeys(KeyValue existing) {
        Map<String, HBeanRow> rowkeys = new HashMap<>();
        final byte[] sidpid = existing.getQualifier();
        final byte[] iids = existing.getValue();
        final byte[] sid = new byte[] { sidpid[0], sidpid[1] };
        for (int i = 0; i < iids.length; i += IID_WIDTH) {
            final byte[] iid = new byte[] { iids[i], iids[i + 1], iids[i + 2], iids[i + 3] };
            final byte[] rowkey = new byte[] { sid[0], sid[1], iids[i], iids[i + 1], iids[i + 2],
                    iids[i + 3] };
            final String instanceId = uids.getUiid().getName(iid);

            rowkeys.put(instanceId, new HBeanRow(rowkey, uids));
        }
        return rowkeys;
    }

    public void set(Bean bean, byte[] rowkey) {
        // overwrite row references and nullify existing ones
        Map<String, KeyValue> setRefs = new HashMap<>();
        for (String propertyName : references.keySet()) {
            String schemaName = bean.getSchema().getReferenceSchemaName(propertyName);
            final KeyValue kv = references.get(propertyName);
            final List<BeanId> refs = bean.getReference(propertyName);
            KeyValue setkv = null;
            if (refs == null) {
                setkv = new KeyValue(rowkey, kv.getQualifier(), null);
            } else {
                setkv = HBeanReferences.getReferenceKeyValue(rowkey, propertyName, schemaName,
                        refs, uids);
            }
            setRefs.put(propertyName, setkv);
        }
        this.references = setRefs;
    }

    /**
     * Set references on a bean using a set of key values containing references.
     */
    public void setReferencesOn(Bean bean) {
        for (KeyValue ref : references.values()) {
            final byte[] sidpid = ref.getQualifier();
            final byte[] iids = ref.getValue();
            final byte[] sid = new byte[] { sidpid[0], sidpid[1] };
            final byte[] pid = new byte[] { sidpid[2], sidpid[3] };

            final String schemaName = uids.getUsid().getName(sid);
            final String propertyName = uids.getUsid().getName(pid);

            for (int i = 0; i < iids.length; i += IID_WIDTH) {
                final byte[] iid = new byte[] { iids[i], iids[i + 1], iids[i + 2], iids[i + 3] };
                final String instanceId = uids.getUiid().getName(iid);
                bean.addReference(propertyName, BeanId.create(instanceId, schemaName));
            }
        }
    }

    public Map<String, KeyValue> getReferencesKeyValue() {
        return references;
    }

    public Set<HBeanRow> getReferences() {
        final Set<HBeanRow> rows = new HashSet<>();
        for (KeyValue ref : references.values()) {
            byte[] sid = ref.getQualifier();
            byte[] iids = ref.getValue();
            for (int i = 0; i < iids.length; i += IID_WIDTH) {
                final byte[] rowkey = new byte[] { sid[0], sid[1], iids[i], iids[i + 1],
                        iids[i + 2], iids[i + 3] };
                rows.add(new HBeanRow(rowkey, uids));
            }
        }
        return rows;
    }

    /**
     * If this key value is of reference familiy type.
     */
    public static boolean isReference(KeyValue kv) {
        if (Bytes.equals(kv.getFamily(), REF_COLUMN_FAMILY)) {
            return true;
        }
        return false;
    }

    public void add(KeyValue kv) {
        String propertyName = HBeanProperties.getPropertyName(kv, uids);
        references.put(propertyName, kv);
    }

    /**
     * Compress a set of instances ids into a byte array where each id consist of
     * 4 bytes each.
     */
    public static byte[] getIids(final List<String> ids, final UniqueIds uids) {
        final int size = ids.size();
        final byte[] iids = new byte[IID_WIDTH * size];
        for (int i = 0; i < size; i++) {
            final String instanceId = ids.get(i);
            final byte[] iid = uids.getUiid().getId(instanceId);
            System.arraycopy(iid, 0, iids, i * IID_WIDTH, IID_WIDTH);
        }
        return iids;
    }

    /**
     * Second version of getIids that takes a set of bean ids instead.
     */
    private static byte[] getIids2(final List<BeanId> ids, final UniqueIds uids) {
        if (ids == null) {
            return new byte[0];
        }
        final AbstractList<String> list = new AbstractList<String>() {

            @Override
            public String get(int index) {
                return ids.get(index).getInstanceId();
            }

            @Override
            public int size() {
                return ids.size();
            }
        };
        return getIids(list, uids);
    }

}
