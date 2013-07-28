package org.deephacks.confit.internal.cached.query;


import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.query.Query;
import org.deephacks.confit.query.ConfigQueryBuilder.And;
import org.deephacks.confit.query.ConfigQueryBuilder.Between;
import org.deephacks.confit.query.ConfigQueryBuilder.Equals;
import org.deephacks.confit.query.ConfigQueryBuilder.GreaterThan;
import org.deephacks.confit.query.ConfigQueryBuilder.Has;
import org.deephacks.confit.query.ConfigQueryBuilder.In;
import org.deephacks.confit.query.ConfigQueryBuilder.LessThan;
import org.deephacks.confit.query.ConfigQueryBuilder.LogicalRestriction;
import org.deephacks.confit.query.ConfigQueryBuilder.Not;
import org.deephacks.confit.query.ConfigQueryBuilder.Or;
import org.deephacks.confit.query.ConfigQueryBuilder.PropertyRestriction;
import org.deephacks.confit.query.ConfigQueryBuilder.Restriction;
import org.deephacks.confit.query.ConfigQueryBuilder.StringContains;
import org.deephacks.confit.spi.CacheManager;

import java.util.ArrayList;
import java.util.Collection;

import static com.googlecode.cqengine.query.QueryFactory.*;


public class ConfigQuery<T> implements org.deephacks.confit.query.ConfigQuery {

    private Query query;
    private ConfigIndexedCollection collection;
    private final CacheManager cacheManager;

    public ConfigQuery(ConfigIndexedCollection collection, CacheManager cacheManager) {
        this.collection = collection;
        this.cacheManager = cacheManager;
    }

    public ConfigQuery(ConfigIndexedCollection collection, Query query, CacheManager cacheManager) {
        this.query = query;
        this.collection = collection;
        this.cacheManager = cacheManager;
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public org.deephacks.confit.query.ConfigQuery add(Restriction restriction) {
        if(restriction instanceof PropertyRestriction) {
            String property = ((PropertyRestriction) restriction).getProperty();
            Attribute attr = collection.getAttribute(property);
            if (restriction instanceof Equals) {
                Query q = equal(attr, ((Equals) restriction).getValue());
                if(query != null) {
                    q = and(query, q);
                }
                return new ConfigQuery<>(collection, q, cacheManager);
            } else if (restriction instanceof StringContains) {
                Query q = contains(attr, ((StringContains) restriction).getValue());
                if(query != null) {
                    q = and(query, q);
                }
                return new ConfigQuery<>(collection, q, cacheManager);
            } else if (restriction instanceof Between) {
                Between _between = ((Between) restriction);
                Query q = between(attr, _between.getLower(), _between.getUpper());
                if(query != null) {
                    q = and(query, q);
                }
                return new ConfigQuery<>(collection, q, cacheManager);
            } else if (restriction instanceof GreaterThan) {
                Query q = greaterThan(attr, ((GreaterThan) restriction).getValue());
                if(query != null) {
                    q = and(query, q);
                }
                return new ConfigQuery<>(collection, q, cacheManager);
            } else if (restriction instanceof LessThan) {
                LessThan lesser = ((LessThan) restriction);
                Query q = lessThan(attr, lesser.getValue());
                if(query != null) {
                    q = and(query, q);
                }

                return new ConfigQuery<>(collection, q, cacheManager);
            } else if (restriction instanceof Has) {
                Query q = has(attr);
                if(query != null) {
                    q = and(query, q);
                }
                return new ConfigQuery<>(collection, q, cacheManager);
            } else if (restriction instanceof In) {
                In _in = ((In) restriction);
                Query q = in(attr, _in.getValues());
                if(query != null) {
                    q = and(query, q);
                }
                return new ConfigQuery<>(collection, q, cacheManager);
            } else {
                throw new IllegalArgumentException("Could not identify restriction: " + restriction);
            }
        } else if(restriction instanceof LogicalRestriction) {
            if (restriction instanceof And) {
                And and = (And) restriction;
                Collection<Query> restrictions = new ArrayList<>();
                for (Restriction res : and.getRestrictions()) {
                    restrictions.add(((ConfigQuery)add(res)).getQuery());
                }
                Query q = new com.googlecode.cqengine.query.logical.And(restrictions);
                if(query != null) {
                    q = and(query, q);
                }
                return new ConfigQuery<>(collection, q, cacheManager);
            } else if (restriction instanceof Or) {
                Or and = (Or) restriction;
                Collection<Query> restrictions = new ArrayList<>();
                for (Restriction res : and.getRestrictions()) {
                    restrictions.add(((ConfigQuery) add(res)).getQuery());
                }
                Query q = new com.googlecode.cqengine.query.logical.Or(restrictions, true);
                if(query != null) {
                    q = and(query, q);
                }
                return new ConfigQuery<>(collection, q, cacheManager);
            } else if (restriction instanceof Not) {
                Not not = (Not) restriction;
                Restriction res = not.getRestrictions().get(0);
                Query q = new com.googlecode.cqengine.query.logical.Not(((ConfigQuery)add(res)).getQuery());
                if(query != null) {
                    q = and(query, q);
                }
                return new ConfigQuery<>(collection, q, cacheManager);
            } else {
                throw new IllegalArgumentException("Could not identify restriction: " + restriction);
            }
        }
        throw new IllegalArgumentException("Could not identify restriction: " + restriction);
    }

    @Override
    public org.deephacks.confit.query.ConfigResultSet retrieve() {
        if(query == null) {
            return new ConfigResultSet(collection.all(), cacheManager);
        }
        com.googlecode.cqengine.resultset.ResultSet resultSet = collection.retrieve(query);
        return (org.deephacks.confit.query.ConfigResultSet) new ConfigResultSet(resultSet, cacheManager);
    }
}
