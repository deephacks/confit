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
package org.deephacks.confit.internal.hbase.query;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.deephacks.confit.internal.hbase.BytesUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RestrictionBuilder {

    public static QualifierRestriction contains(int id, String value) {
        return new HBaseStringContains(id, value);
    }

    public static QualifierRestriction equal(int id, Object value) {
        return new HBaseEquals(id, value);
    }

    public static <A extends Comparable<A>> QualifierRestriction between(int id, A lower, A upper) {
        return new HBaseBetween<>(id, lower, upper);
    }

    public static <A extends Comparable<A>> QualifierRestriction greaterThan(int id, A value) {
        return new HBaseGreaterThan<>(id, value);
    }

    public static <A extends Comparable<A>> QualifierRestriction lessThan(int id, A value) {
        return new HBaseLessThan<>(id, value);
    }

    /*
    public static BeanRestriction has(String property) {
        return new Has(property);
    }
    */

    public static QualifierRestriction in(int id, Object... values) {
        return new HBaseIn(id, Arrays.asList(values));
    }

    public static QualifierRestriction not(QualifierRestriction r) {
        return new HBaseNot(Arrays.asList(r));
    }

    public static interface QualifierRestriction extends Writable {

    }

    public static abstract class PropertyQualifierRestriction implements QualifierRestriction {
        protected int id;
        protected boolean isNot = false;

        public PropertyQualifierRestriction() {

        }

        public PropertyQualifierRestriction(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setNot() {
            this.isNot = true;
        }

        public boolean isNot() {
            return this.isNot;
        }

        public abstract boolean evaluate(Object target);
    }

    public static class HBaseStringContains extends PropertyQualifierRestriction {
        private String value;

        public HBaseStringContains() {

        }

        public HBaseStringContains(int id, String value) {
            super(id);
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            WritableUtils.writeVInt(out, getId());
            BytesUtils.write(out, isNot());
            BytesUtils.write(out, value);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            id = WritableUtils.readVInt(in);
            isNot = (boolean) BytesUtils.read(in);
            value = (String) BytesUtils.read(in);
        }

        @Override
        public boolean evaluate(Object target) {
            if(target == null) {
                return false;
            }
            if (target instanceof Collection) {
                for (Object object : (Collection) target) {
                    if (contains(object)){
                        return true;
                    }
                }
            } else {
                return contains(target);
            }
            return false;
        }

        private boolean contains(Object target) {
            boolean match = target.toString().contains(value);
            return isNot ? !match : match;
        }
    }


    public static class HBaseEquals extends PropertyQualifierRestriction {
        private Object value;

        public HBaseEquals() {

        }

        public HBaseEquals(int id, Object value) {
            super(id);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            WritableUtils.writeVInt(out, getId());
            BytesUtils.write(out, isNot());
            BytesUtils.write(out, value);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            id = WritableUtils.readVInt(in);
            isNot = (boolean) BytesUtils.read(in);
            value = BytesUtils.read(in);
        }

        @Override
        public boolean evaluate(Object target) {
            if(target == null) {
                return false;
            }
            if (target instanceof Collection) {
                for (Object object : (Collection) target) {
                    if(equal(object)) {
                        return true;
                    }
                }
            } else {
                return equal(target);
            }
            return false;
        }

        public boolean equal(Object target) {
            boolean match = target.equals(value);
            return isNot ? !match : match;

        }
    }

    public static class HBaseBetween<A extends Comparable<A>> extends PropertyQualifierRestriction {
        private A lower;
        private A upper;

        public HBaseBetween() {

        }

        public HBaseBetween(int id, A lower, A upper) {
            super(id);
            this.lower = lower;
            this.upper = upper;
        }

        public A getLower() {
            return lower;
        }

        public A getUpper() {
            return upper;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            WritableUtils.writeVInt(out, getId());
            BytesUtils.write(out, lower);
            BytesUtils.write(out, upper);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            id = WritableUtils.readVInt(in);
            lower = (A) BytesUtils.read(in);
            upper = (A) BytesUtils.read(in);
        }

        @Override
        public boolean evaluate(Object target) {
            if (target == null || !(target instanceof Comparable)) {
                return false;
            }
            if (target instanceof Collection) {
                // fixme
                return false;
            } else {
                return ((Comparable) target).compareTo(lower) > 0 && ((Comparable) target).compareTo(upper) < 0;
            }
        }
    }

    public static class HBaseGreaterThan<A extends Comparable<A>> extends PropertyQualifierRestriction {
        private A value;

        public HBaseGreaterThan() {

        }

        public HBaseGreaterThan(int id, A value) {
            super(id);
            this.value = value;
        }

        public A getValue() {
            return value;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            WritableUtils.writeVInt(out, getId());
            BytesUtils.write(out, value);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            id = WritableUtils.readVInt(in);
            value = (A) BytesUtils.read(in);
        }

        @Override
        public boolean evaluate(Object target) {
            if (target == null || !(target instanceof Comparable)) {
                return false;
            }
            if (target instanceof Collection) {
                // fixme
                return false;
            } else {
                return ((Comparable) target).compareTo(value) > 0;
            }
        }
    }

    public static class HBaseLessThan<A extends Comparable<A>> extends PropertyQualifierRestriction {
        private A value;

        public HBaseLessThan() {

        }

        public HBaseLessThan(int id, A value) {
            super(id);
            this.value = value;
        }
        public A getValue() {
            return value;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            WritableUtils.writeVInt(out, getId());
            BytesUtils.write(out, value);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            id = WritableUtils.readVInt(in);
            value = (A) BytesUtils.read(in);
        }

        @Override
        public boolean evaluate(Object target) {
            if (target == null || !(target instanceof Comparable)) {
                return false;
            }
            if (target instanceof Collection) {
                // fixme
                return false;
            } else {
                return ((Comparable) target).compareTo(value) < 0;
            }
        }
    }

    public static class HBaseHas extends PropertyQualifierRestriction {

        public HBaseHas(int id) {
            super(id);
        }

        @Override
        public void write(DataOutput out) throws IOException {
            WritableUtils.writeVInt(out, getId());
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            id = WritableUtils.readVInt(in);
        }

        @Override
        public boolean evaluate(Object target) {
            throw new UnsupportedOperationException("not implemented");
        }
    }

    public static class HBaseIn extends PropertyQualifierRestriction {
        private final List<Object> values;

        public HBaseIn(int id, List<Object> values) {
            super(id);
            this.values = values;
        }

        public List<Object> getValues() {
            return values;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public boolean evaluate(Object target) {
            throw new UnsupportedOperationException("not implemented");
        }
    }

    public static abstract class LogicalRestriction implements QualifierRestriction {
        private List<QualifierRestriction> restrictions;

        public LogicalRestriction(List<QualifierRestriction> restrictions) {
            this.restrictions = restrictions;
        }

        public List<QualifierRestriction> getRestrictions() {
            return restrictions;
        }

    }

    public static class HBaseOr extends LogicalRestriction {

        public HBaseOr(List<QualifierRestriction> restrictions) {
            super(restrictions);
        }

        @Override
        public void write(DataOutput out) throws IOException {
        }

        @Override
        public void readFields(DataInput in) throws IOException {
        }
    }

    public static class HBaseAnd extends LogicalRestriction {

        public HBaseAnd(List<QualifierRestriction> restrictions) {
            super(restrictions);
        }

        @Override
        public void write(DataOutput out) throws IOException {
        }

        @Override
        public void readFields(DataInput in) throws IOException {
        }
    }

    public static class HBaseNot extends LogicalRestriction {

        public HBaseNot(List<QualifierRestriction> restrictions) {
            super(restrictions);
        }

        @Override
        public void write(DataOutput out) throws IOException {
        }

        @Override
        public void readFields(DataInput in) throws IOException {
        }
    }

}
