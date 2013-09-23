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

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.RowMutations;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.internal.hbase.HBeanTable.HBeanNotFoundException;
import org.deephacks.confit.internal.hbase.query.HBaseBeanQuery;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.model.BeanUtils;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.PropertyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.deephacks.confit.internal.hbase.HBeanRow.*;

/**
 * HBase implementation of Bean Manager.
 *
 * Any mofification to storage (create, set, merge, delete) must be validated in order
 * to ensure row uniqueness and referential integrity between rows.
 *
 * Since HBase does not have the foreign keys; this is a manual process that begin by
 * fetching involved beans, enforce validation rules and then synchronize updates back
 * to storage if no violations are found.
 *
 * In order to do this efficiently; rows targeted for modification are fetched in one
 * bulk and then written back to storage in one batch.
 *
 * @author Kristoffer Sjogren
 */
public class HBaseBeanManager extends BeanManager {
    private static final long serialVersionUID = 2158417054421142053L;
    private HBeanTable table;
    private UniqueIds uids;
    private Configuration conf;
    public static final String HBASE_BEAN_ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";

    public HBaseBeanManager() {
        PropertyManager systemProperties = PropertyManager.lookup();
        String value = systemProperties.get(HBASE_BEAN_ZOOKEEPER_QUORUM).or("localhost");
        conf = HBaseConfiguration.create();
        conf.set(HBASE_BEAN_ZOOKEEPER_QUORUM, value);
    }

    public HBaseBeanManager(Configuration conf) {
        this.conf = conf;
    }

    public void init() {
        if (uids == null) {
            uids = createUids(conf);
        }
        if (table == null) {
            table = new HBeanTable(conf, uids);
        }
    }

    /**
     * Create the unique lookup tables.
     * @param conf HBase configuration.
     * @return a holder for sid and iid lookup.
     */
    public static UniqueIds createUids(Configuration conf) {
        UniqueId usid = new UniqueId(SID_TABLE, SID_WIDTH, conf, true);
        UniqueId uiid = new UniqueId(IID_TABLE, IID_WIDTH, conf, true);
        UniqueId upid = new UniqueId(PID_TABLE, PID_WIDTH, conf, true);
        return new UniqueIds(uiid, usid, upid);
    }

    @Override
    public void create(Bean bean) throws AbortRuntimeException {
        init();
        create(Arrays.asList(bean));
    }

    /**
     * This operation must validate the following things.
     *
     * 1) CFG303: Parents does not already exist in storage.
     * 2) CFG301: Children exist either in storage OR memory.
     */
    @Override
    public void create(Collection<Bean> beans) throws AbortRuntimeException {
        init();
        Set<HBeanRow> beanRows = new HashSet<>();
        Map<HBeanRow, HBeanRow> refRows = new HashMap<>();
        Map<BeanId, Bean> beansMap = BeanUtils.uniqueIndex(beans);
        try {
            for (Bean bean : beans) {
                beanRows.add(new HBeanRow(bean, uids));
                for (BeanId ref : bean.getReferences()) {
                    Bean refBean = beansMap.get(ref);
                    HBeanRow row;
                    if (refBean == null) {
                        row = new HBeanRow(ref, uids);
                    } else {
                        row = new HBeanRow(refBean, uids);
                    }
                    refRows.put(row, row);
                }
            }
            table.getLazy(beanRows);
            throw Events.CFG303_BEAN_ALREADY_EXIST(beans.iterator().next().getId());
        } catch (HBeanNotFoundException e) {
            // ensure beans to be create does not exist
        }

        // ensure references exist either in memory or storage
        try {
            refRows = table.getLazyAsMap(refRows);
        } catch (HBeanNotFoundException e) {
            // only throw exception if the bean was not provided
            // as an argument to the create method.
            if (!idsFoundIn(beans, e)) {
                throw Events.CFG301_MISSING_RUNTIME_REF(e.getNotFound().iterator().next());
            }
            // make sure that references found in storage override the refRows
            for (HBeanRow row : e.getFound().keySet()) {
                refRows.put(row, row);
            }

        }
        // select the rows to update predecessors
        for (HBeanRow beanRow : beanRows) {
            for (HBeanRow refRow : beanRow.getReferenceRows()) {
                refRow = refRows.get(refRow);
                refRow.addPredecessor(beanRow);
            }
        }

        table.put(beanRows);
        table.put(Sets.newHashSet(refRows.values()));
    }

    private boolean idsFoundIn(Collection<Bean> beans, HBeanNotFoundException e) {
        for (BeanId id : e.getNotFound()) {
            boolean found = false;
            for (Bean bean : beans) {
                if (bean.getId().equals(id)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return found;
            }
        }
        return true;
    }

    @Override
    public void createSingleton(BeanId singleton) {
        init();
        HBeanRow row = new HBeanRow(singleton, uids);
        row.setSingleton();
        table.put(Sets.newHashSet(row));
    }

    @Override
    public void set(Bean bean) {
        init();
        set(Arrays.asList(bean));
    }

    /**
     * This operation must validate the following things.
     *
     * 1) CFG304: Parents exist in storage.
     * 2) CFG301: Children exist in storage.
     *
     */
    @Override
    public void set(Collection<Bean> beans) throws AbortRuntimeException {
        init();
        Map<HBeanRow, HBeanRow> providedRows = new HashMap<>();
        for (Bean bean : beans) {
            HBeanRow row = new HBeanRow(bean, uids);
            providedRows.put(row, row);
            for (BeanId id : bean.getReferences()) {
                HBeanRow refRow = new HBeanRow(id, uids);
                providedRows.put(refRow, refRow);
            }
        }
        HBeanRowCollector storedCollector = null;
        try {
            storedCollector = table.getEager(providedRows);
        } catch (HBeanNotFoundException e) {
            // only throw exception if the bean was not provided
            if (!idsFoundIn(beans, e)) {
                throw Events.CFG301_MISSING_RUNTIME_REF(e.getNotFound().iterator().next());
            }
        }

        Map<HBeanRow, HBeanRow> storedRows = storedCollector.getRowMap();
        Set<RowMutations> mutations = new HashSet<>();

        for (HBeanRow provided : providedRows.keySet()) {
            HBeanRow stored = storedRows.get(provided);
            RowMutations rowMutations = new RowMutations(provided.getRowKey());
            Set<HBeanRow> deleteReferences = stored.set(provided, rowMutations);
            try {
                deleteReferences = table.getLazy(deleteReferences);
                for (HBeanRow row : deleteReferences) {
                    RowMutations mutation = row.removePredecessor(provided);
                    mutations.add(mutation);
                }
            } catch (HBeanNotFoundException e) {
                throw new IllegalStateException("Broken predecessors");
            }
            mutations.add(rowMutations);
        }
        table.mutate(mutations);
    }

    @Override
    public void merge(Bean bean) throws AbortRuntimeException {
        init();
        merge(Arrays.asList(bean));
    }

    /**
     * This operation must validate the following things.
     *
     * 1) CFG304: Parents exist in storage.
     * 2) CFG301: Children exist in storage.
     */
    @Override
    public void merge(Collection<Bean> beans) throws AbortRuntimeException {
        init();
        Map<HBeanRow, HBeanRow> providedRows = new HashMap<>();
        for (Bean bean : beans) {
            HBeanRow row = new HBeanRow(bean, uids);
            providedRows.put(row, row);
            for (BeanId id : bean.getReferences()) {
                HBeanRow refRow = new HBeanRow(id, uids);
                providedRows.put(refRow, refRow);
            }
        }
        HBeanRowCollector storedCollector = null;
        try {
            storedCollector = table.getEager(providedRows);
        } catch (HBeanNotFoundException e) {
            // only throw exception if the bean was not provided
            if (!idsFoundIn(beans, e)) {
                throw Events.CFG301_MISSING_RUNTIME_REF(e.getNotFound().iterator().next());
            }
        }

        Map<HBeanRow, HBeanRow> storedRows = storedCollector.getRowMap();
        Set<RowMutations> mutations = new HashSet<>();

        for (HBeanRow provided : providedRows.keySet()) {
            HBeanRow stored = storedRows.get(provided);
            RowMutations rowMutations = new RowMutations(provided.getRowKey());
            Set<HBeanRow> deleteReferences = stored.merge(provided, rowMutations);
            try {
                deleteReferences = table.getLazy(deleteReferences);
                for (HBeanRow row : deleteReferences) {
                    RowMutations mutation = row.removePredecessor(provided);
                    mutations.add(mutation);
                }
            } catch (HBeanNotFoundException e) {
                throw new IllegalStateException("Broken predecessors");
            }
            mutations.add(rowMutations);
        }
        table.mutate(mutations);
    }

    @Override
    public Optional<Bean> getEager(BeanId id) throws AbortRuntimeException {
        init();
        final HashSet<HBeanRow> row = Sets.newHashSet(new HBeanRow(id, uids));
        try {
            HBeanRowCollector result = table.getEager(row);
            List<Bean> beans = result.getBeans();
            if (beans == null || beans.size() == 0) {
                return Optional.absent();
            }
            return Optional.of(beans.get(0));
        } catch (HBeanNotFoundException e) {
            return Optional.absent();
        }
    }

    @Override
    public Optional<Bean> getLazy(BeanId id) throws AbortRuntimeException {
        init();
        final HashSet<HBeanRow> row = Sets.newHashSet(new HBeanRow(id, uids));
        try {
            Set<HBeanRow> result = table.getLazy(row);
            if (result == null || result.size() == 0) {
                throw Events.CFG304_BEAN_DOESNT_EXIST(id);
            }
            return Optional.of(result.iterator().next().getBean());
        } catch (HBeanNotFoundException e) {
            throw Events.CFG304_BEAN_DOESNT_EXIST(e.getNotFound().iterator().next());
        }
    }

    @Override
    public Optional<Bean> getSingleton(String schemaName) throws IllegalArgumentException {
        init();
        Collection<Bean> beans = list(schemaName).values();
        if (beans.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(beans.iterator().next());
    }

    @Override
    public Map<BeanId, Bean> list(String schemaName) throws AbortRuntimeException {
        init();
        HBeanRowCollector result;
        try {
            result = table.listEager(schemaName);
            List<Bean> beans = result.getBeans();
            return BeanUtils.uniqueIndex(beans);
        } catch (HBeanNotFoundException e) {
            throw Events.CFG304_BEAN_DOESNT_EXIST(e.getNotFound().iterator().next());
        }
    }

    @Override
    public Map<BeanId, Bean> getBeanToValidate(Collection<Bean> beans) throws AbortRuntimeException {
        init();
        Set<HBeanRow> rows = new HashSet<>();
        for (Bean bean : beans) {
            rows.add(new HBeanRow(bean.getId(), uids));
        }
        try {
            HBeanRowCollector collector = table.getEager(rows);
            return BeanUtils.uniqueIndex(collector.getAllBeans());
        } catch (HBeanNotFoundException e) {
            throw Events.CFG304_BEAN_DOESNT_EXIST(e.getNotFound().iterator().next());
        }
    }

    @Override
    public Map<BeanId, Bean> list(String schemaName, Collection<String> ids)
            throws AbortRuntimeException {
        init();
        HashSet<HBeanRow> rows = new HashSet<>();
        for (String id : ids) {
            rows.add(new HBeanRow(BeanId.create(id, schemaName), uids));
        }

        try {
            HBeanRowCollector result = table.getEager(rows);
            List<Bean> beans = result.getBeans();
            return BeanUtils.uniqueIndex(beans);
        } catch (HBeanNotFoundException e) {
            throw Events.CFG304_BEAN_DOESNT_EXIST(e.getNotFound().iterator().next());
        }

    }

    @Override
    public Bean delete(BeanId id) throws AbortRuntimeException {
        init();
        Optional<Bean> bean = getEager(id);
        delete(id.getSchemaName(), Arrays.asList(id.getInstanceId()));
        return bean.get();
    }

    /**
     * This operation must validate the following things.
     *
     * 1) CFG302: No parents reference these beans.
     *
     */
    @Override
    public Collection<Bean> delete(String schemaName, Collection<String> instanceIds)
            throws AbortRuntimeException {
        init();
        Map<BeanId, Bean> deleted = list(schemaName, instanceIds);
        Set<HBeanRow> rows = new HashSet<>();
        for (String id : instanceIds) {
            rows.add(new HBeanRow(BeanId.create(id, schemaName), uids));
        }
        try {
            rows = table.getEager(rows).getRows();
            ArrayList<BeanId> predecessors = new ArrayList<>();
            for (HBeanRow row : rows) {
                for (BeanId id : row.getPredecessorsBeanIds()) {
                    predecessors.add(id);
                }
            }
            if (predecessors.size() > 0) {
                throw Events.CFG302_CANNOT_DELETE_BEAN(predecessors);
            }
        } catch (HBeanNotFoundException e) {
            throw Events.CFG304_BEAN_DOESNT_EXIST(e.getNotFound().iterator().next());
        }
        table.delete(rows);
        return deleted.values();
    }

    @Override
    public BeanQuery newQuery(Schema schema) {
        init();
        return new HBaseBeanQuery(schema, table, uids);
    }

}
