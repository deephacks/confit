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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.hadoop.hbase.KeyValue;
import org.deephacks.confit.model.Bean.BeanId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.deephacks.confit.internal.hbase.HBeanRow.PRED_COLUMN_FAMILY;

public class HBeanPredecessors {
    private final Map<Short, byte[]> predecessors = new HashMap<>();

    public HBeanPredecessors() {

    }

    public HBeanPredecessors(KeyValue kv) {
        if (!Arrays.equals(kv.getFamily(), HBeanRow.PRED_COLUMN_FAMILY)) {
            throw new IllegalArgumentException("KeyValue is not a predecessor column family");
        }
        short sid = Bytes.getShort(kv.getQualifier());
        predecessors.put(sid, kv.getValue());
    }

    public List<KeyValue> getKeyValue(byte[] rowkey) {
        ArrayList<KeyValue> kvs = new ArrayList<>();
        for (short sid : predecessors.keySet()) {
            byte[] sidBytes = Bytes.fromShort(sid);
            kvs.add(new KeyValue(rowkey, HBeanRow.PRED_COLUMN_FAMILY, sidBytes, predecessors
                    .get(sid)));
        }
        return kvs;
    }

    public List<BeanId> getBeanId(byte[] rowkey, UniqueIds uids) {
        List<KeyValue> kvs = getKeyValue(rowkey);
        List<BeanId> result = new ArrayList<>();

        for (int i = 0; i < kvs.size(); i++) {
            byte[] sid = kvs.get(i).getQualifier();
            byte[][] ids = getIds(kvs.get(i));

            for (byte[] id : ids) {
                byte[] key = new byte[6];
                System.arraycopy(sid, 0, key, 0, 2);
                System.arraycopy(id, 0, key, 2, 4);
                result.add(HBeanRow.getBeanId(key, uids));
            }

        }
        return result;

    }

    public static Multimap<byte[], byte[]> getPredecessors(Multimap<String, String> predecessors,
            final UniqueIds uids) {
        final Multimap<byte[], byte[]> bytes = ArrayListMultimap.create();

        for (String schemaName : predecessors.keySet()) {
            final byte[] sid = uids.getUsid().getId(schemaName);
            Collection<String> ids = predecessors.get(schemaName);
            bytes.put(sid, HBeanReferences.getIids(new ArrayList<String>(ids), uids));
        }
        return bytes;
    }

    /**
     * If this key value is of predecessor familiy type.
     */
    public static boolean isPredecessor(KeyValue kv) {
        if (Bytes.equals(kv.getFamily(), PRED_COLUMN_FAMILY)) {
            return true;
        }
        return false;
    }

    public void addRow(HBeanRow row) {
        addId(row.getSid(), row.getUid());
    }

    public void addKeyValue(KeyValue kv) {
        byte[][] ids = getIds(kv);
        for (byte[] id : ids) {
            addId(kv.getQualifier(), id);
        }
    }

    public void addId(byte[] sid, byte[] id) {
        validateInput(sid, id);
        short shortId = Bytes.getShort(sid);
        byte[] ids = predecessors.get(shortId);
        if (ids == null) {
            ids = new byte[0];
        }
        ids = BytesUtils.add(ids, Bytes.getInt(id));
        predecessors.put(shortId, ids);
    }

    public void removeKeyValue(KeyValue kv) {
        byte[][] ids = getIds(kv);
        for (byte[] id : ids) {
            removeId(kv.getQualifier(), id);
        }
    }

    public void removeRow(HBeanRow row) {
        removeId(row.getSid(), row.getUid());
    }

    private byte[][] getIds(KeyValue kv) {
        byte[] bytes = kv.getValue();
        int num = bytes.length / 4;
        byte[][] ids = new byte[num][];
        for (int i = 0; i < num; i++) {
            int idx = i * 4;
            ids[i] = new byte[] { bytes[idx + 0], bytes[idx + 1], bytes[idx + 2], bytes[idx + 3] };
        }
        return ids;

    }

    public void removeId(byte[] sid, byte[] id) {
        validateInput(sid, id);
        short shortId = Bytes.getShort(sid);
        byte[] ids = predecessors.get(shortId);
        ids = BytesUtils.remove(ids, Bytes.getInt(id));
        predecessors.put(shortId, ids);

    }

    private void validateInput(byte[] sid, byte[] id) {
        if (sid.length != 2) {
            throw new IllegalArgumentException("sid must be 2 bytes.");
        }
        if (id.length != 4) {
            throw new IllegalArgumentException("Ids must be 4 bytes.");
        }
    }

}
