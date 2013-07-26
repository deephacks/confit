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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RowMutations;
import org.deephacks.confit.model.Bean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.deephacks.confit.internal.hbase.HBeanRow.PROP_COLUMN_FAMILY;

public class HBeanProperties {
    /** Kryo is used for serializing properties */
    private static final Kryo kryo = new Kryo();
    private KeyValue properties;
    private UniqueIds uids;

    public HBeanProperties(UniqueIds uids) {
        this.uids = uids;
    }

    public HBeanProperties(KeyValue kv, UniqueIds uids) {
        this.properties = kv;
        this.uids = uids;
    }

    public HBeanProperties(final byte[] rowkey, String[][] properties, UniqueIds uids) {
        this.uids = uids;
        this.properties = getProperties(rowkey, properties);

    }

    public HBeanProperties(Bean bean, UniqueIds uids) {
        this.uids = uids;
        set(bean);
    }

    public void set(Bean bean) {
        byte[] rowkey = HBeanRow.getRowKey(bean.getId(), uids);
        String[][] props = getProperties(bean);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final Output out = new Output(bytes);
        try {
            kryo.writeObject(out, props);
        } finally {
            out.close();
            try {
                bytes.close();
            } catch (IOException e) {
                // ignore
            }
        }
        properties = new KeyValue(rowkey, PROP_COLUMN_FAMILY, PROP_COLUMN_FAMILY,
                bytes.toByteArray());
    }

    public void merge(Bean bean, byte[] rowkey) {
        String[][] mergedProps = merge(getProperties(), HBeanProperties.getProperties(bean));
        this.properties = getProperties(rowkey, mergedProps);
        properties = getProperties(rowkey, mergedProps);
    }

    public void merge(HBeanProperties properties, RowMutations mutations) {
        if (properties == null || properties.getProperties().length == 0) {
            return;
        }
        String[][] mergedProps = merge(getProperties(), properties.getProperties());
        this.properties = getProperties(mutations.getRow(), mergedProps);
        Put p = new Put(mutations.getRow());

        try {
            p.add(this.properties);
            mutations.add(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void set(HBeanProperties properties, RowMutations mutations) {
        if (properties == null || properties.getProperties() == null
                || properties.getProperties().length == 0) {
            Delete d = new Delete(mutations.getRow());

            try {
                d.deleteColumn(this.properties.getFamily(), this.properties.getQualifier());
                mutations.add(d);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        String[][] mergedProps = properties.getProperties();
        this.properties = getProperties(mutations.getRow(), mergedProps);
        Put p = new Put(mutations.getRow());

        try {
            p.add(this.properties);
            mutations.add(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String[][] merge(String[][] source, String[][] target) {
        // merge properties
        HashMap<String, String[]> props = new HashMap<>();
        for (String[] p : source) {
            props.put(p[0], Arrays.copyOfRange(p, 1, p.length));
        }

        for (String[] p : target) {
            props.put(p[0], Arrays.copyOfRange(p, 1, p.length));
        }
        String[][] mergedProps = new String[props.size()][];
        int n = 0;
        for (String propertyName : props.keySet()) {
            String[] values = props.get(propertyName);
            String[] merged = new String[values.length + 1];
            merged[0] = propertyName;
            System.arraycopy(values, 0, merged, 1, values.length);
            mergedProps[n++] = merged;
        }
        return mergedProps;
    }

    /**
     * @param props properties in key value form.
     * @return Properties converted into string matrix form.
     */
    public String[][] getProperties() {
        if (properties == null) {
            return new String[0][];
        }
        try (final Input in = new Input(properties.getValue())){
            return kryo.readObject(in, String[][].class);
        }
    }

    /**
     * In order to reduce the amount of property data stored, we convert the
     * properties into a String matrix. The first element is the property name
     * followed by its values.
     *
     * @param bean Bean to get properties from.
     * @return String matrix
     */
    public static String[][] getProperties(final Bean bean) {
        final List<String> propertyNames = bean.getPropertyNames();
        final int psize = propertyNames.size();
        final String[][] properties = new String[psize][];
        for (int i = 0; i < psize; i++) {
            final String propertyName = propertyNames.get(i);
            final List<String> values = bean.getValues(propertyName);
            final int vsize = values.size();
            properties[i] = new String[vsize + 1];
            properties[i][0] = propertyName;
            for (int j = 0; j < vsize; j++) {
                properties[i][j + 1] = values.get(j);
            }
        }
        return properties;
    }

    public KeyValue getPropertiesKeyValue() {
        return properties;
    }

    private KeyValue getProperties(final byte[] rowkey, String[][] properties) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final Output out = new Output(bytes);
        try {
            kryo.writeObject(out, properties);
        } finally {
            out.close();
            try {
                bytes.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return new KeyValue(rowkey, PROP_COLUMN_FAMILY, PROP_COLUMN_FAMILY, bytes.toByteArray());
    }

    /**
     * Set a string matrix of properties on a particular bean.
     *
     * @param bean Bean to set properties on.
     * @param properties in string matrix form.
     */
    public void setPropertiesOn(final Bean bean) {
        String[][] properties = getProperties();
        for (int i = 0; i < properties.length; i++) {
            if (properties[i].length < 2) {
                continue;
            }
            for (int j = 0; j < properties[i].length - 1; j++) {
                bean.addProperty(properties[i][0], properties[i][j + 1]);
            }
        }
    }

    /**
     * Extract the property name from a key value.
     */
    public static String getPropertyName(KeyValue kv, UniqueIds uids) {
        final byte[] qualifier = kv.getQualifier();
        final byte[] pid = new byte[] { qualifier[2], qualifier[3] };
        final String propertyName = uids.getUsid().getName(pid);
        return propertyName;
    }

    /**
     * If this key value is of property familiy type.
     */
    public static boolean isProperty(KeyValue kv) {
        if (Bytes.equals(kv.getFamily(), PROP_COLUMN_FAMILY)) {
            return true;
        }
        return false;
    }

}
