package org.deephacks.confit.spi.serialization;

import org.deephacks.confit.spi.serialization.BytesUtils.DataType;
import org.deephacks.confit.spi.serialization.BytesUtils.Reference;
import org.deephacks.confit.spi.serialization.BytesUtils.ReferenceList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Each byte array begin with a header followed by actual data. The header
 * store metadata that track id and type for each value and where actual
 * values are stored in the byte array.
 */
public class ValueSerialization {

    /** 4 byte property id and 4 byte index pointing to the value */
    private static final int SIZE_PER_INDEX = 8;

    /**
     * Used for writing beans to binary form.
     */
    public static class ValueWriter {
        private TreeMap<Integer, Value> properties = new TreeMap<>();

        /**
         * Put a collection type property.
         *
         * @param id id of the property.
         * @param collection values
         * @param cls type of values in the collection.
         */
        public void putValues(int id, Collection<?> collection, Class<?> cls) {
            try {
                if (collection == null) {
                    return;
                }
                DataType type = DataType.getDataType(cls);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int size = collection.size();
                switch (type) {
                    case BYTE:
                        bytes.write(DataType.BYTE_LIST.getId());
                        bytes.write(Bytes.fromInt(size));
                        for (Object o : collection) {
                            bytes.write((Byte) o);
                        }
                        properties.put(id, new Value(bytes.toByteArray(), DataType.BYTE_LIST));
                        break;
                    case SHORT:
                        bytes.write(DataType.SHORT_LIST.getId());
                        bytes.write(Bytes.fromInt(size));
                        for (Object o : collection) {
                            bytes.write(Bytes.fromShort((Short) o));
                        }
                        properties.put(id, new Value(bytes.toByteArray(), DataType.SHORT_LIST));
                        break;
                    case INTEGER:
                        bytes.write(DataType.INTEGER_LIST.getId());
                        bytes.write(Bytes.fromInt(size));
                        for (Object o : collection) {
                            bytes.write(Bytes.fromInt((Integer) o));
                        }
                        properties.put(id, new Value(bytes.toByteArray(), DataType.INTEGER_LIST));
                        break;
                    case LONG:
                        bytes.write(DataType.LONG_LIST.getId());
                        bytes.write(Bytes.fromInt(size));
                        for (Object o : collection) {
                            bytes.write(Bytes.fromLong((Long) o));
                        }
                        properties.put(id, new Value(bytes.toByteArray(), DataType.LONG_LIST));
                        break;
                    case FLOAT:
                        bytes.write(DataType.FLOAT_LIST.getId());
                        bytes.write(Bytes.fromInt(size));
                        for (Object o : collection) {
                            bytes.write(Bytes.fromFloat((Float) o));
                        }
                        properties.put(id, new Value(bytes.toByteArray(), DataType.FLOAT_LIST));
                        break;
                    case DOUBLE:
                        bytes.write(DataType.DOUBLE_LIST.getId());
                        bytes.write(Bytes.fromInt(size));
                        for (Object o : collection) {
                            bytes.write(Bytes.fromDouble((Double) o));
                        }
                        properties.put(id, new Value(bytes.toByteArray(), DataType.DOUBLE_LIST));
                        break;
                    case BOOLEAN:
                        bytes.write(DataType.BOOLEAN_LIST.getId());
                        bytes.write(Bytes.fromInt(size));
                        for (Object o : collection) {
                            bytes.write(BytesUtils.toByte((Boolean) o));
                        }
                        properties.put(id, new Value(bytes.toByteArray(), DataType.BOOLEAN_LIST));
                        break;
                    case STRING:
                        bytes.write(DataType.STRING_LIST.getId());
                        String[] strings = collection.toArray(new String[collection.size()]);
                        byte[] byteString = BytesUtils.toBytes(strings);
                        bytes.write(byteString);
                        properties.put(id, new Value(bytes.toByteArray(), DataType.STRING_LIST));
                        break;
                    default:
                        throw new UnsupportedOperationException("Did not recognize " + type);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Put a single valued property.
         *
         * @param id id of the property.
         * @param value value of the property
         */
        public void putValue(int id, Object value) {
            try {
                if (value == null) {
                    return;
                }
                DataType type = DataType.getDataType(value.getClass());
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                switch (type) {
                    case BYTE:
                        bytes.write(DataType.BYTE.getId());
                        bytes.write((Byte) value);
                        properties.put(id, new Value(bytes.toByteArray(), DataType.BYTE));
                        break;
                    case SHORT:
                        bytes.write(DataType.SHORT.getId());
                        bytes.write(Bytes.fromShort((Short)value));
                        properties.put(id, new Value(bytes.toByteArray(), DataType.SHORT));
                        break;
                    case INTEGER:
                        bytes.write(DataType.INTEGER.getId());
                        bytes.write(Bytes.fromInt((Integer)value));
                        properties.put(id, new Value(bytes.toByteArray(), DataType.INTEGER));
                        break;
                    case LONG:
                        bytes.write(DataType.LONG.getId());
                        bytes.write(Bytes.fromLong((Long)value));
                        properties.put(id, new Value(bytes.toByteArray(), DataType.LONG));
                        break;
                    case FLOAT:
                        bytes.write(DataType.FLOAT.getId());
                        bytes.write(Bytes.fromFloat((Float)value));
                        properties.put(id, new Value(bytes.toByteArray(), DataType.FLOAT));
                        break;
                    case DOUBLE:
                        bytes.write(DataType.DOUBLE.getId());
                        bytes.write(Bytes.fromDouble((Double)value));
                        properties.put(id, new Value(bytes.toByteArray(), DataType.DOUBLE));
                        break;
                    case BOOLEAN:
                        bytes.write(DataType.BOOLEAN.getId());
                        bytes.write(BytesUtils.toByte((Boolean)value));
                        properties.put(id, new Value(bytes.toByteArray(), DataType.BOOLEAN));
                        break;
                    case STRING:
                        bytes.write(DataType.STRING.getId());
                        byte[] b = ((String) value).getBytes();
                        bytes.write(Bytes.fromInt(b.length));
                        bytes.write(b);
                        properties.put(id, new Value(bytes.toByteArray(), DataType.STRING));
                        break;
                    case REFERENCE:
                        Reference ref = (Reference) value;
                        bytes.write(DataType.REFERENCE.getId());
                        byte[] byteString = BytesUtils.toBytes(ref);
                        bytes.write(byteString);
                        properties.put(id, new Value(bytes.toByteArray(), DataType.REFERENCE));
                        break;
                    case REFERENCE_LIST:
                        ReferenceList list = (ReferenceList) value;
                        bytes.write(DataType.REFERENCE_LIST.getId());
                        byteString = BytesUtils.toBytes(list);
                        bytes.write(byteString);
                        properties.put(id, new Value(bytes.toByteArray(), DataType.REFERENCE_LIST));
                        break;
                    default:
                        throw new UnsupportedOperationException("Type not recognized " + type);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return writes the values into binary form.
         */
        public byte[] write() {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int indexSize = properties.size() * SIZE_PER_INDEX;
                int idx = 4 + indexSize;
                bytes.write(Bytes.fromInt(idx));
                for (Integer id : properties.keySet()) {
                    Value property = properties.get(id);
                    bytes.write(Bytes.fromInt(id));
                    bytes.write(Bytes.fromInt(idx));
                    idx += property.bytes.length;
                }
                for (Integer id : properties.keySet()) {
                    Value property = properties.get(id);
                    bytes.write(property.bytes);
                }
                return bytes.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean isBasicType(Class<?> type) {
            if(String.class.isAssignableFrom(type)) {
                return true;
            } else if (Byte.class.isAssignableFrom(type)) {
                return true;
            } else if (byte.class.isAssignableFrom(type)) {
                return true;
            } else if (Short.class.isAssignableFrom(type)) {
                return true;
            } else if (short.class.isAssignableFrom(type)) {
                return true;
            } else if (Integer.class.isAssignableFrom(type)) {
                return true;
            } else if (int.class.isAssignableFrom(type)) {
                return true;
            } else if (Long.class.isAssignableFrom(type)) {
                return true;
            } else if (long.class.isAssignableFrom(type)) {
                return true;
            } else if (Float.class.isAssignableFrom(type)) {
                return true;
            } else if (float.class.isAssignableFrom(type)) {
                return true;
            } else if (Double.class.isAssignableFrom(type)) {
                return true;
            } else if (double.class.isAssignableFrom(type)) {
                return true;
            } else if (Boolean.class.isAssignableFrom(type)) {
                return true;
            } else if (boolean.class.isAssignableFrom(type)) {
                return true;
            }
            return false;
        }
    }
    /**
     * Used for reading a binary representation of values.
     */
    public static class ValueReader {
        /** raw KeyValue data of the bean */
        private byte[] data;

        /**
         * Construct values that was written by a ValueWriter.
         *
         * @param data raw values data.
         */
        public ValueReader(byte[] data) {
            this.data = data;
        }

        public Object getValue(int id) {
            int idx = getIndex(id);
            if (idx < 0) {
                return null;
            }
            DataType type = DataType.getDataType(data[idx]);
            idx = idx + 1;
            switch (type) {
                case BYTE: return data[idx];
                case SHORT: return BytesUtils.getShort(data, idx);
                case INTEGER: return Bytes.getInt(data, idx);
                case LONG: return Bytes.getLong(data, idx);
                case FLOAT: return BytesUtils.getFloat(data, idx);
                case DOUBLE: return BytesUtils.getDouble(data, idx);
                case BOOLEAN: return data[idx] != 0;
                case STRING: return BytesUtils.getString(data, idx);
                case REFERENCE: return BytesUtils.toReference(data, idx);
                case BYTE_LIST: return BytesUtils.toByteList(data, idx);
                case SHORT_LIST: return BytesUtils.toShortList(data, idx);
                case INTEGER_LIST: return BytesUtils.toIntList(data, idx);
                case LONG_LIST: return BytesUtils.toLongList(data, idx);
                case FLOAT_LIST: return BytesUtils.toFloatList(data, idx);
                case DOUBLE_LIST: return BytesUtils.toDoubleList(data, idx);
                case BOOLEAN_LIST: return BytesUtils.toBooleanList(data, idx);
                case STRING_LIST: return BytesUtils.toStringList(data, idx);
                case REFERENCE_LIST: return BytesUtils.toReferences(data, idx);
                default:
                    throw new UnsupportedOperationException("Could not recognize " + type);
            }
        }

        private int getIndex(int id) {
            int indexSize = Bytes.getInt(data, 0);
            if(indexSize <= 4) {
                return -1;
            }
            for (int i = 4; i < indexSize + 4; i += SIZE_PER_INDEX) {
                int propertyId = Bytes.getInt(data, i);
                if (propertyId < id) {
                    continue;
                } else if (propertyId > id) {
                    return -1;
                }
                return Bytes.getInt(data, i + 4);
            }
            return -1;
        }

        public int[] getIds() {
            int indexSize = Bytes.getInt(data, 0);
            int[] ids = new int[indexSize / SIZE_PER_INDEX];
            int idx = 0;
            for (int i = 4; i < indexSize; i += SIZE_PER_INDEX) {
                int propertyId = Bytes.getInt(data, i);
                ids[idx++] = propertyId;
            }
            return ids;
        }
    }
    static class Value {
        public byte[] bytes;
        public DataType type;
        public Value(byte[] bytes, DataType type)  {
            this.bytes = bytes;
            this.type = type;
        }
    }
}
