package org.deephacks.confit.internal.mapdb.query;

import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.admin.query.BeanQueryBuilder;
import org.deephacks.confit.admin.query.BeanQueryBuilder.BeanRestriction;
import org.deephacks.confit.admin.query.BeanQueryBuilder.Between;
import org.deephacks.confit.admin.query.BeanQueryBuilder.Equals;
import org.deephacks.confit.admin.query.BeanQueryBuilder.GreaterThan;
import org.deephacks.confit.admin.query.BeanQueryBuilder.Has;
import org.deephacks.confit.admin.query.BeanQueryBuilder.In;
import org.deephacks.confit.admin.query.BeanQueryBuilder.LessThan;
import org.deephacks.confit.admin.query.BeanQueryBuilder.Not;
import org.deephacks.confit.admin.query.BeanQueryBuilder.StringContains;
import org.deephacks.confit.admin.query.BeanQueryResult;
import org.deephacks.confit.internal.mapdb.query.RestrictionBuilder.DefaultBetween;
import org.deephacks.confit.internal.mapdb.query.RestrictionBuilder.DefaultEquals;
import org.deephacks.confit.internal.mapdb.query.RestrictionBuilder.DefaultGreaterThan;
import org.deephacks.confit.internal.mapdb.query.RestrictionBuilder.DefaultLessThan;
import org.deephacks.confit.internal.mapdb.query.RestrictionBuilder.DefaultStringContains;
import org.deephacks.confit.internal.mapdb.query.RestrictionBuilder.PropertyRestriction;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.serialization.BeanSerialization;
import org.deephacks.confit.spi.serialization.UniqueIds;
import org.deephacks.confit.spi.serialization.ValueSerialization.ValueReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.deephacks.confit.admin.query.BeanQueryBuilder.equal;

public class DefaultBeanQuery implements BeanQuery {
        private final LinkedHashMap<BeanId, byte[]> beans;
        private final Schema schema;
        private final UniqueIds uniqueIds;
        private int maxResults = Integer.MAX_VALUE;
        private int firstResult;
        private ArrayList<PropertyRestriction> restrictions = new ArrayList<>();

        public DefaultBeanQuery(Schema schema, LinkedHashMap<BeanId, byte[]> beans, UniqueIds uniqueIds) {
            this.beans = beans;
            this.schema = schema;
            this.uniqueIds = uniqueIds;
        }

        @Override
        public BeanQuery add(BeanRestriction restriction) {
            if(restriction instanceof BeanQueryBuilder.PropertyRestriction) {
                BeanQueryBuilder.PropertyRestriction propertyRestriction = ((BeanQueryBuilder.PropertyRestriction) restriction);
                String property = propertyRestriction.getProperty();
                if (restriction instanceof Equals) {
                    Equals specific = (Equals) restriction;
                    DefaultEquals defaultRestriction = new DefaultEquals(property, specific.getValue());
                    if(propertyRestriction.isNot()) {
                        defaultRestriction.setNot();
                    }
                    restrictions.add(defaultRestriction);
                    return this;
                } else if (restriction instanceof StringContains) {
                    StringContains specific = (StringContains) restriction;
                    DefaultStringContains defaultRestriction = new DefaultStringContains(property, specific.getValue());
                    if(propertyRestriction.isNot()) {
                        defaultRestriction.setNot();
                    }
                    restrictions.add(defaultRestriction);
                    return this;
                } else if (restriction instanceof Between) {
                    Between specific = (Between) restriction;
                    DefaultBetween defaultRestriction = new DefaultBetween(property, specific.getLower(), specific.getUpper());
                    restrictions.add(defaultRestriction);
                    return this;
                } else if (restriction instanceof GreaterThan) {
                    GreaterThan specific = (GreaterThan) restriction;
                    DefaultGreaterThan defaultRestriction = new DefaultGreaterThan(property, specific.getValue());
                    restrictions.add(defaultRestriction);
                    return this;
                } else if (restriction instanceof LessThan) {
                    LessThan specific = (LessThan) restriction;
                    DefaultLessThan defaultRestriction = new DefaultLessThan(property, specific.getValue());
                    restrictions.add(defaultRestriction);
                    return this;
                } else if (restriction instanceof Has) {
                    throw new UnsupportedOperationException("'Has' not implemented yet");
                } else if (restriction instanceof In) {
                    In in = ((In) restriction);
                    List<Object> values = in.getValues();
                    for (Object value : values) {
                        BeanQueryBuilder.PropertyRestriction equal = (BeanQueryBuilder.PropertyRestriction) equal(property, value);
                        if(in.isNot()) {
                            equal.setNot();
                        }
                        add(equal);
                    }
                    return this;
                } else {
                    throw new IllegalArgumentException("Could not identify restriction: " + restriction);
                }
            } else if(restriction instanceof BeanQueryBuilder.LogicalRestriction) {
                if (restriction instanceof Not) {
                    // support only one logical NOT statement at the moment
                    BeanQueryBuilder.PropertyRestriction not = (BeanQueryBuilder.PropertyRestriction) ((Not) restriction).getRestrictions().get(0);
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
            try {
                this.firstResult = Integer.parseInt(firstResult);
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not parse firstResult into an integer.");
            }
            return this;
        }

        @Override
        public BeanQuery setMaxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        @Override
        public BeanQueryResult retrieve() {
            final ArrayList<Bean> result = new ArrayList<>();
            int iterations = 0;
            for (BeanId id : beans.keySet()) {
                if (iterations >= firstResult && result.size() < maxResults) {
                    byte[] current = beans.get(id);
                    if (matches(current)) {
                        BeanSerialization serialization = new BeanSerialization(uniqueIds.getPropertyIds());
                        Bean match = serialization.read(current, id, schema);
                        result.add(match);
                    }
                }
                if (result.size() > maxResults) {
                    break;
                }
                iterations++;
            }
            final String nextFirstResult = Integer.toString(firstResult + result.size());
            return new BeanQueryResult() {
                @Override
                public List<Bean> get() {
                    return result;
                }

                @Override
                public String nextFirstResult() {
                    return nextFirstResult;
                }
            };
        }

    private boolean matches(byte[] current) {
        for (PropertyRestriction restriction : restrictions) {
            String propertyName = restriction.getPropertyName();
            int id = (int) uniqueIds.getPropertyIds().getId(propertyName);
            ValueReader reader = new ValueReader(current);
            if (schema.isProperty(propertyName)) {
                if (!restriction.evaluate(reader.getValue(id))) {
                    return false;
                }
            } else if (schema.isReference(propertyName)) {
                if (!restriction.evaluate(reader.getValue(id))) {
                    return false;
                }
            } else {
                throw new IllegalArgumentException("Property not recognized " + propertyName);
            }
        }
        return true;
    }
}