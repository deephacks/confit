package org.deephacks.confit.admin.query;

import java.util.Arrays;
import java.util.List;

/**
 * A static builder for creating {@link BeanQuery} objects.
 *
 * Index annotations are not required on fields/properties.
 *
 * Querying references to other configurable instances are treated as string
 * values containing the instance id(s), single or list values (maps are treated
 * as lists).
 */
public class BeanQueryBuilder {
    /**
     * Query which asserts that a property contains a certain string.
     *
     * @param property field to query
     * @param value value to query for
     * @return restriction to be added to {@link BeanQuery}.
     */
    public static BeanRestriction contains(String property, String value) {
        return new StringContains(property, value);
    }

    /**
     * Query which asserts that a property equals certain value.
     *
     * @param property field to query
     * @param value value to query for
     * @return restriction to be added to {@link BeanQuery}.
     */
    public static BeanRestriction equal(String property, Object value) {
        return new Equals(property, value);
    }

    /**
     * Query which asserts that a property is between a lower and an upper bound,
     * inclusive.
     *
     * @param property field to query
     * @param lower lower value to be asserted by the query
     * @param upper upper value to be asserted by the query
     * @return restriction to be added to {@link BeanQuery}.
     */
    public static <A extends Comparable<A>> BeanRestriction between(String property, A lower, A upper) {
        return new Between<>(property, lower, upper);
    }

    /**
     * Query which asserts that a property is greater than (but not equal to) a value.
     *
     * @param property field to query
     * @param value value to query for
     * @return restriction to be added to {@link BeanQuery}.
     */
    public static <A extends Comparable<A>> BeanRestriction greaterThan(String property, A value) {
        return new GreaterThan<>(property, value);
    }

    /**
     * Query which asserts that a property is less than (but not equal to) a value.
     *
     * @param property field to query
     * @param value value to query for
     * @return restriction to be added to {@link BeanQuery}.
     */
    public static <A extends Comparable<A>> BeanRestriction lessThan(String property, A value) {
        return new LessThan(property, value);

    }

    /**
     * Query which asserts that an attribute has a value (is not null).
     *
     * @param property field to query
     * @return restriction to be added to {@link BeanQuery}.
     */
    /*
    public static BeanRestriction has(String property) {
        return new Has(property);
    }
    */

    /**
     * A shorthand way to create an AND query comprised of several equal queries.
     *
     * @param property field to query
     * @param values value to query for
     * @return restriction to be added to {@link BeanQuery}.
     */
    public static BeanRestriction in(String property, Object... values) {
        return new In(property, Arrays.asList(values));
    }

    /**
     * A restriction representing a logical NOT of the provided restriction.
     *
     * @param r a restriction
     * @return A NOT restriction.
     */
    public static BeanRestriction not(BeanRestriction r) {
        return new Not(Arrays.asList(r));
    }

    public static interface BeanRestriction {

    }

    public static abstract class PropertyRestriction implements BeanRestriction {
        private String property;
        private boolean isNot = false;
        public PropertyRestriction(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }

        public void setNot() {
            this.isNot = true;
        }

        public boolean isNot() {
            return this.isNot;
        }

    }

    public static class StringContains extends PropertyRestriction {
        private String value;

        public StringContains(String property, String value) {
            super(property);
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class Equals extends PropertyRestriction {
        private Object value;

        public Equals(String property, Object value) {
            super(property);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class Between<A extends Comparable<A>> extends PropertyRestriction {
        private A lower;
        private A upper;

        public Between(String property, A lower, A upper) {
            super(property);
            this.lower = lower;
            this.upper = upper;
        }

        public A getLower() {
            return lower;
        }

        public A getUpper() {
            return upper;
        }
    }

    public static class GreaterThan<A extends Comparable<A>> extends PropertyRestriction {
        private A value;

        public GreaterThan(String property, A value) {
            super(property);
            this.value = value;
        }

        public A getValue() {
            return value;
        }
    }

    public static class LessThan<A extends Comparable<A>> extends PropertyRestriction {
        private A value;

        public LessThan(String property, A value) {
            super(property);
            this.value = value;
        }
        public A getValue() {
            return value;
        }
    }

    public static class Has extends PropertyRestriction {

        public Has(String property) {
            super(property);
        }
    }

    public static class In extends PropertyRestriction {
        private final List<Object> values;
        public In(String property, List<Object> values) {
            super(property);
            this.values = values;
        }

        public List<Object> getValues() {
            return values;
        }
    }

    public static abstract class LogicalRestriction implements BeanRestriction {
        private List<BeanRestriction> restrictions;

        public LogicalRestriction(List<BeanRestriction> restrictions) {
            this.restrictions = restrictions;
        }

        public List<BeanRestriction> getRestrictions() {
            return restrictions;
        }

    }

    public static class Or extends LogicalRestriction {

        public Or(List<BeanRestriction> restrictions) {
            super(restrictions);
        }
    }

    public static class And extends LogicalRestriction {

        public And(List<BeanRestriction> restrictions) {
            super(restrictions);
        }
    }

    public static class Not extends LogicalRestriction {

        public Not(List<BeanRestriction> restrictions) {
            super(restrictions);
        }
    }
}


