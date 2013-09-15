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
package org.deephacks.confit.internal.mapdb.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RestrictionBuilder {

    public static Restriction contains(String propertyName, String value) {
        return new DefaultStringContains(propertyName, value);
    }

    public static Restriction equal(String propertyName, Object value) {
        return new DefaultEquals(propertyName, value);
    }

    public static <A extends Comparable<A>> Restriction between(String propertyName, A lower, A upper) {
        return new DefaultBetween<>(propertyName, lower, upper);
    }

    public static <A extends Comparable<A>> Restriction greaterThan(String propertyName, A value) {
        return new DefaultGreaterThan<>(propertyName, value);
    }

    public static <A extends Comparable<A>> Restriction lessThan(String propertyName, A value) {
        return new DefaultLessThan<>(propertyName, value);
    }

    /*
    public static BeanRestriction has(String property) {
        return new DefaultHas(property);
    }
    */

    public static Restriction in(String propertyName, Object... values) {
        return new DefaultIn(propertyName, Arrays.asList(values));
    }

    public static Restriction not(Restriction r) {
        return new DefaultNot(Arrays.asList(r));
    }

    public static interface Restriction {

    }

    public static abstract class PropertyRestriction implements Restriction {
        protected String propertyName;
        protected boolean isNot = false;

        public PropertyRestriction() {

        }

        public PropertyRestriction(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public void setNot() {
            this.isNot = true;
        }

        public boolean isNot() {
            return this.isNot;
        }

        public abstract boolean evaluate(Object target);
    }

    public static class DefaultStringContains extends PropertyRestriction {
        private String value;

        public DefaultStringContains() {

        }

        public DefaultStringContains(String propertyName, String value) {
            super(propertyName);
            this.value = value;
        }

        public String getValue() {
            return value;
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


    public static class DefaultEquals extends PropertyRestriction {
        private Object value;

        public DefaultEquals() {

        }

        public DefaultEquals(String propertyName, Object value) {
            super(propertyName);
            this.value = value;
        }

        public Object getValue() {
            return value;
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

    public static class DefaultBetween<A extends Comparable<A>> extends PropertyRestriction {
        private A lower;
        private A upper;

        public DefaultBetween() {

        }

        public DefaultBetween(String propertyName, A lower, A upper) {
            super(propertyName);
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

    public static class DefaultGreaterThan<A extends Comparable<A>> extends PropertyRestriction {
        private A value;

        public DefaultGreaterThan() {

        }

        public DefaultGreaterThan(String propertyName, A value) {
            super(propertyName);
            this.value = value;
        }

        public A getValue() {
            return value;
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

    public static class DefaultLessThan<A extends Comparable<A>> extends PropertyRestriction {
        private A value;

        public DefaultLessThan() {

        }

        public DefaultLessThan(String propertyName, A value) {
            super(propertyName);
            this.value = value;
        }
        public A getValue() {
            return value;
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

    public static class DefaultHas extends PropertyRestriction {

        public DefaultHas(String propertyName) {
            super(propertyName);
        }

        @Override
        public boolean evaluate(Object target) {
            throw new UnsupportedOperationException("not implemented");
        }
    }

    public static class DefaultIn extends PropertyRestriction {
        private final List<Object> values;

        public DefaultIn(String propertyName, List<Object> values) {
            super(propertyName);
            this.values = values;
        }

        public List<Object> getValues() {
            return values;
        }

        @Override
        public boolean evaluate(Object target) {
            throw new UnsupportedOperationException("not implemented");
        }
    }

    public static abstract class LogicalRestriction implements Restriction {
        private List<Restriction> restrictions;

        public LogicalRestriction(List<Restriction> restrictions) {
            this.restrictions = restrictions;
        }

        public List<Restriction> getRestrictions() {
            return restrictions;
        }

    }

    public static class DefaultOr extends LogicalRestriction {

        public DefaultOr(List<Restriction> restrictions) {
            super(restrictions);
        }

    }

    public static class DefaultAnd extends LogicalRestriction {

        public DefaultAnd(List<Restriction> restrictions) {
            super(restrictions);
        }

    }

    public static class DefaultNot extends LogicalRestriction {

        public DefaultNot(List<Restriction> restrictions) {
            super(restrictions);
        }

    }

}
