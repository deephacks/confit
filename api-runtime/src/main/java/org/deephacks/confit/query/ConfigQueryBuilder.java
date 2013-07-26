package org.deephacks.confit.query;

import java.util.Arrays;
import java.util.List;

/**
 * A static builder for creating {@link ConfigQuery} objects.
 *
 * Note that each field/property that is queried must have an index annotation.
 *
 * Querying references to other configurable instances are treated as string
 * values containing the instance id(s), single or list values (maps are treated
 * as lists).
 */
public class ConfigQueryBuilder {
    /**
     * Query which asserts that a property contains a certain string.
     *
     * @param property field to query
     * @param value value to query for
     * @return restriction to be added to {@link ConfigQuery}.
     */
    public static Restriction contains(String property, String value) {
        return new StringContains(property, value);
    }

    /**
     * Query which asserts that a property equals certain value.
     *
     * @param property field to query
     * @param value value to query for
     * @return restriction to be added to {@link ConfigQuery}.
     */
    public static Restriction equal(String property, Object value) {
        return new Equals(property, value);
    }

    /**
     * Query which asserts that a property is between a lower and an upper bound,
     * inclusive.
     *
     * @param property field to query
     * @param lower lower value to be asserted by the query
     * @param upper upper value to be asserted by the query
     * @return restriction to be added to {@link ConfigQuery}.
     */
    public static <A extends Comparable<A>> Restriction between(String property, A lower, A upper) {
        return new Between<>(property, lower, upper);
    }

    /**
     * Query which asserts that a property is greater than (but not equal to) a value.
     *
     * @param property field to query
     * @param value value to query for
     * @return restriction to be added to {@link ConfigQuery}.
     */
    public static <A extends Comparable<A>> Restriction greaterThan(String property, A value) {
        return new GreaterThan<>(property, value);
    }

    /**
     * Query which asserts that a property is less than (but not equal to) a value.
     *
     * @param property field to query
     * @param value value to query for
     * @return restriction to be added to {@link ConfigQuery}.
     */
    public static <A extends Comparable<A>> Restriction lessThan(String property, A value) {
        return new LessThan(property, value);

    }

    /**
     * Query which asserts that an attribute has a value (is not null).
     *
     * @param property field to query
     * @return restriction to be added to {@link ConfigQuery}.
     */
    public static Restriction has(String property) {
        return new Has(property);
    }

    /**
     * A shorthand way to create an OR query comprised of several equal queries.
     *
     * @param property field to query
     * @param values value to query for
     * @return restriction to be added to {@link ConfigQuery}.
     */
    public static Restriction in(String property, Object... values) {
        return new In(property, Arrays.asList(values));
    }

    /**
     * A query representing a logical AND of the provided restrictions.
     s
     * @param r1 Restriction one
     * @param r2 Restriction two
     * @return An AND restriction.
     */
    public static Restriction and(Restriction r1, Restriction r2) {
        return new And(Arrays.asList(r1, r2));
    }

    /**
     * A restriction representing a logical OR of the provided restrictions.
     s
     * @param r1 Restriction one
     * @param r2 Restriction two
     * @return An OR restriction.
     */
    public static Restriction or(Restriction r1, Restriction r2) {
        return new Or(Arrays.asList(r1, r2));
    }

    /**
     * A restriction representing a logical NOT of the provided restriction.
     *
     * @param r a restriction
     * @return A NOT restriction.
     */
    public static Restriction not(Restriction r) {
        return new Not(Arrays.asList(r));
    }



    public static interface Restriction {

    }

    public static abstract class PropertyRestriction implements Restriction {
        private String property;

        public PropertyRestriction(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
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

    public static abstract class LogicalRestriction implements Restriction {
        private List<Restriction> restrictions;

        public LogicalRestriction(List<Restriction> restrictions) {
            this.restrictions = restrictions;
        }

        public List<Restriction> getRestrictions() {
            return restrictions;
        }

    }

    public static class Or extends LogicalRestriction {

        public Or(List<Restriction> restrictions) {
            super(restrictions);
        }
    }

    public static class And extends LogicalRestriction {

        public And(List<Restriction> restrictions) {
            super(restrictions);
        }
    }

    public static class Not extends LogicalRestriction {

        public Not(List<Restriction> restrictions) {
            super(restrictions);
        }
    }

}
