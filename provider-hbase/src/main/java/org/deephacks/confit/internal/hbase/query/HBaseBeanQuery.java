package org.deephacks.confit.internal.hbase.query;


import org.apache.hadoop.hbase.util.Bytes;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.admin.query.BeanQueryBuilder.BeanRestriction;
import org.deephacks.confit.admin.query.BeanQueryBuilder.Between;
import org.deephacks.confit.admin.query.BeanQueryBuilder.Equals;
import org.deephacks.confit.admin.query.BeanQueryBuilder.GreaterThan;
import org.deephacks.confit.admin.query.BeanQueryBuilder.Has;
import org.deephacks.confit.admin.query.BeanQueryBuilder.In;
import org.deephacks.confit.admin.query.BeanQueryBuilder.LessThan;
import org.deephacks.confit.admin.query.BeanQueryBuilder.LogicalRestriction;
import org.deephacks.confit.admin.query.BeanQueryBuilder.Not;
import org.deephacks.confit.admin.query.BeanQueryBuilder.PropertyRestriction;
import org.deephacks.confit.admin.query.BeanQueryBuilder.StringContains;
import org.deephacks.confit.admin.query.BeanQueryResult;
import org.deephacks.confit.internal.hbase.HBeanRow;
import org.deephacks.confit.internal.hbase.HBeanTable;
import org.deephacks.confit.internal.hbase.UniqueIds;
import org.deephacks.confit.internal.hbase.MultiKeyValueComparisonFilter;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseBetween;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseEquals;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseGreaterThan;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseLessThan;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseStringContains;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Schema;

import java.util.ArrayList;
import java.util.List;

import static org.deephacks.confit.admin.query.BeanQueryBuilder.equal;

public class HBaseBeanQuery implements BeanQuery {
    private final HBeanTable table;
    private final MultiKeyValueComparisonFilter filter;
    private final UniqueIds uids;
    private final byte[] sid;
    private String firstResult;
    private int maxResults = Integer.MAX_VALUE;

    public HBaseBeanQuery(Schema schema, HBeanTable table, UniqueIds uids) {
        this.uids = uids;
        this.table = table;
        this.sid = table.extractSidPrefix(schema.getName());
        filter = new MultiKeyValueComparisonFilter(sid);
    }

    @Override
    public BeanQuery add(BeanRestriction restriction) {
        if(restriction instanceof PropertyRestriction) {
            PropertyRestriction propertyRestriction = ((PropertyRestriction) restriction);
            String property = propertyRestriction.getProperty();
            int id = Bytes.toShort(uids.getUpid().getId(property));
            if (restriction instanceof Equals) {
                Equals specific = (Equals) restriction;
                HBaseEquals  hbaseRestriction = new HBaseEquals(id, specific.getValue());
                if(propertyRestriction.isNot()) {
                    hbaseRestriction.setNot();
                }
                filter.add(hbaseRestriction);
                return this;
            } else if (restriction instanceof StringContains) {
                StringContains specific = (StringContains) restriction;
                HBaseStringContains hbaseRestriction = new HBaseStringContains(id, specific.getValue());
                if(propertyRestriction.isNot()) {
                    hbaseRestriction.setNot();
                }
                filter.add(hbaseRestriction);
                return this;
            } else if (restriction instanceof Between) {
                Between specific = (Between) restriction;
                HBaseBetween hbaseRestriction = new HBaseBetween(id, specific.getLower(), specific.getUpper());
                filter.add(hbaseRestriction);
                return this;
            } else if (restriction instanceof GreaterThan) {
                GreaterThan specific = (GreaterThan) restriction;
                HBaseGreaterThan hbaseRestriction = new HBaseGreaterThan(id, specific.getValue());
                filter.add(hbaseRestriction);
                return this;
            } else if (restriction instanceof LessThan) {
                LessThan specific = (LessThan) restriction;
                HBaseLessThan hbaseRestriction = new HBaseLessThan(id, specific.getValue());
                filter.add(hbaseRestriction);
                return this;
            } else if (restriction instanceof Has) {
                throw new UnsupportedOperationException("'Has' not implemented yet");
            } else if (restriction instanceof In) {
                In in = ((In) restriction);
                List<Object> values = in.getValues();
                for (Object value : values) {
                    PropertyRestriction equal = (PropertyRestriction) equal(property, value);
                    if(in.isNot()) {
                        equal.setNot();
                    }
                    add(equal);
                }
                return this;
            } else {
                throw new IllegalArgumentException("Could not identify restriction: " + restriction);
            }
        } else if(restriction instanceof LogicalRestriction) {
            if (restriction instanceof Not) {
                // support only one logical NOT statement at the moment
                PropertyRestriction not = (PropertyRestriction) ((Not) restriction).getRestrictions().get(0);
                not.setNot();
                add(not);
                return this;
            }
            throw new UnsupportedOperationException("logical restriction not supported " + restriction);
        }
        throw new UnsupportedOperationException("Could not identify restriction: " + restriction);
    }

    @Override
    public BeanQuery setFirstResult(String firstResult) {
        this.firstResult = firstResult;
        return this;
    }

    @Override
    public BeanQuery setMaxResults(int maxResults) {
        filter.setMaxResults(maxResults);
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public BeanQueryResult retrieve() {
        final List<Bean> beans = new ArrayList<>();
        final List<HBeanRow> rows = table.scan(sid, filter, firstResult);
        int i = 0;
        String firstResult = null;
        for (HBeanRow row : rows) {
            if (i++ < maxResults) {
                beans.add(row.getBean());
            } else {
                firstResult = row.getBean().getId().getInstanceId();
                break;
            }
        }
        final String nextFirstResult = firstResult;
        return new BeanQueryResult() {
            @Override
            public List<Bean> get() {
                return beans;
            }

            @Override
            public String nextFirstResult() {
                return nextFirstResult;
            }
        };
    }
}
