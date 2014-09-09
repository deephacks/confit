/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.confit.model;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Objects.equal;

/**
 * <p>
 * Schema is used to provide information to administrative users in order to explain
 * its purpose and how configuration changes may affect system behaviour.
 * </p>
 * <p>
 * Schemas are handled by the framework and are not intended for spontaneous creation or
 * modification by any user. Schemas originate from when a configurable class is registered
 * in the system and are considered read-only entities.
 * </p>
 *
 * @author Kristoffer Sjogren
 */
public final class Schema implements Serializable {
    private static final long serialVersionUID = -2914939410489202548L;
    private SchemaId id;
    private final String name;
    private final String type;
    private final String description;
    private Multimap<Class<? extends AbstractSchemaProperty>, AbstractSchemaProperty> properties = HashMultimap
            .create();
    private transient Set<String> referenceNames = new HashSet<>();
    private transient Set<String> propertyNames = new HashSet<>();

    private Schema(final SchemaId id, final String type, final String name, final String description) {
        this.id = Preconditions.checkNotNull(id);
        this.type = Preconditions.checkNotNull(type);
        this.description = Preconditions.checkNotNull(description);
        this.name = Preconditions.checkNotNull(name);
    }

    /**
     * Creates a new schema. Not to be used by users, schemas are created when a configurable class are
     * registered in the system.
     *
     * @param id that identify this schema.
     * @param classType classname that fully qualifies the configurable class that this schema originates from.
     * @param name of this schema as specified in meta data, names must be unique.
     * @param description purpose and useful information needed in order to manage this schema.

     * @return A Schema.
     */
    public static Schema create(SchemaId id, final String classType, final String name,
            final String description) {
        return new Schema(id, classType, name, description);
    }

    /**
     * Identification for this this schema. This id must be unqiue in the system.
     *
     * @return id for this schema.
     */
    public SchemaId getId() {
        return id;
    }

    /**
     * This is the fully qualified classname of the configurable class that this schema originates from.
     *
     * Do not display this property to end users, use the 'name' property instead .
     *
     * @return A full class name.
     */
    public final String getType() {
        return type;
    }

    /**
     * This is the fully qualified classname of the configurable class that this schema originates from.
     *
     * @return Class of this schema.
     */
    public Class<?> getClassType() {
        try {
            return Class.forName(getType(), true, ClassLoaderHolder.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Description that justify the existence of the schema by putting it into a
     * high-level context and describe how it relates to system concepts.
     *
     * @return description
     */
    public final String getDesc() {
        return description;
    }

    /**
     * A unique name that identifies the schema in the system. Good names are those
     * which describe domain specific aspects established in the system architecture.
     *
     * @return name
     */
    public final String getName() {
        return name;
    }

    /**
     * Adds a property of a specific type to this schema. Not to be used by users.
     *
     * @param property property
     */
    public void add(AbstractSchemaProperty property) {
        Class type = property.getClass();
        properties.put(type, property);
        if (SchemaProperty.class.isAssignableFrom(type)) {
            propertyNames.add(property.getName());
        } else if (SchemaPropertyList.class.isAssignableFrom(type)) {
            propertyNames.add(property.getName());
        } else if (SchemaPropertyRef.class.isAssignableFrom(type)) {
            referenceNames.add(property.getName());
        } else if (SchemaPropertyRefList.class.isAssignableFrom(type)) {
            referenceNames.add(property.getName());
        } else if (SchemaPropertyRefMap.class.isAssignableFrom(type)) {
            referenceNames.add(property.getName());
        } else {
            throw new IllegalArgumentException("Unknown property type " + type.getName());
        }
    }

    /**
     * Returns all properties that have been marked as indexed.
     *
     * @param <T> type
     * @return a list of properties.
     */
    public <T extends AbstractSchemaProperty> Set<T> getIndexed() {
        HashSet<AbstractSchemaProperty> indexed = new HashSet<>();
        for (AbstractSchemaProperty prop : properties.values()) {
            if (prop.isIndexed()) indexed.add(prop);
        }
        return (Set<T>) indexed;
    }

    /**
     * Returns all the properties of a particular type.
     *
     * @param clazz The specific type of properties to get.
     * @return A list of properties that matches the clazz.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends AbstractSchemaProperty> Set<T> get(final Class<T> clazz) {
        return (Set<T>) properties.get(clazz);
    }

    public String getReferenceSchemaName(String propertyName) {
        for (AbstractSchemaProperty schema : properties.values()) {
            if (!schema.getName().equals(propertyName)) {
                if (!schema.getFieldName().equals(propertyName)) {
                    continue;
                }
            }
            if (schema instanceof SchemaPropertyRef) {
                return ((SchemaPropertyRef) schema).getSchemaName();
            }
            if (schema instanceof SchemaPropertyRefList) {
                return ((SchemaPropertyRefList) schema).getSchemaName();
            }
            if (schema instanceof SchemaPropertyRefMap) {
                return ((SchemaPropertyRefMap) schema).getSchemaName();
            }
        }
        return null;
    }

    /**
     * Returns a specific properties of a particular type identified with a name.
     *
     * @param clazz specific type of property to get.
     * @param name The AbstractSchemaProperty name of the property.
     * @return Matching property.
     */
    public <T extends AbstractSchemaProperty> T get(final Class<T> clazz, final String name) {
        Set<T> propertyCollection = get(clazz);
        for (T property : propertyCollection) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    /**
     * Returns all property names that exist for this schema.
     */
    public Set<String> getPropertyNames() {
        return propertyNames;
    }

    /**
     * Returns all reference names that exist for this schema.
     */
    public Set<String> getReferenceNames() {
        return referenceNames;
    }

    public Set<Class<?>> getReferenceSchemaTypes() {
        Set<Class<?>> names = new HashSet<>();
        for (AbstractSchemaProperty prop : get(SchemaPropertyRef.class)) {
            names.add(prop.getClassType());
        }
        for (AbstractSchemaProperty prop : get(SchemaPropertyRefList.class)) {
            names.add(prop.getClassType());
        }
        for (AbstractSchemaProperty prop : get(SchemaPropertyRefMap.class)) {
            names.add(prop.getClassType());
        }
        return names;
    }

    public String toString() {
        return Objects.toStringHelper(Schema.class).add("id", id).add("name", getName())
                .add("type", getType()).add("desc", getDesc()).add("properties", properties)
                .toString();

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties, getType(), getDesc(), getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Schema)) {
            return false;
        }
        Schema o = (Schema) obj;
        return equal(getName(), o.getName()) && equal(getType(), o.getType());
    }

    public boolean isReference(String property) {
        return referenceNames.contains(property);
    }

    public boolean isProperty(String property) {
        return propertyNames.contains(property);
    }

    /**
     * Description of the identification of a a particular schema registered
     * in the system.
     */
    public static class SchemaId implements Serializable {
        private static final long serialVersionUID = 5803256931889425514L;
        private final String name;
        private final String desc;
        private boolean isSingleton;

        private SchemaId(final String name, final String desc, final boolean isSingleton) {
            this.name = Preconditions.checkNotNull(name);
            this.desc = Preconditions.checkNotNull(desc);
            this.isSingleton = isSingleton;
        }

        public static SchemaId create(final String name, final String desc,
                final boolean isSingleton) {
            return new SchemaId(name, desc, isSingleton);
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        public boolean isSingleton() {
            return isSingleton;
        }

        public String toString() {
            return Objects.toStringHelper(SchemaId.class).add("id", getName())
                    .add("desc", getDesc()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getName(), getDesc());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SchemaId)) {
                return false;
            }
            SchemaId o = (SchemaId) obj;
            return equal(getName(), o.getName()) && equal(getDesc(), o.getDesc());
        }
    }

    /**
     * Abstract base class for schema properties.
     */
    public static class AbstractSchemaProperty implements Serializable {
        private static final long serialVersionUID = -3057627766413883885L;
        private final String name;
        private final Class<?> type;
        private final String desc;
        private final String fieldName;
        private final boolean isImmutable;
        private final boolean indexed;

        protected AbstractSchemaProperty(final String name, Class<?> type, final String fieldName,
                final String desc, final boolean isimmutable, final boolean indexed) {
            this.name = Preconditions.checkNotNull(name);
            this.type = type;
            this.fieldName = Preconditions.checkNotNull(fieldName);
            this.desc = Preconditions.checkNotNull(desc);
            this.isImmutable = isimmutable;
            this.indexed = indexed;
        }

        public boolean isImmutable() {
            return isImmutable;
        }

        public boolean isIndexed() {
            return indexed;
        }

        public String getName() {
            return this.name;
        }

        public String getFieldName() {
            return this.fieldName;
        }

        public String getDesc() {
            return this.desc;
        }

        public Class<?> getClassType() {
            return type;
        }

        ToStringHelper toStringHelper(Class<?> clazz) {
            return Objects.toStringHelper(clazz).add("name", name).add("fieldName", fieldName)
                    .add("desc", desc).add("immutable", isImmutable);
        }

        int getHashCode() {
            return Objects.hashCode(getName(), getFieldName(), getDesc(), isImmutable());
        }

        boolean equals(AbstractSchemaProperty o) {
            return equal(getName(), o.getName()) && equal(getFieldName(), o.getFieldName())
                    && equal(getDesc(), o.getDesc()) && equal(isImmutable(), o.isImmutable());
        }
    }

    /**
     * Description of a single simple type.
     */
    public final static class SchemaProperty extends AbstractSchemaProperty {
        private static final long serialVersionUID = -8108590860088240249L;
        private String defaultValue;
        private String type;
        private List<String> enums;

        private SchemaProperty(final String name, final String fieldName, final String type,
                final String desc, final boolean isImmutable, final List<String> enums,
                final String defaultValue, final boolean indexed) {
            super(name, getClassTypeFromName(type), fieldName, desc, isImmutable, indexed);
            this.defaultValue = defaultValue;
            this.enums = enums;
            this.type = Preconditions.checkNotNull(type);
        }

        /**
         * Not to be used by users.
         */
        public static SchemaProperty create(final String name, final String fieldName,
                final String type, final String desc, final boolean isImmutable,
                final List<String> enums, final String defaultValue, final boolean indexed) {

            return new SchemaProperty(name, fieldName, type, desc, isImmutable, enums, defaultValue, indexed);
        }

        public String getType() {
            return type;
        }

        public boolean isEnum() {
            return enums != null && enums.size() > 0;
        }

        public List<String> getEnums() {
            return enums;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String toString() {
            return super.toStringHelper(SchemaProperty.class).add("type", getType())
                    .add("defaultValue", getDefaultValue()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(super.getHashCode(), getDefaultValue(), getType());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SchemaProperty)) {
                return false;
            }
            SchemaProperty o = (SchemaProperty) obj;
            return super.equals(o) && equal(getDefaultValue(), o.getDefaultValue())
                    && equal(getType(), o.getType());
        }
    }

    /**
     * Description of a collection (or any other subtype) of simple types.
     */
    public final static class SchemaPropertyList extends AbstractSchemaProperty {
        private static final long serialVersionUID = 3192273741446945936L;
        private final String type;
        private final String collectionType;
        private List<String> defaultValues;
        private List<String> enums;
        private Class<?> cls;
        private Class<?> collectionTypeCls;

        private SchemaPropertyList(final String name, final String fieldName, final String type,
                final String desc, final boolean isImmutable, final List<String> enums,
                final String collectionType, final List<String> defaultValues, final boolean indexed) {
            super(name, getClassTypeFromName(type), fieldName, desc, isImmutable, indexed);
            this.collectionType = Preconditions.checkNotNull(collectionType);
            this.type = Preconditions.checkNotNull(type);
            this.defaultValues = defaultValues;
            this.enums = enums;
        }

        /**
         * Not to be used by users.
         */
        public static SchemaPropertyList create(final String name, final String fieldName,
                final String type, final String desc, boolean isImmutable,
                final List<String> enums, List<String> defaultValues, final String collectionType, final boolean indexed) {
            return new SchemaPropertyList(name, fieldName, type, desc, isImmutable, enums,
                    collectionType, defaultValues, indexed);
        }

        public String getType() {
            return type;
        }

        public String getCollectionType() {
            return collectionType;
        }

        public Class<?> getClassCollectionType() {
            if(collectionTypeCls != null) {
                return collectionTypeCls;
            }
            try {
                collectionTypeCls = Class.forName(collectionType, true, ClassLoaderHolder.getClassLoader());
                return collectionTypeCls;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        public Class<?> getClassType() {
            if(cls != null) {
                return cls;
            }
            try {
                cls = Class.forName(type, true, ClassLoaderHolder.getClassLoader());
                return cls;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        public List<String> getDefaultValues() {
            return defaultValues;
        }

        public boolean isEnum() {
            return enums != null && enums.size() > 0;
        }

        public List<String> getEnums() {
            return enums;
        }

        public String toString() {
            return Objects.toStringHelper(SchemaPropertyList.class)
                    .add("type", getCollectionType()).add("collectionType", getCollectionType())
                    .add("defaultValue", getDefaultValues()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(super.getHashCode(), getType(), getCollectionType());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SchemaPropertyList)) {
                return false;
            }
            SchemaPropertyList o = (SchemaPropertyList) obj;
            return equals(o) && equal(getCollectionType(), o.getCollectionType())
                    && equal(getType(), o.getType());
        }
    }

    /**
     * Description of a single reference to any other bean registered in the system.
     */
    public final static class SchemaPropertyRef extends AbstractSchemaProperty {
        private final String schemaName;
        private boolean isSingleton;
        private static final long serialVersionUID = 987642987178370676L;

        protected SchemaPropertyRef(final String name, final String fieldName,
                final String schemaName, Class<?> classType, final String desc, final boolean isImmutable,
                final boolean isSingleton, final boolean indexed) {
            super(name, classType, fieldName, desc, isImmutable, indexed);
            this.isSingleton = isSingleton;
            this.schemaName = Preconditions.checkNotNull(schemaName);
        }

        /**
         * Not to be used by users.
         */
        public static SchemaPropertyRef create(final String name, final String fieldName,
                final String schemaName, final Class<?> classType, final String desc, final boolean isImmutable,
                final boolean isSingleton, final boolean indexed) {
            return new SchemaPropertyRef(name, fieldName, schemaName, classType, desc, isImmutable,
                    isSingleton, indexed);
        }

        public final String getSchemaName() {
            return schemaName;
        }

        public boolean isSingleton() {
            return isSingleton;
        }

        @Override
        public int hashCode() {
            return super.getHashCode() + Objects.hashCode(getSchemaName());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SchemaPropertyRef)) {
                return false;
            }
            SchemaPropertyRef o = (SchemaPropertyRef) obj;
            return equals(o) && equal(getSchemaName(), o.getSchemaName());
        }

        @Override
        public final String toString() {
            return super.toStringHelper(SchemaPropertyRef.class).add("schema-name", schemaName)
                    .toString();
        }
    }

    /**
     * Description of a collection (or any other subtype) of references to any other
     * bean defined in the system.
     */
    public final static class SchemaPropertyRefList extends AbstractSchemaProperty {

        private static final long serialVersionUID = -2386455434996679436L;
        private final String collectionType;
        private final String schemaName;
        private Class<?> collectionTypeCls;

        private SchemaPropertyRefList(final String name, final String fieldName,
                final String schemaName, final Class<?> classType, final String desc, final boolean isImmutable,
                final String collectionType, final boolean indexed) {
            super(name, classType, fieldName, desc, isImmutable, indexed);
            this.collectionType = Preconditions.checkNotNull(collectionType);
            this.schemaName = Preconditions.checkNotNull(schemaName);
        }

        /**
         * Not to be used by users.
         */
        public static SchemaPropertyRefList create(final String name, final String fieldName,
                final String schemaName, final Class<?> classType, final String desc, final boolean isImmutable,
                final String collectionType, final boolean indexed) {
            return new SchemaPropertyRefList(name, fieldName, schemaName, classType, desc, isImmutable,
                    collectionType, indexed);
        }

        public final String getCollectionType() {
            return collectionType;
        }

        public Class<?> getClassCollectionType() {
            if(collectionTypeCls != null) {
                return collectionTypeCls;
            }
            try {
                collectionTypeCls = Class.forName(collectionType, true, ClassLoaderHolder.getClassLoader());
                return collectionTypeCls;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        public final String getSchemaName() {
            return schemaName;
        }

        @Override
        public int hashCode() {
            return super.getHashCode() + Objects.hashCode(collectionType, schemaName);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SchemaPropertyRefList)) {
                return false;
            }
            SchemaPropertyRefList o = (SchemaPropertyRefList) obj;
            return equals(o) && equal(getCollectionType(), o.getCollectionType())
                    && equal(getSchemaName(), o.getSchemaName());
        }

        @Override
        public final String toString() {
            return toStringHelper(SchemaPropertyRefList.class)
                    .add("collectionType", getCollectionType()).add("schema-name", getSchemaName())
                    .toString();
        }
    }

    /**
     * Represent a map of references indexed on instance id.
     */
    public final static class SchemaPropertyRefMap extends AbstractSchemaProperty {

        private static final long serialVersionUID = 9128725908670921628L;
        private final String mapType;
        private final String schemaName;
        private Class<?> collectionTypeCls;

        private SchemaPropertyRefMap(final String name, final String fieldName,
                final String schemaName, final Class<?> classType, final String desc, final boolean isImmutable,
                final String mapType, final boolean indexed) {
            super(name, classType, fieldName, desc, isImmutable, indexed);
            this.mapType = Preconditions.checkNotNull(mapType);
            this.schemaName = Preconditions.checkNotNull(schemaName);
        }

        /**
         * Not to be used by users.
         */
        public static SchemaPropertyRefMap create(final String name, final String fieldName,
                final String schemaName, final Class<?> classType, final String desc, final boolean isImmutable,
                final String mapType, final boolean indexed) {
            return new SchemaPropertyRefMap(name, fieldName, schemaName, classType, desc, isImmutable, mapType, indexed);
        }

        public String getMapType() {
            return mapType;
        }

        public Class<?> getClassMapType() {
            if(collectionTypeCls != null) {
                return collectionTypeCls;
            }
            collectionTypeCls = getClassTypeFromName(mapType);
            return collectionTypeCls;
        }

        public String getSchemaName() {
            return schemaName;
        }

        @Override
        public int hashCode() {
            return super.getHashCode() + Objects.hashCode(mapType, schemaName);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SchemaPropertyRefMap)) {
                return false;
            }
            SchemaPropertyRefMap o = (SchemaPropertyRefMap) obj;
            return equals(o) && equal(getMapType(), o.getMapType())
                    && equal(getSchemaName(), o.getSchemaName());
        }

        @Override
        public String toString() {
            return toStringHelper(SchemaPropertyRefList.class).add("mapType", getMapType())
                    .add("schema-name", getSchemaName()).toString();
        }

    }
    private static final Map<String, Class<?>> PRIMITIVE_TYPES = new HashMap<>();
    static {
        for (Class<?> primitive : Arrays.asList(char.class, boolean.class, byte.class, short.class,
                int.class, long.class, float.class, double.class)) {
            PRIMITIVE_TYPES.put(primitive.getName(), primitive);
        }
    }
    public static Class<?> getClassTypeFromName(String name) {
        try {
            Class<?> primitive = PRIMITIVE_TYPES.get(name);
            return  primitive != null ? primitive : Class.forName(name, true, ClassLoaderHolder.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}