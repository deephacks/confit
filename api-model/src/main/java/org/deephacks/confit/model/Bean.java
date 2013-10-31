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
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.model.Schema.SchemaPropertyRefMap;
import org.deephacks.confit.serialization.Conversion;
import org.deephacks.confit.serialization.UniqueIds;
import org.deephacks.confit.serialization.ValueSerialization.ValueReader;
import org.deephacks.confit.serialization.ValueSerialization.ValueWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Objects.equal;
import static org.deephacks.confit.model.Events.CFG310_CIRCULAR_REF;

/**
 * <p>
 * Bean represent an instance of a configurable bean. Every bean
 * that are to be provisioned must first have its corresponding schema
 * registered in the system.
 * </p>
 * <p>
 * Bean only serve as a simple transfer object and should not be
 * concerned about schema specific thing.  It should not try to
 * understand or validate if specific values are correct.
 * </p>
 * <p>
 * Bean only know about two kinds of values, properties and references. All
 * values are treated as a list of plain strings and do not care if certain
 * properties in reality are single valued.
 * </p>
 * @author Kristoffer Sjogren
 */
public final class Bean {
    private static final Conversion conversion = Conversion.get();
    /** lazy, only needed for binary serialization */
    private static UniqueIds ids;
    private final BeanId id;
    private final HashMap<String, List<String>> properties = new HashMap<>();
    private final HashMap<String, List<BeanId>> references = new HashMap<>();

    private Bean(BeanId id) {
        Preconditions.checkNotNull(id);
        // make a defensive copy
        if (id.isSingleton()) {
            this.id = BeanId.createSingleton(id.getSchemaName());
        } else {
            this.id = BeanId.create(id.getInstanceId(), id.getSchemaName());
        }
    }

    /**
     * Create a admin bean instance.
     *
     * @param id unique identification of the bean instance.
     * @return AdminBean
     */
    public static Bean create(final BeanId id) {
        Preconditions.checkNotNull(id);
        Bean bean = new Bean(id);
        bean.set(id.getSchema());
        return bean;
    }

    /**
     * @return unique identification of the bean instance.
     */
    public BeanId getId() {
        return id;
    }

    /**
     * Schema will only be present if the Bean was fetched or given from
     * a managed administrative context.
     *
     * @return the schema that belong to the bean instance.
     */
    public Schema getSchema() {
        return id.getSchema();
    }

    /**
     * Set the schema that define content structure and constraints of this
     * the bean instance.
     *
     * @param schema schema
     */
    public void set(final Schema schema) {
        this.id.set(schema);
    }

    /**
     * Return the list of property names which have values. Properties
     * with default values are not returned.
     *
     * @return list of property names.
     */
    public List<String> getPropertyNames() {
        ArrayList<String> names = new ArrayList<>(properties.keySet());
        Collections.sort(names);
        return names;
    }

    /**
     * Return the list of property names which are references.
     *
     * @return list of property names.
     */
    public List<String> getReferenceNames() {
        ArrayList<String> names = new ArrayList<>(references.keySet());
        Collections.sort(names);
        return names;
    }

    /**
     * Add a list of value to a property on this bean.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @param values final String representations of the parameterized type of the collection
     * that conforms to its type as defined by the bean's schema.
     */
    public void addProperty(final String propertyName, final Collection<String> values) {
        Preconditions.checkNotNull(values);
        Preconditions.checkNotNull(propertyName);
        List<String> list = properties.get(propertyName);
        if (list == null) {
            properties.put(propertyName, new ArrayList<>(values));
        } else {
            list.addAll(values);
        }
    }

    /**
     * Add a value to a property on this bean.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @param value final String representations of the property that conforms to
     * its type as defined by the bean's schema.
     */
    public void addProperty(final String propertyName, final String value) {
        Preconditions.checkNotNull(propertyName);
        Preconditions.checkNotNull(value);
        List<String> values = properties.get(propertyName);
        if (values == null) {
            values = new ArrayList<>();
            values.add(value);
            properties.put(propertyName, values);
        } else {
            values.add(value);
        }
    }

    /**
     * Overwrite the current values with the provided value.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @param value final String representations of the property that conforms to
     * its type as defined by the bean's schema.
     */
    public void setProperty(final String propertyName, final String value) {
        Preconditions.checkNotNull(propertyName);
        if (value == null) {
            properties.put(propertyName, null);
            return;
        }
        List<String> values = new ArrayList<>();
        values.add(value);
        properties.put(propertyName, values);
    }

    /**
     * Overwrite/replace the current values with the provided values.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @param values final String representations of the property that conforms to
     * its type as defined by the bean's schema.
     */
    public void setProperty(final String propertyName, final List<String> values) {
        Preconditions.checkNotNull(propertyName);
        if (values == null) {
            properties.put(propertyName, null);
            return;
        }
        properties.put(propertyName, values);
    }

    /**
     * Clear the values of a property or reference, setting it to null.
     *
     * This operation can be useful when removing properties using "merge"
     * operation.
     *
     * @param propertyName of the property as defined by the bean's schema.
     */
    public void clear(final String propertyName) {
        Preconditions.checkNotNull(propertyName);
        if (properties.containsKey(propertyName)) {
            properties.put(propertyName, null);
        } else if (references.containsKey(propertyName)) {
            references.put(propertyName, null);
        }
    }

    /**
     * Remove a property or reference as a property from this bean.
     *
     * This operation can be useful when removing properties using "set"
     * operation.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     */
    public void remove(final String propertyName) {
        Preconditions.checkNotNull(propertyName);
        if (properties.containsKey(propertyName)) {
            properties.remove(propertyName);
        } else if (references.containsKey(propertyName)) {
            references.remove(propertyName);
        }
    }

    /**
     * Get the values of a property on a bean.
     *
     * @param propertyName of the property as defined by the bean's schema.
     * @return final String representations of the property that conforms to
     * its type as defined by the bean's schema.
     */
    public List<String> getValues(final String propertyName) {
        Preconditions.checkNotNull(propertyName);
        List<String> values = properties.get(propertyName);
        if (values == null) {
            return null;
        }
        // creates a shallow defensive copy
        return new ArrayList<>(values);
    }

    /**
     * A helper method for getting the value of single valued properties. Returns
     * null if the property does not exist.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @return final String representations of the property that conforms to
     * its type as defined by the bean's schema.
     */
    public String getSingleValue(final String propertyName) {
        Preconditions.checkNotNull(propertyName);
        List<String> values = getValues(propertyName);
        if (values == null || values.size() < 1) {
            return null;
        }
        return values.get(0);
    }

    /**
     * Add a list of references to a property on this bean.
     *
     * A reference identify other beans based on schema and instance id.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @param refs the reference as defined by the bean's schema.
     */
    public void addReference(final String propertyName, final Collection<BeanId> refs) {
        Preconditions.checkNotNull(refs);
        Preconditions.checkNotNull(propertyName);
        checkCircularReference(refs.toArray(new BeanId[refs.size()]));
        List<BeanId> list = references.get(propertyName);
        if (list == null) {
            list = new ArrayList<>();
            list.addAll(refs);
            references.put(propertyName, list);
        } else {
            list.addAll(refs);
        }
    }

    /**
     * Check that references does not point to self.
     * @param references from
     */
    private void checkCircularReference(final BeanId... references) {
        for (BeanId beanId : references) {
            if (getId().equals(beanId)) {
                throw CFG310_CIRCULAR_REF(getId(), getId());
            }
        }
    }

    /**
     * Add a reference to a property on this bean.
     *
     * A reference identify other beans based on schema and instance id.
     *
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @param ref the reference as defined by the bean's schema.
     */
    public void addReference(final String propertyName, final BeanId ref) {
        Preconditions.checkNotNull(ref);
        Preconditions.checkNotNull(propertyName);
        checkCircularReference(ref);
        List<BeanId> list = references.get(propertyName);
        if (list == null) {
            list = new ArrayList<>();
            list.add(ref);
            references.put(propertyName, list);
        } else {
            list.add(ref);
        }
    }

    /**
     * Get the references of a property on a bean.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @return References that identify other beans.
     */
    public List<BeanId> getReference(final String propertyName) {
        List<BeanId> values = references.get(propertyName);
        if (values == null) {
            return null;
        }
        return values;
    }

    /**
     * Get all references for all properties of this bean.
     *
     * @return References that identify other beans.
     */
    public List<BeanId> getReferences() {
        if (references == null) {
            return new ArrayList<>();
        }
        ArrayList<BeanId> result = new ArrayList<>();
        for (List<BeanId> b : references.values()) {
            if (b != null) {
                result.addAll(b);
            }

        }
        return result;
    }

    /**
     * Overwrite/replace the current references with the provided reference.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @param values override
     */
    public void setReferences(final String propertyName, final List<BeanId> values) {
        Preconditions.checkNotNull(propertyName);
        if (values == null || values.size() == 0) {
            references.put(propertyName, null);
            return;
        }
        checkCircularReference(values.toArray(new BeanId[values.size()]));
        references.put(propertyName, values);
    }

    /**
     * Overwrite/replace the current references with a provided reference.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @param value override
     */
    public void setReference(final String propertyName, final BeanId value) {
        Preconditions.checkNotNull(propertyName);
        if (value == null) {
            references.put(propertyName, null);
            return;
        }
        checkCircularReference(value);
        List<BeanId> values = new ArrayList<>();
        values.add(value);
        references.put(propertyName, values);
    }

    /**
     * A helper method for getting the value of single referenced property. Returns
     * null if the refrences does not exist.
     *
     * @param propertyName name of the property as defined by the bean's schema.
     * @return the value.
     */
    public BeanId getFirstReference(final String propertyName) {
        List<BeanId> refrences = getReference(propertyName);
        if (refrences == null || refrences.size() < 1) {
            return null;
        }
        return refrences.get(0);
    }

    /**
     * Clears this bean from all properties and references.
     */
    public void clear() {
        properties.clear();
        references.clear();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Bean)) {
            return false;
        }
        Bean other = (Bean) obj;
        return equal(getId(), other.getId());
    }

    @Override
    public final String toString() {
        return Objects.toStringHelper(Bean.class).add("id", id).add("schema", getSchema())
                .add("properties", properties).add("references", references).toString();
    }

    public static Collection<Bean> copy(Collection<Bean> beans) {
        Collection<Bean> copies = new ArrayList<>();
        for (Bean bean : beans) {
            copies.add(copy(bean));
        }
        return copies;
    }

    public static Bean copy(Bean bean) {
        if (bean == null) {
            return null;
        }
        Bean copy = Bean.create(bean.getId());
        for (String property : bean.getPropertyNames()) {
            Collection<String> values = bean.getValues(property);
            if (values == null) {
                continue;
            }
            copy.setProperty(property, bean.getValues(property));
        }
        for (String property : bean.getReferenceNames()) {
            List<BeanId> ids = bean.getReference(property);
            if (ids == null) {
                continue;
            }
            for (BeanId id : ids) {
                copy.addReference(property, BeanId.create(id.getInstanceId(), id.getSchemaName()));
            }
        }
        return copy;
    }

    public byte[] write() {
        if (ids == null) {
            ids = UniqueIds.lookup();
        }
        Schema schema = getSchema();
        Preconditions.checkNotNull(schema);
        ValueWriter writer = new ValueWriter();

        for (SchemaProperty property : schema.get(SchemaProperty.class)) {
            int propId = ids.getSchemaId(property.getFieldName());
            Object value;
            if(writer.isBasicType(property.getClassType())) {
                value = conversion.convert(getSingleValue(property.getFieldName()), property.getClassType());
            } else {
                value = getSingleValue(property.getFieldName());
            }
            writer.putValue(propId, value);
        }
        for (SchemaPropertyList property : schema.get(SchemaPropertyList.class)) {
            int propId = ids.getSchemaId(property.getFieldName());
            Collection<?> values;
            if(writer.isBasicType(property.getClassType())) {
                values = conversion.convert(getValues(property.getFieldName()), property.getClassType());
                writer.putValues(propId, values, property.getClassType());
            } else {
                values = getValues(property.getFieldName());
                writer.putValues(propId, values, String.class);
            }
        }
        for (SchemaPropertyRef property : schema.get(SchemaPropertyRef.class)) {
            int propId = ids.getSchemaId(property.getFieldName());
            BeanId beanId = getFirstReference(property.getFieldName());
            if (beanId != null) {
                writer.putValue(propId, beanId.getInstanceId());
            }
        }
        for (SchemaPropertyRefList property : schema.get(SchemaPropertyRefList.class)) {
            int propId = ids.getSchemaId(property.getFieldName());
            ArrayList<String> list = new ArrayList<>();
            List<BeanId> beanIds = getReference(property.getFieldName());
            if(beanIds != null) {
                for (BeanId id : beanIds) {
                    list.add(id.getInstanceId());
                }
                writer.putValues(propId, list, String.class);
            }
        }
        for (SchemaPropertyRefMap property : schema.get(SchemaPropertyRefMap.class)) {
            int propId = ids.getSchemaId(property.getFieldName());
            ArrayList<String> list = new ArrayList<>();
            List<BeanId> beanIds = getReference(property.getFieldName());
            if(beanIds != null) {
                for (BeanId id : beanIds) {
                    list.add(id.getInstanceId());
                }
                writer.putValues(propId, list, String.class);
            }
        }
        return writer.write();
    }

    public static Bean read(BeanId beanId, byte[] data) {
        if (ids == null) {
            ids = UniqueIds.lookup();
        }
        Preconditions.checkNotNull(beanId);
        Preconditions.checkNotNull(data);
        Schema schema = beanId.getSchema();
        Preconditions.checkNotNull(schema);

        Bean bean = Bean.create(beanId);
        ValueReader reader = new ValueReader(data);

        Multimap<String, String> properties = ArrayListMultimap.create();
        Multimap<String, BeanId> references = ArrayListMultimap.create();

        int[] propertyIds = reader.getIds();
        for (int id : propertyIds) {
            String propertyName = ids.getSchemaName(id);
            Object value = reader.getValue(id);
            if (schema.isProperty(propertyName)) {
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    properties.putAll(propertyName, conversion.convert((Collection)value, String.class));
                } else {
                    properties.put(propertyName, conversion.convert(value, String.class));
                }
            } else if (schema.isReference(propertyName)) {
                String schemaName = schema.getReferenceSchemaName(propertyName);
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    for (String instanceId : (Collection<String>) value) {
                        references.put(propertyName, BeanId.create(instanceId, schemaName));
                    }
                } else {
                    String instanceId = (String) value;
                    references.put(propertyName, BeanId.create(instanceId, schemaName));
                }
            } else {
                throw new IllegalArgumentException("Unrecognized property " + propertyName);
            }
        }
        for (String propertyName : properties.keySet()) {
            bean.addProperty(propertyName, properties.get(propertyName));
        }
        for (String propertyName : references.keySet()) {
            bean.addReference(propertyName, references.get(propertyName));
        }
        bean.set(schema);
        return bean;
    }

}
