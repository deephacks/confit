package org.deephacks.confit.query;


import org.deephacks.confit.query.ConfigQueryBuilder.Restriction;

/**
 * Used for retrieving configurable instances by composing <tt>Criterion</tt>
 * objects that scan indexed fields.
 *
 * {@link ConfigQueryBuilder} is used to compose SQL-like queries together with
 * this object.
 *
 * This feature is currently experimental and must be turned on by setting
 * System Property 'typesafe.query.enabled' to true.
 *
 * @see {@link ConfigQueryBuilder}
 */
public interface ConfigQuery<T> {
    /**
     * Add another criterion on this query. This is treated as a logical AND operation
     * with regards to the already composed query.
     *
     * @param restriction a restriction on the query.
     * @return ConfigQuery
     */
    public abstract ConfigQuery<T> add(Restriction restriction);

    /**
     * Execute the query and retrieve a lazy evaluated result set.
     *
     * @return result set.
     */
    public ConfigResultSet<T> retrieve();

}
