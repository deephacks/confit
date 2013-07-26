package org.deephacks.confit.query;

import java.util.Iterator;

/**
 * A lazy evaluated result set iterator matching a certain query that created this
 * result set. Items that are not iterated are not evaluated.
 *
 * @param <T> type of configurable instances
 */
public abstract class ConfigResultSet<T> implements Iterable<T> {

    public abstract Iterator<T> iterator();

}
