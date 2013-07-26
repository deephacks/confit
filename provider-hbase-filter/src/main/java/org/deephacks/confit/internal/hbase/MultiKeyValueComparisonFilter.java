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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.filter.FilterBase;
import org.apache.hadoop.io.WritableUtils;
import org.deephacks.confit.internal.hbase.BytesUtils.Reference;
import org.deephacks.confit.internal.hbase.BytesUtils.ReferenceList;
import org.deephacks.confit.internal.hbase.HBeanKeyValue.HBeanReader;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseBetween;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseEquals;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseGreaterThan;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseHas;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseIn;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseLessThan;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseNot;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseStringContains;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.LogicalRestriction;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.PropertyQualifierRestriction;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.QualifierRestriction;
import org.deephacks.confit.internal.hbase.query.RestrictionType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A custom HBase filter that enforce queries and pagination.
 *
 * This filter runs on the server side, inside the region server. Hence, this maven project
 * jar must be deployed on every region server. The easiest way to do this is to put the
 * jar in the lib directory, or append the jar to HBASE_CLASSPATH.
 *
 */
public class MultiKeyValueComparisonFilter extends FilterBase {
    private static final Log log = LogFactory.getLog(MultiKeyValueComparisonFilter.class);
    private byte[] sid;
    private boolean filterRow = true;
    private boolean filterAllRemaining = false;
    private List<QualifierRestriction> restrictions = new ArrayList<>();
    private int firstResult;
    private int maxResults = Integer.MAX_VALUE;
    private int matchedRows;
    private boolean foundColumn = false;


    public MultiKeyValueComparisonFilter() {
    }

    public MultiKeyValueComparisonFilter(byte[] sid) {
        this.sid = sid;
    }

    public void add(QualifierRestriction restriction) {
        if (restriction instanceof PropertyQualifierRestriction) {
            PropertyQualifierRestriction propertyRestriction = ((PropertyQualifierRestriction) restriction);
            int id = propertyRestriction.getId();
            if (restriction instanceof HBaseEquals) {
                restrictions.add(restriction);
            } else if (restriction instanceof HBaseStringContains) {
                restrictions.add(restriction);
            } else if (restriction instanceof HBaseBetween) {
                restrictions.add(restriction);
            } else if (restriction instanceof HBaseGreaterThan) {
                restrictions.add(restriction);
            } else if (restriction instanceof HBaseLessThan) {
                restrictions.add(restriction);
            } else if (restriction instanceof HBaseHas) {
                restrictions.add(restriction);
            } else if (restriction instanceof HBaseIn) {
                HBaseIn in = ((HBaseIn) restriction);
                List<Object> values = in.getValues();
                for (Object value : values) {
                    PropertyQualifierRestriction equal = (PropertyQualifierRestriction) RestrictionBuilder.equal(id, value);
                    if(in.isNot()) {
                        equal.setNot();
                    }
                    add(equal);
                }
            } else {
                throw new IllegalArgumentException("Could not identify restriction: " + restriction);
            }
        } else if(restriction instanceof LogicalRestriction) {
            if (restriction instanceof HBaseNot) {
                // support only one logical NOT statement at the moment
                PropertyQualifierRestriction not = (PropertyQualifierRestriction) ((HBaseNot) restriction).getRestrictions().get(0);
                not.setNot();
                add(not);
            }
            throw new UnsupportedOperationException("Not implemented yet");
        } else {
            throw new UnsupportedOperationException("Could not identify restriction: " + restriction);
        }

    }

    @Override
    public boolean filterAllRemaining() {
        return (matchedRows - firstResult) > maxResults || filterAllRemaining;
    }

    @Override
    public boolean filterRow() {
        return filterRow;
    }

    @Override
    public boolean filterRowKey(byte[] buffer, int offset, int length) {
        if (matchedRows < firstResult) {
            return true;
        }
        // no need, at the moment, since startRow and stopRow is used on scanner
        // return compareTo(sid, 0, sid.length, buffer, offset, sid.length) != 0;
        return false;
    }

    @Override
    public ReturnCode filterKeyValue(KeyValue kv) {
        if (foundColumn) {
            return ReturnCode.NEXT_ROW;
        }
        if(!Arrays.equals(HBeanKeyValue.BEAN_COLUMN_FAMILY, kv.getFamily())) {
            return ReturnCode.NEXT_COL;
        }
        try {
            HBeanReader hbean = new HBeanReader(kv.getValue());
            if (filter(hbean)) {
                filterRow = true;
                return ReturnCode.NEXT_ROW;
            }
            matchedRows++;
            foundColumn = true;
            filterRow = false;
            return ReturnCode.INCLUDE;
        } catch (Exception e) {
            if(log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            filterAllRemaining = true;
            filterRow = true;
            return ReturnCode.NEXT_ROW;
        }
    }

    private boolean filter(HBeanReader hbean) {
        for (QualifierRestriction restriction : restrictions) {
            if(restriction instanceof PropertyQualifierRestriction) {
                PropertyQualifierRestriction propertyRestriction = (PropertyQualifierRestriction) restriction;
                Object target = hbean.getValue(propertyRestriction.getId());
                if (target instanceof Reference) {
                    Reference ref = (Reference) target;
                    if(!propertyRestriction.evaluate(ref.getInstance())) {
                        return true;
                    }
                }
                if (target instanceof ReferenceList) {
                    ReferenceList list = (ReferenceList) target;
                    if(!propertyRestriction.evaluate(list.getInstances())) {
                        return true;
                    }
                } else {
                    if(!propertyRestriction.evaluate(target)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void reset() {
        foundColumn = false;
        filterRow = false;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        WritableUtils.writeVInt(output, sid.length);
        output.write(sid);
        WritableUtils.writeVInt(output, firstResult);
        WritableUtils.writeVInt(output, maxResults);
        WritableUtils.writeVInt(output, restrictions.size());
        for (QualifierRestriction restriction : restrictions) {
            int val = RestrictionType.valueOf(restriction).ordinal();
            WritableUtils.writeVInt(output, val);
            restriction.write(output);
        }
    }

    @Override
    public void readFields(DataInput input) throws IOException {
        int sidSize = WritableUtils.readVInt(input);
        sid = new byte[sidSize];
        input.readFully(sid);
        firstResult = WritableUtils.readVInt(input);
        maxResults = WritableUtils.readVInt(input);
        int restrictionLength = WritableUtils.readVInt(input);
        for (int i = 0; i < restrictionLength; i++) {
            QualifierRestriction restriction = RestrictionType.values()[WritableUtils.readVInt(input)].newInstance();
            restriction.readFields(input);
            restrictions.add(restriction);
        }
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }
}
