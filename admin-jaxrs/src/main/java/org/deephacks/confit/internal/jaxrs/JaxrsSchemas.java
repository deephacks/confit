package org.deephacks.confit.internal.jaxrs;

import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JaxrsSchemas {
    private Collection<JaxrsSchema> schemas = new ArrayList<>();

    public void add(JaxrsSchema schema) {
        this.schemas.add(schema);
    }

    public void addAll(Collection<Schema> schemas) {
        for (Schema schema : schemas) {
            add(new JaxrsSchema(schema));
        }
    }

    public Collection<JaxrsSchema> getSchemas() {
        return schemas;
    }

    public Map<String, Schema> toSchema() {
        Map<String, Schema> result = new HashMap<>();
        for (JaxrsSchema schema : schemas) {
            result.put(schema.getSchemaName(), schema.toSchema());
        }
        return result;
    }

    public JaxrsSchemas() {

    }

    public static class JaxrsSchema {
        private String schemaName;
        private String className;
        private String desc;
        private String idName;
        private String idDesc;
        private boolean singleton;
        private List<String> propertyNames = new ArrayList<>();
        private List<SchemaProperty> property = new ArrayList<>();
        private List<SchemaPropertyList> propertyList = new ArrayList<>();
        private List<SchemaPropertyRef> propertyRef = new ArrayList<>();
        private List<SchemaPropertyRefList> propertyRefList = new ArrayList<>();
        private List<SchemaPropertyRefMap> propertyRefMap = new ArrayList<>();

        public JaxrsSchema() {

        }

        public JaxrsSchema(Schema schema) {
            this.schemaName = schema.getName();
            this.className = schema.getType();
            this.desc = schema.getDesc();
            this.idName = schema.getId().getName();
            this.idDesc = schema.getId().getDesc();
            this.singleton = schema.getId().isSingleton();
            for (Schema.SchemaProperty prop : schema.get(Schema.SchemaProperty.class)) {
                property.add(new SchemaProperty(prop));
                propertyNames.add(prop.getName());
            }

            for (Schema.SchemaPropertyList prop : schema.get(Schema.SchemaPropertyList.class)) {
                propertyList.add(new SchemaPropertyList(prop));
                propertyNames.add(prop.getName());
            }

            for (Schema.SchemaPropertyRef prop : schema.get(Schema.SchemaPropertyRef.class)) {
                propertyRef.add(new SchemaPropertyRef(prop));
                propertyNames.add(prop.getName());
            }

            for (Schema.SchemaPropertyRefList prop : schema.get(Schema.SchemaPropertyRefList.class)) {
                propertyRefList.add(new SchemaPropertyRefList(prop));
                propertyNames.add(prop.getName());
            }

            for (Schema.SchemaPropertyRefMap prop : schema.get(Schema.SchemaPropertyRefMap.class)) {
                propertyRefMap.add(new SchemaPropertyRefMap(prop));
                propertyNames.add(prop.getName());
            }
        }

        public String getIdName() {
            return idName;
        }

        public void setIdName(String idName) {
            this.idName = idName;
        }

        public String getIdDesc() {
            return idDesc;
        }

        public void setIdDesc(String idDesc) {
            this.idDesc = idDesc;
        }

        public List<String> getPropertyNames() {
            return propertyNames;
        }

        public void setPropertyNames(List<String> propertyNames) {
            this.propertyNames = propertyNames;
        }

        public List<SchemaProperty> getProperty() {
            return property;
        }

        public void setProperty(List<SchemaProperty> property) {
            this.property = property;
        }

        public List<SchemaPropertyList> getPropertyList() {
            return propertyList;
        }

        public void setPropertyList(List<SchemaPropertyList> propertyList) {
            this.propertyList = propertyList;
        }

        public List<SchemaPropertyRef> getPropertyRef() {
            return propertyRef;
        }

        public void setPropertyRef(List<SchemaPropertyRef> propertyRef) {
            this.propertyRef = propertyRef;
        }

        public List<SchemaPropertyRefList> getPropertyRefList() {
            return propertyRefList;
        }

        public void setPropertyRefList(List<SchemaPropertyRefList> propertyRefList) {
            this.propertyRefList = propertyRefList;
        }

        public List<SchemaPropertyRefMap> getPropertyRefMap() {
            return propertyRefMap;
        }

        public void setPropertyRefMap(List<SchemaPropertyRefMap> propertyRefMap) {
            this.propertyRefMap = propertyRefMap;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public void setSchemaName(String name) {
            this.schemaName = name;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String type) {
            this.className = type;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public boolean isSingleton() {
            return singleton;
        }

        public void setSingleton(boolean singleton) {
            this.singleton = singleton;
        }

        public Schema toSchema() {
            SchemaId id = SchemaId.create(idName, idDesc, singleton);
            Schema schema = Schema.create(id, className, schemaName, desc);

            for (SchemaProperty schemaProperty : property) {
                schema.add(schemaProperty.toSchema());
            }
            for (SchemaPropertyList schemaPropertyList : propertyList) {
                schema.add(schemaPropertyList.toSchema());
            }
            for (SchemaPropertyRef schemaPropertyRef : propertyRef) {
                schema.add(schemaPropertyRef.toSchema());
            }
            for (SchemaPropertyRefList schemaPropertyRefList : propertyRefList) {
                schema.add(schemaPropertyRefList.toSchema());
            }
            for (SchemaPropertyRefMap schemaPropertyRefMap : propertyRefMap) {
                schema.add(schemaPropertyRefMap.toSchema());
            }
            return schema;
        }

        public static class AbstractSchemaProperty {
            private String name;
            private String desc;
            private String fieldName;
            private boolean isImmutable;

            public AbstractSchemaProperty(){

            }
            public AbstractSchemaProperty(Schema.AbstractSchemaProperty schema) {
                this.name = schema.getName();
                this.desc = schema.getDesc();
                this.fieldName = schema.getFieldName();
                this.isImmutable = schema.isImmutable();
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getDesc() {
                return desc;
            }

            public void setDesc(String desc) {
                this.desc = desc;
            }

            public String getFieldName() {
                return fieldName;
            }

            public void setFieldName(String fieldName) {
                this.fieldName = fieldName;
            }

            public boolean isImmutable() {
                return isImmutable;
            }

            public void setImmutable(boolean isImmutable) {
                this.isImmutable = isImmutable;
            }

        }

        public final static class SchemaProperty extends AbstractSchemaProperty {
            private String defaultValue;
            private String type;
            private List<String> enums;

            public SchemaProperty(){

            }

            public SchemaProperty(Schema.SchemaProperty schema) {
                super(schema);
                this.defaultValue = schema.getDefaultValue();
                this.type = schema.getType();
                this.enums = schema.getEnums();
            }

            public String getDefaultValue() {
                return defaultValue;
            }

            public void setDefaultValue(String defaultValue) {
                this.defaultValue = defaultValue;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public List<String> getEnums() {
                return enums;
            }

            public void setEnums(List<String> enums) {
                this.enums = enums;
            }

            public Schema.SchemaProperty toSchema() {
                return Schema.SchemaProperty.create(getName(), getFieldName(), getType(), getDesc(), isImmutable(), getEnums(), getDefaultValue(), false);
            }
        }

        public final static class SchemaPropertyList extends AbstractSchemaProperty {
            private String type;
            private String collectionType;
            private List<String> defaultValues;
            private List<String> enums;

            public SchemaPropertyList(){

            }

            public SchemaPropertyList(Schema.SchemaPropertyList schema) {
                super(schema);
                this.type = schema.getType();
                this.collectionType = schema.getCollectionType();
                this.defaultValues = schema.getDefaultValues();
                this.enums = schema.getEnums();
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getCollectionType() {
                return collectionType;
            }

            public void setCollectionType(String collectionType) {
                this.collectionType = collectionType;
            }

            public List<String> getDefaultValues() {
                return defaultValues;
            }

            public void setDefaultValues(List<String> defaultValues) {
                this.defaultValues = defaultValues;
            }

            public List<String> getEnums() {
                return enums;
            }

            public void setEnums(List<String> enums) {
                this.enums = enums;
            }

            public Schema.AbstractSchemaProperty toSchema() {
                return Schema.SchemaPropertyList.create(getName(), getFieldName(), getType(), getDesc(), isImmutable(), getEnums(), getDefaultValues(), getCollectionType(), false);
            }
        }

        public final static class SchemaPropertyRef extends AbstractSchemaProperty {
            private String schemaName;
            private String classType;
            private boolean isSingleton;

            public SchemaPropertyRef(){

            }

            public SchemaPropertyRef(Schema.SchemaPropertyRef schema) {
                super(schema);
                this.schemaName = schema.getSchemaName();
                this.classType = schema.getClassType().getName();
                this.isSingleton = schema.isSingleton();
            }

            public String getSchemaName() {
                return schemaName;
            }

            public void setSchemaName(String schemaName) {
                this.schemaName = schemaName;
            }

            public boolean isSingleton() {
                return isSingleton;
            }

            public void setSingleton(boolean isSingleton) {
                this.isSingleton = isSingleton;
            }

            public Class<?> getClassType() {
                try {
                    return Class.forName(classType);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            public Schema.AbstractSchemaProperty toSchema() {
                return Schema.SchemaPropertyRef.create(getName(), getFieldName(), getSchemaName(), getClassType(), getDesc(), isImmutable(), isSingleton(), false);
            }
        }

        public final static class SchemaPropertyRefList extends AbstractSchemaProperty {
            private String collectionType;
            private String schemaName;
            private String classType;

            public SchemaPropertyRefList(){

            }

            public SchemaPropertyRefList(Schema.SchemaPropertyRefList schema) {
                super(schema);
                this.schemaName = schema.getSchemaName();
                this.classType = schema.getClassType().getName();
                this.collectionType = schema.getCollectionType();
            }

            public String getCollectionType() {
                return collectionType;
            }

            public void setCollectionType(String collectionType) {
                this.collectionType = collectionType;
            }

            public Class<?> getClassType() {
                try {
                    return Class.forName(classType);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            public String getSchemaName() {
                return schemaName;
            }

            public void setSchemaName(String schemaName) {
                this.schemaName = schemaName;
            }

            public Schema.AbstractSchemaProperty toSchema() {
                return Schema.SchemaPropertyRefList.create(getName(), getFieldName(), getSchemaName(), getClassType(), getDesc(), isImmutable(), getCollectionType(), false);
            }
        }

        public final static class SchemaPropertyRefMap extends AbstractSchemaProperty {
            private String mapType;
            private String schemaName;
            private String classType;

            public SchemaPropertyRefMap(){

            }

            public SchemaPropertyRefMap(Schema.SchemaPropertyRefMap schema) {
                super(schema);
                this.schemaName = schema.getSchemaName();
                this.classType = schema.getClassType().getName();
                this.mapType = schema.getMapType();
            }

            public String getMapType() {
                return mapType;
            }

            public void setMapType(String mapType) {
                this.mapType = mapType;
            }

            public String getSchemaName() {
                return schemaName;
            }

            public void setSchemaName(String schemaName) {
                this.schemaName = schemaName;
            }

            public Class<?> getClassType() {
                try {
                    return Class.forName(classType);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            public Schema.AbstractSchemaProperty toSchema() {
                return Schema.SchemaPropertyRefMap.create(getName(), getFieldName(), getSchemaName(), getClassType(), getDesc(), isImmutable(), getMapType(), false);
            }
        }
    }
}
