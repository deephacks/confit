package org.deephacks.confit.admin.query;


/**
 * This is a convenient function for administrators that enable them to search for
 * configuration and limit/paginate through result sets. Beans are not required to
 * be indexed for this to work.
 *
 * {@link BeanQueryBuilder} is used to compose SQL-like queries together with
 * this object. Only logical AND queries are supported at the moment.
 *
 * @see {@link BeanQueryBuilder}
 */
public interface BeanQuery {
    /**
     * Add a restriction on the result set.
     *
     * @param restriction a restriction on the query.
     * @return  BeanQuery
     */
    public abstract BeanQuery add(BeanQueryBuilder.BeanRestriction restriction);

    /**
     * Set the position of the first result to retrieve. If this parameter is not
     * set, the query starts from the absolute first instance.
     *
     * @param firstResult start position of the first result.
     * @return the same query instance
     */
    public abstract BeanQuery setFirstResult(String firstResult);

    /**
     * Set the maximum number of results to retrieve.
     *
     * @param maxResults max number of instances to fetch (all by default)
     * @return the same query instance
     */
    public abstract BeanQuery setMaxResults(int maxResults);

    /**
     * Execute the query and retrieve the result set. This is not a lazy evaluation.
     *
     * @return result set.
     */
    public BeanQueryResult retrieve();

}
