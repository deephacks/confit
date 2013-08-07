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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.client.RowMutations;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.deephacks.confit.model.Bean.BeanId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.deephacks.confit.internal.hbase.HBeanRow.BEAN_TABLE;

/**
 * The table where beans are stored.
 *
 * Each bean is kept in a separate row. Each operation on a bean will be atomic on
 * individual beans, but not accross beans. Each bean will also be versioned
 * as a whole.
 *
 */
public class HBeanTable {
    /** Lookup table for short ids: sid, iid, pid. */
    private final UniqueIds uids;
    /** table used for storing key values */
    private final HTable table;
    /** the max depth allowed for traversing and fetching references */
    private final static int FETCH_DEPTH_MAX = 10;

    public HBeanTable(Configuration conf, UniqueIds uids) {
        table = getBeanTable(conf);
        this.uids = uids;
    }

    public static HTable getBeanTable(Configuration conf) {
        try {
            return new HTable(conf, BEAN_TABLE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetch list rows for a particular schema and traverse and fetch references eagerly.
     *
     * @param schemaName schema to fetch
     * @param fetchType data to fetch
     * @return collector carring the result from the query
     */
    public HBeanRowCollector listEager(String schemaName, FetchType... fetchType)
            throws HBeanNotFoundException {
        Set<HBeanRow> rows = listLazy(schemaName, fetchType);
        HBeanRowCollector collector = new HBeanRowCollector(rows);
        getEager(rows, collector, FETCH_DEPTH_MAX, fetchType);
        return collector;
    }

    /**
     * Fetch a set of rows and traverse and fetch references eagerly.
     *
     * @param rows to fetch
     * @param fetchType data to fetch
     * @return collector carring the result from the query
     */
    public HBeanRowCollector getEager(Set<HBeanRow> rows, FetchType... fetchType)
            throws HBeanNotFoundException {
        Set<HBeanRow> result;
        result = getLazy(rows, fetchType);
        HBeanRowCollector collector = new HBeanRowCollector(result);
        getEager(result, collector, FETCH_DEPTH_MAX, fetchType);
        return collector;
    }

    public HBeanRowCollector getEager(Map<HBeanRow, HBeanRow> rows, FetchType... fetchType)
            throws HBeanNotFoundException {
        return getEager(rows.keySet(), fetchType);
    }

    /**
     * Fetch a set of rows without fetching their references.
     *
     * @param schemaName schema to fetch
     * @param fetchType data to fetch
     * @return rows found
     */
    public Set<HBeanRow> listLazy(String schemaName, FetchType... fetchType) {
        byte[] sid = extractSidPrefix(schemaName);
        Scan scan = new Scan();
        HBeanRow.setColumnFilter(scan, fetchType);
        FilterList list = new FilterList();
        if (scan.getFilter() != null) {
            list.addFilter(scan.getFilter());

        }
        list.addFilter(new PrefixFilter(sid));
        scan.setFilter(list);

        Set<HBeanRow> rows = new HashSet<>();
        try {
            ResultScanner scanner = table.getScanner(scan);
            for (Result r : scanner) {
                HBeanRow row = new HBeanRow(r.raw(), uids);
                rows.add(row);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    /**
     * Extract sid from schema name.
     */
    public byte[] extractSidPrefix(final String schemaName) {
        return uids.getUsid().getId(schemaName);
    }

    /**
     * Fetch a set of rows without fetching their references.
     *
     * @param rows  rows to fetch
     * @param fetchType data to fetch
     * @return rows found
     */
    public Set<HBeanRow> getLazy(Set<HBeanRow> rows, FetchType... fetchType)
            throws HBeanNotFoundException {
        final List<Row> gets = new ArrayList<>(rows.size());
        for (HBeanRow row : rows) {
            final Get get = new Get(row.getRowKey());
            HBeanRow.setColumnFilter(get, fetchType);
            gets.add(get);
        }
        Set<HBeanRow> result = new HashSet<>();
        Set<BeanId> notFound = new HashSet<>();
        try {
            final Result[] r = new Result[gets.size()];
            table.batch(gets, r);
            table.flushCommits();
            for (int i = 0; i < r.length; i++) {
                KeyValue[] kvs = r[i].raw();
                if (kvs.length == 0) {
                    notFound.add(new HBeanRow(gets.get(i).getRow(), uids).getBeanId());
                } else {
                    HBeanRow row = new HBeanRow(kvs, uids);
                    result.add(row);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (notFound.size() > 0) {
            throw new HBeanNotFoundException(notFound, result);
        }
        return result;
    }

    public Map<HBeanRow, HBeanRow> getLazyAsMap(Map<HBeanRow, HBeanRow> rows,
            FetchType... fetchType) throws HBeanNotFoundException {
        Set<HBeanRow> result = getLazy(rows.keySet(), fetchType);
        HashMap<HBeanRow, HBeanRow> map = new HashMap<>();
        for (HBeanRow row : result) {
            map.put(row, row);
        }
        return map;
    }

    public void mutate(Set<RowMutations> mutations) {
        try {
            List<RowMutations> batch = new ArrayList<>();
            for (RowMutations mutation : mutations) {
                batch.add(mutation);
            }
            table.batch(batch);
            table.flushCommits();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Put a set of rows.
     *
     * @param rows to put.
     */
    public void put(Set<HBeanRow> rows) {
        final List<Row> create = new ArrayList<>();
        try {
            for (HBeanRow row : rows) {
                final Put write = new Put(row.getRowKey());
                if (row.getPropertiesKeyValue() != null) {
                    write.add(row.getPropertiesKeyValue());
                }
                for (KeyValue kv : row.getPredecessors()) {
                    write.add(kv);
                }
                for (KeyValue kv : row.getReferencesKeyValue().values()) {
                    write.add(kv);
                }

                KeyValue hBean = row.getHBeanKeyValue();
                write.add(hBean);

                if (row.isSingleton()) {
                    write.add(new KeyValue(row.getRowKey(), HBeanRow.SINGLETON_COLUMN_FAMILY,
                            HBeanRow.SINGLETON_COLUMN_FAMILY, new byte[] { 1 }));
                }
                // hbase cannot have rowkeys without columns so we need
                // a dummy value to represent beans without any values
                write.add(new KeyValue(row.getRowKey(), HBeanRow.DUMMY_COLUMN_FAMILY,
                        HBeanRow.DUMMY_COLUMN_FAMILY, new byte[] { 1 }));
                create.add(write);
            }
            table.batch(create);
            table.flushCommits();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Recursive method for traversing and collecting row references.
     */
    private void getEager(Set<HBeanRow> rows, HBeanRowCollector collector, int level,
            FetchType... fetchType) throws HBeanNotFoundException {
        int size = rows.size();
        if (size == 0) {
            return;
        }
        if (--level < 0) {
            return;
        }
        Set<HBeanRow> refs = new HashSet<>();
        for (HBeanRow row : rows) {
            refs.addAll(row.getReferenceRows());
        }
        // only recurse rowkeys we havent already
        // visited to break circular references
        refs = collector.filterUnvisted(refs);
        refs = getLazy(refs, fetchType);

        collector.addReferences(refs);
        getEager(refs, collector, level, fetchType);
    }

    /**
     * Delete a set of rows.
     *
     * @param rows to be deleted.
     */
    public void delete(Set<HBeanRow> rows) {
        final List<Row> delete = new ArrayList<>();
        try {
            for (HBeanRow row : rows) {
                delete.add(new Delete(row.getRowKey()));
            }
            table.batch(delete);
            table.flushCommits();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Only scan instances of a specific schemaName (sid) by setting
     * start and stop row accordingly on the filter.
     */
    public List<HBeanRow> scan(byte[] sid, MultiKeyValueComparisonFilter filter, String firstResult) {
        Scan scan = new Scan();
        HBeanRow.setColumnFilter(scan);
        scan.setFilter(filter);
        final byte[] firstRowKey;
        if (!Strings.isNullOrEmpty(firstResult)) {
            byte[] uid = uids.getUiid().getId(firstResult);
            firstRowKey = getRowKey(sid, uid);
        } else {
            byte[] uid = uids.getUiid().getMinWidth();
            firstRowKey = getRowKey(sid, uid);
        }
        scan.setStartRow(firstRowKey);

        byte[] uid = uids.getUiid().getMaxWidth();
        byte[] stopRowKey = getRowKey(sid, uid);
        scan.setStopRow(stopRowKey);
        ArrayList<HBeanRow> rows = new ArrayList<>();
        try {
            ResultScanner scanner = table.getScanner(scan);
            for (Result r : scanner) {
                HBeanRow row = new HBeanRow(r.raw(), uids);
                rows.add(row);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    private byte[] getRowKey(byte[] sid, byte[] uid) {
        final byte[] rowkey = new byte[sid.length + uid.length];
        System.arraycopy(sid, 0, rowkey, 0, sid.length);
        System.arraycopy(uid, 0, rowkey, sid.length, uid.length);
        return rowkey;
    }

    public static class HBeanNotFoundException extends Exception {
        private static final long serialVersionUID = -7994691832123397253L;
        private Set<BeanId> notFound = new HashSet<>();
        private Set<HBeanRow> found = new HashSet<>();

        public HBeanNotFoundException(Set<BeanId> notFound, Set<HBeanRow> found) {
            Preconditions.checkNotNull(notFound);
            Preconditions.checkNotNull(found);
            this.notFound.addAll(notFound);
            this.found.addAll(found);
        }

        public Set<BeanId> getNotFound() {
            return notFound;
        }

        public Map<HBeanRow, HBeanRow> getFound() {
            HashMap<HBeanRow, HBeanRow> map = new HashMap<>();
            for (HBeanRow row : found) {
                map.put(row, row);
            }
            return map;
        }
    }

}