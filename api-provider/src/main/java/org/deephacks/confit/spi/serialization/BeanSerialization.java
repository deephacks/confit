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
package org.deephacks.confit.spi.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.model.Schema.SchemaPropertyRefMap;
import org.deephacks.confit.spi.Conversion;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.spi.serialization.ValueSerialization.ValueReader;
import org.deephacks.confit.spi.serialization.ValueSerialization.ValueWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Binary representation of a bean.
 *
 */
public class BeanSerialization {
    private static final Conversion conversion = Conversion.get();
    private static final SchemaManager schemaManager = SchemaManager.lookup();
    private final UniqueId propertyIds;

    public BeanSerialization(UniqueId propertyIds) {
        this.propertyIds = propertyIds;
    }

    public byte[] write(Bean bean) {
        Preconditions.checkNotNull(bean);
        Preconditions.checkNotNull(bean.getSchema());
        ValueWriter writer = new ValueWriter();

        Preconditions.checkNotNull(bean);
        Schema schema = bean.getSchema();
        for (SchemaProperty property : schema.get(SchemaProperty.class)) {
            int propId = (int) propertyIds.getId(property.getFieldName());
            Object value;
            if(writer.isBasicType(property.getClassType())) {
                value = conversion.convert(bean.getSingleValue(property.getFieldName()), property.getClassType());
            } else {
                value = bean.getSingleValue(property.getFieldName());
            }
            writer.putValue(propId, value);
        }
        for (SchemaPropertyList property : schema.get(SchemaPropertyList.class)) {
            int propId = (int) propertyIds.getId(property.getFieldName());
            Collection<?> values;
            if(writer.isBasicType(property.getClassType())) {
                values = conversion.convert(bean.getValues(property.getFieldName()), property.getClassType());
                writer.putValues(propId, values, property.getClassType());
            } else {
                values = bean.getValues(property.getFieldName());
                writer.putValues(propId, values, String.class);
            }
        }
        for (SchemaPropertyRef property : schema.get(SchemaPropertyRef.class)) {
            int propId = (int) propertyIds.getId(property.getFieldName());
            BeanId beanId = bean.getFirstReference(property.getFieldName());
            if (beanId != null) {
                writer.putValue(propId, beanId.getInstanceId());
            }
        }
        for (SchemaPropertyRefList property : schema.get(SchemaPropertyRefList.class)) {
            int propId = (int) propertyIds.getId(property.getFieldName());
            ArrayList<String> list = new ArrayList<>();
            List<BeanId> beanIds = bean.getReference(property.getFieldName());
            if(beanIds != null) {
                for (BeanId id : beanIds) {
                    list.add(id.getInstanceId());
                }
                writer.putValues(propId, list, String.class);
            }
        }
        for (SchemaPropertyRefMap property : schema.get(SchemaPropertyRefMap.class)) {
            int propId = (int) propertyIds.getId(property.getFieldName());
            ArrayList<String> list = new ArrayList<>();
            List<BeanId> beanIds = bean.getReference(property.getFieldName());
            if(beanIds != null) {
                for (BeanId id : beanIds) {
                    list.add(id.getInstanceId());
                }
                writer.putValues(propId, list, String.class);
            }
        }
        return writer.write();
    }

    public Bean read(byte[] data, BeanId beanId, Schema schema) {
        if (data == null) {
            return null;
        }
        Preconditions.checkNotNull(beanId);
        Preconditions.checkNotNull(schema);
        Bean bean = Bean.create(beanId);
        ValueReader reader = new ValueReader(data);

        Multimap<String, String> properties = ArrayListMultimap.create();
        Multimap<String, BeanId> references = ArrayListMultimap.create();

        int[] ids = reader.getIds();
        for (int id : ids) {
            String propertyName = propertyIds.getName(id);
            Object value = reader.getValue(id);
            if (schema.isProperty(propertyName)) {
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    properties.putAll(propertyName, conversion.convert((Collection)value, String.class));
                } else {
                    properties.put(propertyName, conversion.convert(value, String.class));
                }
            } else if (schema.isReference(propertyName)) {
                String schemaName = schema.getReferenceSchemaName(propertyName);
                Schema refSchema = schemaManager.getSchema(schemaName);
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    for (String instanceId : (Collection<String>) value) {
                        if (refSchema.getId().isSingleton()) {
                            references.put(propertyName, BeanId.createSingleton(schemaName));
                        } else {
                            references.put(propertyName, BeanId.create(instanceId, schemaName));
                        }
                    }
                } else {
                    String instanceId = (String) value;
                    if (refSchema.getId().isSingleton()) {
                        references.put(propertyName, BeanId.createSingleton(schemaName));
                    } else {
                        references.put(propertyName, BeanId.create(instanceId, schemaName));
                    }
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

    private Collection<String> toString(Collection<?> collection) {
        ArrayList<String> result = new ArrayList<>();
        for (Object o : collection) {
            result.add(o.toString());
        }
        return result;
    }
}
