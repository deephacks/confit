package org.deephacks.confit.internal.jaxrs;

import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.admin.query.BeanQueryBuilder.BeanRestriction;
import org.deephacks.confit.admin.query.BeanQueryResult;

import java.util.ArrayList;
import java.util.List;

public class JaxrsQuery implements BeanQuery {

    private String schemaName;

    public JaxrsQuery() {

    }

    public JaxrsQuery(String query) {

    }

    public String getSchemaName() {
        return schemaName;
    }

    @Override
    public BeanQuery add(BeanRestriction restriction) {
        return null;
    }

    @Override
    public BeanQuery setFirstResult(String firstResult) {
        return null;
    }

    @Override
    public BeanQuery setMaxResults(int maxResults) {
        return null;
    }

    @Override
    public BeanQueryResult retrieve() {
        return null;
    }

    public List<BeanRestriction> getRestrictions() {
        ArrayList<BeanRestriction> restrictions = new ArrayList<>();
        return restrictions;
    }
}
