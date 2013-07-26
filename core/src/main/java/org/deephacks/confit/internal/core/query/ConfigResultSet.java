package org.deephacks.confit.internal.core.query;

import org.deephacks.confit.spi.CacheManager;

import java.util.Iterator;


public class ConfigResultSet extends org.deephacks.confit.query.ConfigResultSet<Object> {
    private final com.googlecode.cqengine.resultset.ResultSet resultSet;
    private final CacheManager cacheManager;

    public ConfigResultSet(com.googlecode.cqengine.resultset.ResultSet resultSet, CacheManager cacheManager) {
        this.resultSet = resultSet;
        this.cacheManager = cacheManager;
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            Iterator<ConfigIndexFields> it = resultSet.iterator();
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                return cacheManager.get(it.next().getBeanId());
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }
}
