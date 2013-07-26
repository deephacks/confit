package org.deephacks.confit.internal.jpa.query;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
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
import org.deephacks.confit.internal.jpa.Jpa20BeanManager;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Schema;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

import static org.deephacks.confit.admin.query.BeanQueryBuilder.equal;

public class JpaBeanQuery implements BeanQuery {
    private static final String INNER_JOIN_PROPERTY = "INNER JOIN CONFIG_PROPERTY p%d ON b.BEAN_ID = p%d.FK_BEAN_ID ";
    private static final String INNER_JOIN_REFERENCE = "INNER JOIN CONFIG_BEAN_REF r%d ON b.BEAN_ID = r%d.FK_SOURCE_BEAN_ID ";
    private final EntityManager em;
    private final Jpa20BeanManager manager;
    private final Schema schema;
    private String schemaName;
    private List<String> join = new ArrayList<>();
    private List<String> where = new ArrayList<>();
    private Optional<Integer> maxResults = Optional.absent();
    private Optional<Integer> firstResult = Optional.absent();

    public JpaBeanQuery(Schema schema, EntityManager em, Jpa20BeanManager manager) {
        this.schema = schema;
        this.schemaName = schema.getName();
        this.em = em;
        this.manager = manager;
    }


    @Override
    public BeanQuery add(BeanRestriction restriction) {
        if(restriction instanceof PropertyRestriction) {
            PropertyRestriction propertyRestriction = ((PropertyRestriction) restriction);
            String property = propertyRestriction.getProperty();
            String alias = schema.isReference(property) ? String.format("r%d", where.size()) : String.format("p%d", where.size());
            String column = schema.isReference(property) ? "FK_TARGET_BEAN_ID" : "PROP_VALUE";
            int count = join.size();
            String joinStmt = schema.isReference(property) ? String.format(INNER_JOIN_REFERENCE, count, count) : String.format(INNER_JOIN_PROPERTY, count, count);
            join.add(joinStmt);
            if (restriction instanceof Equals) {
                Object value = ((Equals) restriction).getValue();
                String query;
                if (propertyRestriction.isNot()) {
                    query = String.format(" (%s.prop_name='%s' AND NOT %s.%s='%s') ", alias, property, alias, column, value);
                } else {
                    query = String.format(" (%s.prop_name='%s' AND %s.%s='%s') ", alias, property, alias, column, value);
                }
                where.add(query);
                return this;
            } else if (restriction instanceof StringContains) {
                Object value = ((StringContains) restriction).getValue();
                String query;
                if (propertyRestriction.isNot()) {
                    query = String.format(" (%s.prop_name='%s' AND %s.%s NOT LIKE '%%%s%%') ", alias, property, alias, column, value);
                } else {
                    query = String.format(" (%s.prop_name='%s' AND %s.%s LIKE '%%%s%%') ", alias, property, alias, column, value);
                }
                where.add(query);
                return this;
            } else if (restriction instanceof Between) {
                /*
                Between between = ((Between) restriction);
                Comparable lower = between.getLower();
                Comparable upper = between.getUpper();
                return this;
                */
                throw new UnsupportedOperationException("'Between' not implemented yet");
            } else if (restriction instanceof GreaterThan) {
                Comparable value = ((GreaterThan) restriction).getValue();
                String cast = castValue(value, where.size());
                String query;
                if (propertyRestriction.isNot()) {
                    query = String.format(" (%s.prop_name='%s' AND NOT %s > %s) ", alias, property, cast, value);
                } else {
                    query = String.format(" (%s.prop_name='%s' AND %s > %s) ", alias, property, cast, value);
                }
                where.add(query);
                return this;
            } else if (restriction instanceof LessThan) {
                Comparable value = ((LessThan) restriction).getValue();
                String cast = castValue(value, where.size());
                String query;
                if (propertyRestriction.isNot()) {
                    query = String.format(" (%s.prop_name='%s' AND NOT %s < %s) ", alias, property, cast, value);
                } else {
                    query = String.format(" (%s.prop_name='%s' AND %s < %s) ", alias, property, cast, value);
                }
                where.add(query);
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
    public BeanQuery setFirstResult(int firstResult) {
        this.firstResult = Optional.of(firstResult);
        return this;
    }

    @Override
    public BeanQuery setMaxResults(int maxResults) {
        this.maxResults = Optional.of(maxResults);
        return this;
    }

    private String castValue(Comparable value, int i) {
        String type;
        if(value instanceof Double || value instanceof Float) {
            type = "DECIMAL";
        } else if(value instanceof Long ||
                  value instanceof Integer ||
                  value instanceof Short ||
                  value instanceof Byte) {
                type = "BIGINT";
        } else {
           return String.format("p%d.prop_value", i);
        }
        return String.format("CAST(p%d.prop_value AS %s)", i, type);
    }

    @Override
    public List<Bean> retrieve() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT b.BEAN_ID FROM CONFIG_BEAN b ");
        for (int i = 0; i < join.size(); i++) {
            sb.append(join.get(i));
        }
        sb.append(" WHERE ");
        sb.append(String.format("b.BEAN_SCHEMA_NAME='%s'", schemaName));

        for (int i = 0; i < where.size(); i++) {
            sb.append(" AND ");
            sb.append(where.get(i));
        }

        sb.append(" GROUP BY b.BEAN_ID ");
        System.out.println(sb.toString());
        Query q = em.createNativeQuery(sb.toString());
        if(firstResult.isPresent()) {
            q.setFirstResult(firstResult.get());
        }
        if(maxResults.isPresent()) {
            q.setMaxResults(maxResults.get());
        }
        List<String> instanceIds = q.getResultList();
        return Lists.newArrayList(manager.list(schemaName, instanceIds).values());
    }
}
