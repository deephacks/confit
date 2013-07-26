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
package org.deephacks.confit.internal.hbase;

import org.apache.hadoop.hbase.KeyValue;
import org.deephacks.confit.internal.hbase.BytesUtils.Reference;
import org.deephacks.confit.internal.hbase.BytesUtils.ReferenceList;
import org.deephacks.confit.internal.hbase.HBeanKeyValue.HBeanReader;
import org.deephacks.confit.internal.hbase.HBeanKeyValue.HBeanWriter;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.model.Schema.SchemaPropertyRefMap;
import org.deephacks.confit.spi.Conversion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * HBase representation of a bean.
 *
 * @see org.deephacks.confit.internal.hbase.HBeanKeyValue.HBeanReader
 * @see org.deephacks.confit.internal.hbase.HBeanKeyValue.HBeanWriter
 */
public class HBean {
    private static final Conversion conversion = Conversion.get();
    private HBeanWriter writer = new HBeanWriter();
    private HBeanReader reader;
    private final UniqueIds uids;

    public HBean(Bean bean, UniqueIds uids) {
        this.uids = uids;
        Schema schema = bean.getSchema();
        for (SchemaProperty property : schema.get(SchemaProperty.class)) {
            int propId = Bytes.getShort(uids.getUpid().getId(property.getFieldName()));
            Object value;
            if(writer.isBasicType(property.getClassType())) {
                value = conversion.convert(bean.getSingleValue(property.getFieldName()), property.getClassType());
            } else {
                value = bean.getSingleValue(property.getFieldName());
            }
            writer.putValue(propId, value);
        }
        for (SchemaPropertyList property : schema.get(SchemaPropertyList.class)) {
            int propId = Bytes.getShort(uids.getUpid().getId(property.getFieldName()));
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
            int propId = Bytes.getShort(uids.getUpid().getId(property.getFieldName()));
            int sid = Bytes.getShort(uids.getUsid().getId(property.getSchemaName()));
            BeanId beanId = bean.getFirstReference(property.getFieldName());
            if (beanId != null) {
                Reference ref = new Reference(sid, beanId.getInstanceId());
                writer.putValue(propId, ref);
            }
        }
        for (SchemaPropertyRefList property : schema.get(SchemaPropertyRefList.class)) {
            int propId = Bytes.getShort(uids.getUpid().getId(property.getFieldName()));
            int sid = Bytes.getShort(uids.getUsid().getId(property.getSchemaName()));
            ReferenceList list = new ReferenceList(sid);
            List<BeanId> beanIds = bean.getReference(property.getFieldName());
            if(beanIds != null) {
                for (BeanId id : beanIds) {
                    list.addInstance(id.getInstanceId());
                }
                writer.putValue(propId, list);
            }
        }
        for (SchemaPropertyRefMap property : schema.get(SchemaPropertyRefMap.class)) {
            int propId = Bytes.getShort(uids.getUpid().getId(property.getFieldName()));
            int sid = Bytes.getShort(uids.getUsid().getId(property.getSchemaName()));
            ReferenceList list = new ReferenceList(sid);
            List<BeanId> beanIds = bean.getReference(property.getFieldName());
            if(beanIds != null) {
                for (BeanId id : beanIds) {
                    list.addInstance(id.getInstanceId());
                }
                writer.putValue(propId, list);
            }
        }
    }

    public HBean(KeyValue kv, UniqueIds uids) {
        this.uids = uids;
        this.reader = new HBeanReader(kv.getValue());
    }

    public int[] getIds() {
        return reader.getIds();
    }

    public byte[] getBytes() {
        return writer.write();
    }

    public void set(Bean bean) {
        // TODO
        int[] ids = reader.getIds();
        for (int id : ids) {
            String propertyName = uids.getUpid().getName(Bytes.fromInt((id)));
            Object value = reader.getValue(id);
            if (Collection.class.isAssignableFrom(value.getClass())) {
                bean.addProperty(propertyName, toString((Collection<?>) value));
            } else if (ReferenceList.class.isAssignableFrom(value.getClass())) {

            } else {
                bean.addProperty(propertyName, value.toString());
            }
        }
    }

    public void merge(Bean bean) {
        // TODO
    }

    private List<String> toString(Collection<?> collection) {
        ArrayList<String> result = new ArrayList<>();
        for (Object o : collection) {
            result.add(o.toString());
        }
        return result;
    }
}
