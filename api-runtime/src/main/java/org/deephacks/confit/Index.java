package org.deephacks.confit;

import org.deephacks.confit.query.ConfigQuery;
import org.deephacks.confit.query.ConfigQueryBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark fields as indexed which is requirement when using
 * {@link ConfigQuery} in order to compose queries.
 *
 * Every indexed field of bean instances are kept on-heap and enable queries with
 * ultra-low latency over many instances.
 *
 * Instances themselves are generally not kept on-heap in order to lower the pressure
 * on garbage collection. Usually instances are cached in direct memory off-heap,
 * as directed by the cache manager. Indexes and caches are automatically kept in sync
 * with bean manager storage as instances are administrated (created, updated, deleted).
 *
 * Note that a CacheManager MUST be available in order to perform queries. It is the
 * CacheManager that store and fetch particular instances from query result sets.
 *
 * @see {@link ConfigQueryBuilder}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
@Inherited
public @interface Index {
}
