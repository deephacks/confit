package org.deephacks.confit.internal.cached.query;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.MultiValueNullableAttribute;
import com.googlecode.cqengine.attribute.SimpleNullableAttribute;
import org.deephacks.confit.Index;
import org.deephacks.confit.internal.core.Reflections;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.AbstractSchemaProperty;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.model.Schema.SchemaPropertyRefMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigIndex {
    private Class<?> cls;
    private HashMap<String, Attribute<?, ?>> attributes = new HashMap<>();

    public ConfigIndex(Schema schema) {
        for(AbstractSchemaProperty property : schema.getIndexed()) {
            String propertyName = property.getName();
            Class<?> type = getType(property);
            if(Number.class.isAssignableFrom(type) || Reflections.isPrimitiveNumber(type)) {
                NumberAttribute attr = new NumberAttribute(propertyName);
                attributes.put(propertyName, attr);
            } else if (Collection.class.isAssignableFrom(type)) {
                MultiObjectAttribute attr = new MultiObjectAttribute(propertyName);
                attributes.put(propertyName, attr);
            } else if (Map.class.isAssignableFrom(type)) {
                MultiObjectAttribute attr = new MultiObjectAttribute(propertyName);
                attributes.put(propertyName, attr);
            } else {
                ObjectAttribute attr = new ObjectAttribute(propertyName);
                attributes.put(propertyName, attr);
            }
        }
    }
    private Class<?> getType(final AbstractSchemaProperty property) {
        if (property instanceof SchemaProperty) {
            return ((SchemaProperty) property).getClassType();
        } else if (property instanceof SchemaPropertyList) {
            return  ((SchemaPropertyList) property).getClassCollectionType();
        } else if (property instanceof SchemaPropertyRef) {
            return String.class;
        } else if (property instanceof SchemaPropertyRefList ||
                property instanceof SchemaPropertyRefMap) {
            return ArrayList.class;
        } else {
            throw new IllegalArgumentException("Unrecognized property");
        }
    }

    public Attribute get(String field) {
        return attributes.get(field);
    }

    public List<Attribute> get() {
        List<Attribute> attrs = new ArrayList<>();
        for(Attribute att : attributes.values()){
            attrs.add(att);
        }
        return attrs;
    }

    private List<Field> getIndexedFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        for(final Field field : cls.getDeclaredFields()) {
            field.setAccessible(true);
            if(field.getAnnotation(Index.class) != null) {
                fields.add(field);
            }
        }
        return fields;
    }

    static final class ObjectAttribute extends SimpleNullableAttribute<ConfigIndexFields, Object> {
        private final String field;

        public ObjectAttribute(String field) {
            this.field = field;
        }

        @Override
        public Object getValue(ConfigIndexFields data) {
            return data.fields.get(field);
        }
    }

    static final class MultiObjectAttribute extends MultiValueNullableAttribute<ConfigIndexFields, Object> {
        private final String field;

        public MultiObjectAttribute(String field) {
            super(field, false);
            this.field = field;
        }

        @Override
        public List<Object> getNullableValues(ConfigIndexFields data) {
            Object o = data.fields.get(field);
            if(o == null) {
                return null;
            }
            Class<?> cls = o.getClass();
            if(Collection.class.isAssignableFrom(cls)) {
                return (List<Object>) o;
            } else if(Map.class.isAssignableFrom(cls)) {
                ArrayList<Object> values = new ArrayList<>();
                for(Object value : ((Map)o).values()) {
                    values.add(value);
                }
                return values;
            } else {
                throw new IllegalArgumentException("Could not get correct collection from ["+o+"]");
            }
        }
    }

    static final class NumberAttribute extends SimpleNullableAttribute<ConfigIndexFields, Number> {
        private final String field;

        public NumberAttribute(String field) {
            this.field = field;
        }

        @Override
        public Number getValue(ConfigIndexFields data) {
            return (Number) data.fields.get(field);
        }
    }

}