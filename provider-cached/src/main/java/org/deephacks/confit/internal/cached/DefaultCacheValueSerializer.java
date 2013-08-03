package org.deephacks.confit.internal.cached;

import org.deephacks.cached.CacheValueSerializer;
import org.deephacks.cached.buffer.ByteBuf;
import org.deephacks.cached.buffer.Unpooled;
import org.deephacks.cached.buffer.util.internal.chmv8.ConcurrentHashMapV8;
import org.deephacks.confit.internal.cached.DefaultCacheValueSerializer.UniqueId.ByteArrayReader;
import org.deephacks.confit.internal.cached.DefaultCacheValueSerializer.UniqueId.DataType;
import org.deephacks.confit.internal.cached.DefaultCacheValueSerializer.UniqueId.UnsafeSetOp;
import org.deephacks.confit.internal.cached.proxy.ConfigProxyGenerator;
import org.deephacks.confit.internal.cached.proxy.ConfigReferenceHolder;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.spi.Conversion;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.deephacks.confit.internal.cached.UnsafeUtils.*;

/**
 * Serializer that read and write object proxies to a binary off-heap cache.
 * DefaultCacheValueSerializer is roughly twice as fast KryoCacheValueSerializer.
 *
 * Each class is given an integer id which is used to correlate how to read objects
 * from a byte buffer without actually writing the whole class into the buffer.
 */
public class DefaultCacheValueSerializer extends CacheValueSerializer<Object>  {

    /** keeps track of class -> id */
    private static final ConcurrentHashMapV8<Class<?>, Integer> classToId = new ConcurrentHashMapV8<>();

    /** keeps track of id -> class */
    private static final ConcurrentHashMapV8<Integer, Class<?>> idToClass = new ConcurrentHashMapV8<>();
    private static final ConcurrentHashMapV8<Integer, UniqueId> idToUniqueIds = new ConcurrentHashMapV8<>();

    private static final ConcurrentHashMapV8<String, Schema> schemas = new ConcurrentHashMapV8<>();
    private static final Unsafe unsafe = getUnsafe();

    /** maintain unique ids to classes */
    private static final AtomicInteger clsCount = new AtomicInteger(0);
    private static final Conversion conversion = Conversion.get();

    @Override
    public ByteBuf write(Object value) {
        Class<?> cls = value.getClass();
        Integer id = classToId.get(cls);
        if(id == null) {
            id = clsCount.incrementAndGet();
            classToId.put(cls, id);
            idToClass.put(id, cls);
        }
        UniqueId uniqueId = idToUniqueIds.get(id);
        if(uniqueId == null) {
            uniqueId = new UniqueId();
            idToUniqueIds.put(id, uniqueId);
        }
        ByteBuf buf = Unpooled.directBuffer();
        buf.writeInt(id);
        Schema schema = schemas.get(cls.getName());

        writeId(value, schema.getId().getName(), buf, uniqueId);

        for (SchemaProperty property : schema.get(SchemaProperty.class)) {
            writeProperty(value, property, buf, uniqueId);
        }
        for (SchemaPropertyList property : schema.get(SchemaPropertyList.class)) {
            writeProperty(value, property, buf, uniqueId);
        }
        writeReferenceHolder(value, buf, uniqueId);
        return buf;
    }

    @Override
    public Object read(ByteBuf buf) {
        buf.resetReaderIndex();
        int id = buf.readInt();
        UniqueId uniqueId = idToUniqueIds.get(id);
        Object object = createObject(id);
        setProperties(object, uniqueId, buf);
        return object;
    }

    public void put(Schema schema) {
        schemas.put(schema.getClassType().getName() + ConfigProxyGenerator.PROXY_CLASS_SUFFIX, schema);
    }

    private void writeId(Object object, String fieldName, ByteBuf buf, UniqueId uniqueId) {
        Field field;
        Class<?> cls = object.getClass();
        try {
            field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Integer id = getId(uniqueId, field, fieldName, DataType.SCHEMA_ID, String.class);

        buf.writeInt(id);
        String value = (String) getValue(object, fieldName);
        byte[] bytes = value.getBytes();
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private Integer getId(UniqueId uniqueId, Field field, String fieldName, DataType type, Class<?> cls) {
        return uniqueId.getId(new UnsafeSetOp(field, fieldName, type, cls));
    }

    private Integer getId(UniqueId uniqueId, Field field, String fieldName, DataType type) {
        return uniqueId.getId(new UnsafeSetOp(field, fieldName, type));
    }

    private void writeReferenceHolder(Object object, ByteBuf buf, UniqueId uniqueId) {
        Class<?> cls = object.getClass();
        String fieldName = ConfigProxyGenerator.PROXY_FIELD_NAME;
        Field field = null;
        ConfigReferenceHolder holder;
        try {
            field = cls.getDeclaredField(ConfigProxyGenerator.PROXY_FIELD_NAME);
            field.setAccessible(true);
            holder = (ConfigReferenceHolder) field.get(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Integer id = getId(uniqueId, field, fieldName, DataType.REFERENCE_HOLDER);
        // Integer id = uniqueId.getBeanId(new SetOp(field, fieldName, DataType.REFERENCE_HOLDER, String.class));
        Map<String, List<String>> references = holder.getReferences();

        buf.writeInt(id);
        Collection<String> properties = references.keySet();
        buf.writeInt(properties.size());
        for (String property : properties) {
            List<String> instanceIds = references.get(property);
            byte[] bytes = property.getBytes();
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
            buf.writeInt(instanceIds.size());
            for (String instanceId : instanceIds)  {
                bytes = instanceId.getBytes();
                buf.writeInt(bytes.length);
                buf.writeBytes(bytes);
            }
        }
    }

    private Object createObject(int id)  {
        try {
            Class<?> cls = idToClass.get(id);
            return cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeProperty(Object object, SchemaPropertyList property, ByteBuf buf, UniqueId uniqueId) {
        Class<?> cls = object.getClass();
        Class<?> propertyClassType = property.getClassType();
        String fieldName = property.getFieldName();
        Field field;
        try {
            field = cls.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true);
        Collection collection = (Collection) getValue(object, fieldName);
        boolean isSet = Set.class.isAssignableFrom(collection.getClass());
        if(collection == null) {
            return;
        }
        int size = collection.size();
        if(Byte.class.isAssignableFrom(propertyClassType) || byte.class.isAssignableFrom(propertyClassType)) {
            Integer id;
            if(isSet) {
                id = getId(uniqueId, field, fieldName, DataType.BYTE_SET);
            } else {
                id = getId(uniqueId, field, fieldName, DataType.BYTE_LIST);
            }
            buf.writeInt(id);
            buf.writeInt(size);
            for (Object value : collection) {
                buf.writeByte((Byte) value);
            }

        } else if (Short.class.isAssignableFrom(propertyClassType) || short.class.isAssignableFrom(propertyClassType)) {
            Integer id;
            if(isSet) {
                id = getId(uniqueId, field, fieldName, DataType.SHORT_SET);
            } else {
                id = getId(uniqueId, field, fieldName, DataType.SHORT_LIST);
            }
            buf.writeInt(id);
            buf.writeInt(size);
            for (Object value : collection) {
                buf.writeShort((Short) value);
            }
        } else if (Integer.class.isAssignableFrom(propertyClassType) || int.class.isAssignableFrom(propertyClassType)) {
            Integer id;
            if(isSet) {
                id = getId(uniqueId, field, fieldName, DataType.INTEGER_SET);
            } else {
                id = getId(uniqueId, field, fieldName, DataType.INTEGER_LIST);
            }
            buf.writeInt(id);
            buf.writeInt(size);
            for (Object value : collection) {
                buf.writeInt((Integer) value);
            }
        } else if (Long.class.isAssignableFrom(propertyClassType) || long.class.isAssignableFrom(propertyClassType)) {
            Integer id;
            if(isSet) {
                id = getId(uniqueId, field, fieldName, DataType.LONG_SET);
            } else {
                id = getId(uniqueId, field, fieldName, DataType.LONG_LIST);
            }
            buf.writeInt(id);
            buf.writeInt(size);
            for (Object value : collection) {
                buf.writeLong((Long) value);
            }
        } else if (Float.class.isAssignableFrom(propertyClassType) || float.class.isAssignableFrom(propertyClassType)) {
            Integer id;
            if(isSet) {
                id = getId(uniqueId, field, fieldName, DataType.FLOAT_SET);
            } else {
                id = getId(uniqueId, field, fieldName, DataType.FLOAT_LIST);
            }
            buf.writeInt(id);
            buf.writeInt(size);
            for (Object value : collection) {
                buf.writeFloat((Float) value);
            }
        } else if (Double.class.isAssignableFrom(propertyClassType) || double.class.isAssignableFrom(propertyClassType)) {
            Integer id;
            if(isSet) {
                id = getId(uniqueId, field, fieldName, DataType.DOUBLE_SET);
            } else {
                id = getId(uniqueId, field, fieldName, DataType.DOUBLE_LIST);
            }
            buf.writeInt(id);
            buf.writeInt(size);
            for (Object value : collection) {
                buf.writeDouble((Double) value);
            }
        } else if (Boolean.class.isAssignableFrom(propertyClassType) || boolean.class.isAssignableFrom(propertyClassType)) {
            Integer id;
            if(isSet) {
                id = getId(uniqueId, field, fieldName, DataType.BOOLEAN_SET);
            } else {
                id = getId(uniqueId, field, fieldName, DataType.BOOLEAN_LIST);
            }
            buf.writeInt(id);
            buf.writeInt(size);
            for (Object value : collection) {
                if ((Boolean) value) {
                    buf.writeByte(1);
                } else {
                    buf.writeByte(0);
                }
            }
        } else if (String.class.isAssignableFrom(propertyClassType)) {
            Integer id;
            if(isSet) {
                id = getId(uniqueId, field, fieldName, DataType.STRING_SET);
            } else {
                id = getId(uniqueId, field, fieldName, DataType.STRING_LIST);
            }
            buf.writeInt(id);
            buf.writeInt(size);
            for (Object value : collection) {
                byte[] bytes = value.toString().getBytes();
                buf.writeInt(bytes.length);
                buf.writeBytes(bytes);
            }
        } else {
            Integer id;
            if(isSet) {
                id = getId(uniqueId, field, fieldName, DataType.OBJECT_SET, propertyClassType);
            } else {
                id = getId(uniqueId, field, fieldName, DataType.OBJECT_LIST, propertyClassType);
            }
            buf.writeInt(id);
            buf.writeInt(size);
            Collection<String> stringValue = conversion.convert(collection, String.class);
            for (String value : stringValue) {
                byte[] bytes = value.getBytes();
                buf.writeInt(bytes.length);
                buf.writeBytes(bytes);
            }
        }
    }

    private void writeProperty(Object object, SchemaProperty property, ByteBuf buf, UniqueId uniqueId) {
        Class<?> cls = object.getClass();
        Class<?> propertyClassType = property.getClassType();
        String fieldName = property.getFieldName();
        Field field;
        try {
            field = cls.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true);
        Object value = getValue(object, fieldName);
        if(value == null) {
            return;
        }
        if(Byte.class.isAssignableFrom(propertyClassType) || byte.class.isAssignableFrom(propertyClassType)) {
            Integer id = getId(uniqueId, field, fieldName, DataType.BYTE);
            buf.writeInt(id);
            buf.writeByte((Byte) value);
        } else if (Short.class.isAssignableFrom(propertyClassType) || short.class.isAssignableFrom(propertyClassType)) {
            Integer id = getId(uniqueId, field, fieldName, DataType.SHORT);
            buf.writeInt(id);
            buf.writeShort((Short) value);
        } else if (Integer.class.isAssignableFrom(propertyClassType) || int.class.isAssignableFrom(propertyClassType)) {
            Integer id = getId(uniqueId, field, fieldName, DataType.INTEGER);
            buf.writeInt(id);
            buf.writeInt((Integer) value);
        } else if (Long.class.isAssignableFrom(propertyClassType) || long.class.isAssignableFrom(propertyClassType)) {
            Integer id = getId(uniqueId, field, fieldName, DataType.LONG);
            buf.writeInt(id);
            buf.writeLong((Long) value);
        } else if (Float.class.isAssignableFrom(propertyClassType) || float.class.isAssignableFrom(propertyClassType)) {
            Integer id = getId(uniqueId, field, fieldName, DataType.FLOAT);
            buf.writeInt(id);
            buf.writeFloat((Float) value);
        } else if (Double.class.isAssignableFrom(propertyClassType) || double.class.isAssignableFrom(propertyClassType)) {
            Integer id = getId(uniqueId, field, fieldName, DataType.DOUBLE);
            buf.writeInt(id);
            buf.writeDouble((Double) value);
        } else if (Boolean.class.isAssignableFrom(propertyClassType) || boolean.class.isAssignableFrom(propertyClassType)) {
            Integer id = getId(uniqueId, field, fieldName, DataType.BOOLEAN);
            buf.writeInt(id);
            if ((Boolean) value) {
                buf.writeByte(1);
            } else {
                buf.writeByte(0);
            }
        } else if (String.class.isAssignableFrom(propertyClassType)) {
            Integer id = getId(uniqueId, field, fieldName, DataType.STRING);
            buf.writeInt(id);
            byte[] bytes = value.toString().getBytes();
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        } else {
            Integer id = getId(uniqueId, field, fieldName, DataType.OBJECT, propertyClassType);
            buf.writeInt(id);
            String stringValue = conversion.convert(value, String.class);
            byte[] bytes = stringValue.getBytes();
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }
    }

    /**
     * This is where performance gets really important since we want to serialize each
     * off-heap ByteBuf into an instance and return it to the client as fast as possible.
     */
    private void setProperties(Object object, UniqueId uniqueId, ByteBuf byteBuf) {
        ByteArrayReader reader = new ByteArrayReader(byteBuf);

        while(reader.available()) {
            int id = reader.readInt();
            UnsafeSetOp op = uniqueId.getUnsafeSetOp(id);
            Object value;
            int size;
            ArrayList<Object> list = new ArrayList<>();
            HashSet<Object> set = new HashSet<>();
            switch (op.getType()) {
                case SCHEMA_ID:
                    int length = reader.readInt();
                    value = reader.readString(length);
                    op.set(object, value);
                    break;
                case BYTE:
                    byte byteValue = (byte) reader.readByte();
                    op.set(object, byteValue);
                    break;
                case SHORT:
                    short shortVvalue = (short) reader.readShort();
                    op.set(object, shortVvalue);
                    break;
                case INTEGER:
                    int intValue = reader.readInt();
                    op.set(object, intValue);
                    break;
                case LONG:
                    long longValue = reader.readLong();
                    op.set(object, longValue);
                    break;
                case FLOAT:
                    float floatValue = reader.readFloat();
                    op.set(object, floatValue);
                    break;
                case DOUBLE:
                    double doubleValue = reader.readDouble();
                    op.set(object, doubleValue);
                    break;
                case STRING:
                    length = reader.readInt();
                    value = reader.readString(length);
                    op.set(object, value);
                    break;
                case BOOLEAN:
                    value = reader.readBoolean();
                    op.set(object, value);
                    break;
                case OBJECT:
                    length = reader.readInt();
                    value = conversion.convert(reader.readString(length), op.getObjectClass());
                    op.set(object, value);
                    break;
                case BYTE_LIST:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        list.add(reader.readByte());
                    }
                    op.set(object, list);
                    break;
                case SHORT_LIST:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        list.add(reader.readShort());
                    }
                    op.set(object, list);
                    break;
                case INTEGER_LIST:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        list.add(reader.readInt());
                    }
                    op.set(object, list);
                    break;
                case LONG_LIST:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        list.add(reader.readLong());
                    }
                    op.set(object, list);
                    break;
                case FLOAT_LIST:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        list.add(reader.readFloat());
                    }
                    op.set(object, list);
                    break;
                case DOUBLE_LIST:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        list.add(reader.readDouble());
                    }
                    op.set(object, list);
                    break;
                case STRING_LIST:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        length = reader.readInt();
                        list.add(reader.readString(length));
                    }
                    op.set(object, list);
                    break;
                case BOOLEAN_LIST:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        list.add(reader.readBoolean());
                    }
                    op.set(object, list);
                    break;
                case OBJECT_LIST:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        length = reader.readInt();
                        String stringValue = reader.readString(length);
                        value = conversion.convert(stringValue, op.getObjectClass());
                        list.add(value);
                    }
                    op.set(object, list);
                    break;
                case BYTE_SET:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        set.add(reader.readByte());
                    }
                    op.set(object, set);
                    break;
                case SHORT_SET:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        set.add(reader.readShort());
                    }
                    op.set(object, set);
                    break;
                case INTEGER_SET:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        set.add(reader.readInt());
                    }
                    op.set(object, set);
                    break;
                case LONG_SET:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        set.add(reader.readLong());
                    }
                    op.set(object, set);
                    break;
                case FLOAT_SET:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        set.add(reader.readFloat());
                    }
                    op.set(object, set);
                    break;
                case DOUBLE_SET:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        set.add(reader.readDouble());
                    }
                    op.set(object, set);
                    break;
                case STRING_SET:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        length = reader.readInt();
                        set.add(reader.readString(length));
                    }
                    op.set(object, set);
                    break;
                case BOOLEAN_SET:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        set.add(reader.readBoolean());
                    }
                    op.set(object, set);
                    break;
                case OBJECT_SET:
                    size = reader.readInt();
                    for (int i = 0; i < size; i++) {
                        length = reader.readInt();
                        String stringValue = reader.readString(length);
                        value = conversion.convert(stringValue, op.getObjectClass());
                        set.add(value);
                    }
                    op.set(object, set);
                    break;
                case REFERENCE_HOLDER:
                    Map<String, List<String>> references = new HashMap<>();
                    ConfigReferenceHolder holder = new ConfigReferenceHolder(references);
                    int numProperties = reader.readInt();
                    for (int i = 0; i < numProperties; i++) {
                        length = reader.readInt();
                        String property = reader.readString(length);
                        int numInstances = reader.readInt();
                        ArrayList<String> instances = new ArrayList<>();
                        for (int j = 0; j < numInstances; j++) {
                            length = reader.readInt();
                            instances.add(reader.readString(length));
                        }
                        references.put(property, instances);
                    }
                    op.set(object, holder);
                    break;
            }
        }
    }

    private Object getValue(Object object, String field) {
        try {
            Field f = object.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Keeps track of how to de-serialize certain data types of a ByteBuf into actual
     * values. We could do this with reflection, but it is a lot faster to switch on
     * an enum than doing reflection operations.
     */
    public static class UniqueId {
        private final ConcurrentHashMap<String, Integer> idCache = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Integer, SetOp> setOpCache = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Integer, UnsafeSetOp> unsafeSetOpCache = new ConcurrentHashMap<>();
        private final AtomicInteger counter = new AtomicInteger();

        public Integer getId(final SetOp setOp) {
            Integer id = idCache.get(setOp.fieldName);
            if (id != null) {
                return id;
            }
            id = counter.incrementAndGet();
            idCache.put(setOp.fieldName, id);
            setOpCache.put(id, setOp);
            return id;
        }

        public SetOp getSetOp(final Integer id) {
            return setOpCache.get(id);
        }

        public Integer getId(final UnsafeSetOp setOp) {
            Integer id = idCache.get(setOp.fieldName);
            if (id != null) {
                return id;
            }
            id = counter.incrementAndGet();
            idCache.put(setOp.fieldName, id);
            unsafeSetOpCache.put(id, setOp);
            return id;
        }

        public UnsafeSetOp getUnsafeSetOp(final Integer id) {
            return unsafeSetOpCache.get(id);
        }

        public static class SetOp {
            private final Field field;
            private final String fieldName;
            private final DataType type;
            private Class<?> objectClass;

            public SetOp(Field field, String fieldName, DataType type) {
                this.field = field;
                this.fieldName = fieldName;
                this.type = type;
            }

            public SetOp(Field field, String fieldName, DataType type, Class<?> objectClass) {
                this(field, fieldName, type);
                this.objectClass = objectClass;
            }

            public String getFieldName() {
                return fieldName;
            }

            public DataType getType() {
                return type;
            }

            public void set(Object object, Object value) {
                try {
                    field.set(object, value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public Class<?> getObjectClass() {
                return objectClass;
            }
        }

        public static class UnsafeSetOp {
            private final String fieldName;
            private final DataType type;
            private final Class<?> fieldType;
            private long offset;

            private Class<?> objectClass;

            public UnsafeSetOp(Field field, String fieldName, DataType type) {
                this.fieldName = fieldName;
                this.fieldType = field.getType();
                this.type = type;
                this.offset = unsafe.objectFieldOffset(field);
            }

            public UnsafeSetOp(Field field, String fieldName, DataType type, Class<?> objectClass) {
                this(field, fieldName, type);
                this.objectClass = objectClass;
            }

            public String getFieldName() {
                return fieldName;
            }

            public DataType getType() {
                return type;
            }

            public void set(Object object, Object value) {
                try {
                    if (fieldType.isPrimitive()) {
                        if (byte.class.isAssignableFrom(fieldType)) {
                            unsafe.putByte(object, offset, (byte) value);
                        } else if (short.class.isAssignableFrom(fieldType)) {
                            unsafe.putShort(object, offset, (short) value);
                        } else if (int.class.isAssignableFrom(fieldType)) {
                            unsafe.putInt(object, offset, (int) value);
                        } else if (long.class.isAssignableFrom(fieldType)) {
                            unsafe.putLong(object, offset, (long) value);
                        } else if (float.class.isAssignableFrom(fieldType)) {
                            unsafe.putFloat(object, offset, (float) value);
                        } else if (double.class.isAssignableFrom(fieldType)) {
                            unsafe.putDouble(object, offset, (double) value);
                        } else if (boolean.class.isAssignableFrom(fieldType)) {
                            unsafe.putBoolean(object, offset, (boolean) value);
                        }
                    } else {
                        unsafe.putObject(object, offset, value);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public Class<?> getObjectClass() {
                return objectClass;
            }
        }

        public static enum DataType {
            BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, STRING, BOOLEAN, OBJECT,
            BYTE_LIST, SHORT_LIST, INTEGER_LIST, LONG_LIST, FLOAT_LIST, DOUBLE_LIST, STRING_LIST, BOOLEAN_LIST, OBJECT_LIST,
            BYTE_SET, SHORT_SET, INTEGER_SET, LONG_SET, FLOAT_SET, DOUBLE_SET, STRING_SET, BOOLEAN_SET, OBJECT_SET,
            REFERENCE_HOLDER, SCHEMA_ID
        }

        /**
         * This class reads data from a byte array (representing an instance) and
         * keep track of a read index of where we are in the byte array.
         *
         * We could read data directly from the ByteBuf, but this is slower than
         * instead reading the whole buffer and operate on a byte array.
         */
        public static class ByteArrayReader {
            private byte[] data;
            private int idx;
            private int length;

            public ByteArrayReader(ByteBuf byteBuf) {


                byte[] data = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(data);
                this.data = data;
                this.length = data.length;
            }

            public int readByte() {
                return data[idx++];
            }

            public long readShort() {
                long value = getShort(data, idx);
                idx = idx + 2;
                return value;
            }

            public short getShort(final byte[] b, final int offset) {
                return (short) (b[offset] << 8 | b[offset + 1] & 0xFF);
            }

            private int readInt() {
                int value = getInt(data, idx);
                idx = idx + 4;
                return value;
            }

            private int getInt(final byte[] b, final int offset) {
                return (b[offset + 0] & 0xFF) << 24 | (b[offset + 1] & 0xFF) << 16
                        | (b[offset + 2] & 0xFF) << 8 | (b[offset + 3] & 0xFF) << 0;
            }

            public long readLong() {
                long value = getLong(data, idx);
                idx = idx + 8;
                return value;
            }

            private long getLong(final byte[] b, final int offset) {
                return (b[offset + 0] & 0xFFL) << 56 | (b[offset + 1] & 0xFFL) << 48
                        | (b[offset + 2] & 0xFFL) << 40 | (b[offset + 3] & 0xFFL) << 32
                        | (b[offset + 4] & 0xFFL) << 24 | (b[offset + 5] & 0xFFL) << 16
                        | (b[offset + 6] & 0xFFL) << 8 | (b[offset + 7] & 0xFFL) << 0;
            }

            public float readFloat() {
                return Float.intBitsToFloat(readInt());
            }

            public double readDouble() {
                return Double.longBitsToDouble(readLong());
            }

            public boolean readBoolean() {
                return readByte() != 0;
            }

            public String readString(int length) {
                byte[] bytes = new byte[length];
                System.arraycopy(data, idx, bytes, 0, length);
                idx = idx + length;
                return new String(bytes);
            }

            public boolean available() {
                return idx < length;
            }
        }
    }
}
