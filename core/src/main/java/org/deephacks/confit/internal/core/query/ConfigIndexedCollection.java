package org.deephacks.confit.internal.core.query;

import com.googlecode.cqengine.CQEngine;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;

import java.util.Iterator;

public class ConfigIndexedCollection<T> {
    final IndexedCollection<ConfigIndexFields> collection = CQEngine.newInstance();
    private final ConfigIndex index;

    public ConfigIndexedCollection(ConfigIndex index) {
        this.index = index;
        for(Attribute a : index.get()) {
            collection.addIndex(NavigableIndex.onAttribute(a));
        }
    }

    public void add(Bean bean) {
        ConfigIndexFields fields = new ConfigIndexFields(bean);
        collection.add(fields);
    }

    public void remove(BeanId beanId) {
        ConfigIndexFields id = new ConfigIndexFields(beanId);
        collection.remove(id);
    }

    Attribute getAttribute(String prop) {
        Attribute attr = index.get(prop);
        if(attr == null) {
            throw new IllegalArgumentException("Field ["+prop+"] is not indexed.");
        }
        return attr;
    }

    public ResultSet<ConfigIndexFields> retrieve(Query query) {
        return collection.retrieve(query);
    }

    public ResultSet<ConfigIndexFields> all() {
        return new ResultSet<ConfigIndexFields>() {
            @Override
            public Iterator<ConfigIndexFields> iterator() {
                return collection.iterator();
            }

            @Override
            public boolean contains(ConfigIndexFields object) {
                return collection.contains(object);
            }

            @Override
            public int getRetrievalCost() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getMergeCost() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int size() {
                return collection.size();
            }
        };
    }


}
