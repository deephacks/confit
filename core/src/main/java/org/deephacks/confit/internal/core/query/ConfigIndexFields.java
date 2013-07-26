package org.deephacks.confit.internal.core.query;


import com.google.common.base.Preconditions;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema.AbstractSchemaProperty;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.model.Schema.SchemaPropertyRefMap;
import org.deephacks.confit.spi.Conversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ConfigIndexFields contain field values for a particular schema and bean.
 */
public class ConfigIndexFields implements Comparable<ConfigIndexFields> {
    /** Bean id of this instance */
    public BeanId id;

    /** fieldName -> value of an indexed field */
    public HashMap<String, Object> fields = new HashMap<>();

    private static final Conversion conv = Conversion.get();

    /**
     * Use only when removing instances from an indexed collection!
     */
    public ConfigIndexFields(BeanId beanId) {
        this.id = beanId;
    }

    public ConfigIndexFields(Bean bean) {
        Preconditions.checkNotNull(bean.getSchema(), "Schema must be available on bean.");
        this.id = bean.getId();
        for (AbstractSchemaProperty prop : bean.getSchema().getIndexed()) {
            Object value = getValuesAsObject(prop, bean);
            if (value != null) {
                fields.put(prop.getName(), value);
            }
        }
    }

    private Object getValuesAsObject(final AbstractSchemaProperty property, final Bean bean) {
        String propertyName = property.getName();
        Object value = null;
        if (property instanceof SchemaProperty) {
            Class<?> cls = ((SchemaProperty) property).getClassType();
            List<String> values = bean.getValues(propertyName);
            if (values != null && values.size() > 0) {
                value = conv.convert(values.get(0), cls);
            }
        } else if (property instanceof SchemaPropertyList) {
            SchemaPropertyList list = (SchemaPropertyList) property;
            Class<?> cls = list.getClassType();
            List<String> values = bean.getValues(propertyName);
            if (values != null && values.size() > 0) {
                value = conv.convert(values, cls);
            }
        } else if (property instanceof SchemaPropertyRef) {
            List<BeanId> ids = bean.getReference(propertyName);
            if (ids != null && ids.size() > 0) {
                value = ids.get(0).getInstanceId();
            }
        } else if (property instanceof SchemaPropertyRefList ||
                property instanceof SchemaPropertyRefMap) {
            List<BeanId> ids = bean.getReference(propertyName);
            if (ids != null) {
                ArrayList<String> instanceIds = new ArrayList<>();
                for (BeanId id : ids) {
                    instanceIds.add(id.getInstanceId());
                }
                value = instanceIds;
            }
        } else {
            throw new IllegalArgumentException("Unrecognized property");
        }
        return value;
    }

    public BeanId getBeanId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigIndexFields that = (ConfigIndexFields) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public int compareTo(ConfigIndexFields o) {
        return 0;
    }
}
