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

import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedBytes;
import sun.misc.Unsafe;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BytesUtils {

    /**
     * Adds a big-endian 4-byte integer to a sorted array of bytes.
     *
     * @param arr This byte array is assumed to be sorted array
     *            of signed ints.
     * @param n value to add.
     * @return new array with the added value.
     */
    public static byte[] add(byte[] arr, int n) {
        int index = binarySearch(arr, n);
        byte[] arr2 = new byte[arr.length + 4];
        System.arraycopy(arr, 0, arr2, 0, index);
        System.arraycopy(arr, index, arr2, index + 4, arr.length - index);
        org.deephacks.confit.internal.hbase.Bytes.setInt(arr2, n, index);
        return arr2;
    }

    /**
     * Remove a big-endian 4-byte integer to a sorted array of bytes.
     *
     * @param arr This byte array is assumed to be sorted array
     *            of signed ints.
     * @param n value to remove.
     * @return new array with the added value.
     */
    public static byte[] remove(byte[] arr, int n) {
        int index = binarySearch(arr, n);
        byte[] arr2 = new byte[arr.length - 4];
        System.arraycopy(arr, 0, arr2, 0, index);
        System.arraycopy(arr, index + 4, arr2, index, arr.length - index - 4);
        return arr2;
    }

    /**
     * Search for a big-endian 4-byte integer in a array of bytes.
     *
     * @param a array of containing only big-endian 4-byte integers.
     * @param key the value to seach for.
     * @return the index found.
     */
    public static int binarySearch(byte[] a, int key) {
        int low = 0;
        int high = a.length;

        while (low < high) {
            int mid = (low + high) >>> 1;
            if (mid % 4 != 0) {
                if (high == a.length) {
                    mid = low;
                } else {
                    mid = high;
                }
            }
            int midVal = getInt(a, mid);

            if (midVal < key)
                low = mid + 4;
            else if (midVal > key)
                high = mid - 4;
            else
                return mid; // key found
        }
        if (low == a.length) {
            return low;
        }
        return key > getInt(a, low) ? low + 4 : low;

    }

    public static void write(DataOutput out, Object value) {
        try {
            if (value instanceof Byte) {
                out.write(DataType.BYTE.getId());
                out.write((byte) value);
            } else if (value instanceof Short) {
                out.write(DataType.SHORT.getId());
                out.writeShort((short) value);
            } else if (value instanceof Integer) {
                out.write(DataType.INTEGER.getId());
                out.writeInt((int) value);
            } else if (value instanceof Long) {
                out.write(DataType.LONG.getId());
                out.writeLong((long) value);
            } else if (value instanceof Float) {
                out.write(DataType.FLOAT.getId());
                out.writeFloat((float) value);
            } else if (value instanceof Double) {
                out.write(DataType.DOUBLE.getId());
                out.writeDouble((double) value);
            } else if (value instanceof Boolean) {
                out.write(DataType.BOOLEAN.getId());
                out.writeBoolean((boolean) value);
            } else if (value instanceof String) {
                out.write(DataType.STRING.getId());
                out.writeUTF((String) value);
            } else {
                throw new UnsupportedOperationException("Did not recognize type " + value.getClass());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Object read(DataInput in)  {
        try {
            DataType type = DataType.getDataType(in.readByte());
            switch (type) {
                case BYTE: return in.readByte();
                case SHORT: return in.readShort();
                case INTEGER: return in.readInt();
                case LONG: return in.readLong();
                case FLOAT: return in.readFloat();
                case DOUBLE: return in.readDouble();
                case BOOLEAN: return in.readBoolean();
                case STRING: return in.readUTF();
                case BYTE_LIST: throw new UnsupportedOperationException("Did not recognize type " + type);
                case SHORT_LIST: throw new UnsupportedOperationException("Did not recognize type " + type);
                case INTEGER_LIST: throw new UnsupportedOperationException("Did not recognize type " + type);
                case LONG_LIST: throw new UnsupportedOperationException("Did not recognize type " + type);
                case FLOAT_LIST: throw new UnsupportedOperationException("Did not recognize type " + type);
                case DOUBLE_LIST: throw new UnsupportedOperationException("Did not recognize type " + type);
                case BOOLEAN_LIST: throw new UnsupportedOperationException("Did not recognize type " + type);
                case STRING_LIST: throw new UnsupportedOperationException("Did not recognize type " + type);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        throw new UnsupportedOperationException("Did not recognize type");
    }

    public static byte[] toBytes(long[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * 8);
        for (int i = 0; i < values.length; i++){
            buf.putLong(i * 8, values[i]);
        }
        return buf.array();
    }

    public static byte[] toBytes(int[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * 4);
        for (int i = 0; i < values.length; i++){
            buf.putInt(i * 4, values[i]);
        }
        return buf.array();
    }

    public static byte[] toBytes(short[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * 2);
        for (int i = 0; i < values.length; i++){
            buf.putShort(i * 2, values[i]);
        }
        return buf.array();
    }

    public static byte[] toBytes(byte[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length);
        for (int i = 0; i < values.length; i++){
            buf.put(i, values[i]);
        }
        return buf.array();
    }

    public static byte[] toBytes(boolean[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length);
        for (int i = 0; i < values.length; i++){
            byte val = values[i] ? (byte) 1 : (byte) 0;
            buf.put(i, val);
        }
        return buf.array();
    }

    public static byte[] toBytes(float[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * 4);
        for (int i = 0; i < values.length; i++){
            buf.putFloat(i * 4, values[i]);
        }
        return buf.array();
    }

    public static byte[] toBytes(double[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * 8);
        for (int i = 0; i < values.length; i++){
            buf.putDouble(i * 8, values[i]);
        }
        return buf.array();
    }

    public static byte[] toBytes(String[] strings) {
        List<byte[]> stringBytes = new ArrayList<>();
        int size = 0;
        for (String str : strings) {
            byte[] bytes = str.getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
            stringBytes.add(buffer.array());
            size += 4 + bytes.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(size + 4);
        buffer.putInt(strings.length);
        for (byte[] bytes : stringBytes) {
            buffer.put(bytes);
        }
        return buffer.array();
    }
    public static byte[] toBytes(Reference reference) {
        byte[] bytes = reference.getInstance().getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4 + 4);
        buffer.putInt(reference.getSid());
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        return buffer.array();
    }

    public static byte[] toBytes(ReferenceList reference) {
        List<byte[]> stringBytes = new ArrayList<>();
        int size = 0;
        for (String str : reference.getInstances()) {
            byte[] bytes = str.getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
            stringBytes.add(buffer.array());
            size += 4 + bytes.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(size + 4 + 4);
        buffer.putInt(reference.getInstances().size());
        buffer.putInt(reference.getSid());
        for (byte[] bytes : stringBytes) {
            buffer.put(bytes);
        }
        return buffer.array();
    }

    public static Reference toReference(byte[] value, int offset) {
        Reference reference = new Reference(getInt(value, offset));
        int size = getInt(value, offset);
        byte[] bytes = new byte[size];
        System.arraycopy(value, offset + 4, bytes, 0, size);
        reference.setInstance(new String(bytes));
        return reference;
    }

    public static ReferenceList toReferences(byte[] value, int offset) {
        int length = getInt(value, offset);
        return toReferences(value, offset + 4, length);
    }

    public static ReferenceList toReferences(byte[] value, int offset, int length) {
        ReferenceList reference = new ReferenceList(getInt(value, offset));
        offset += 4;
        for (int i = 0; i < length; i++) {
            int size = getInt(value, offset);
            byte[] bytes = new byte[size];
            System.arraycopy(value, offset + 4, bytes, 0, size);
            reference.addInstance(new String(bytes));
            offset += 4 + size;
        }
        return reference;
    }

    public static boolean[] toBooleans(byte[] value, int offset, int num) {
        boolean[] values = new boolean[num];
        int idx = 0;
        for (int i = offset; i < num + offset; i++) {
            values[idx++] = getBoolean(value[i]);
        }
        return values;
    }

    public static boolean[] toBooleans(byte[] value, int offset) {
        int length = getInt(value, offset);
        return toBooleans(value, offset + 4, length);
    }

    public static List<Boolean> toBooleanList(byte[] value, int offset) {
        int length = getInt(value, offset);
        boolean[] values = toBooleans(value, offset + 4, length);
        ArrayList<Boolean> list = new ArrayList<>();
        for (Boolean v : values) {
            list.add(v);
        }
        return list;
    }

    public static long[] toLongs(byte[] value, int offset, int num) {
        long[] values = new long[num];
        int idx = 0;
        for (int i = offset; i < offset + (num * 8); i += 8) {
            values[idx++] = getLong(value, i);
        }
        return values;
    }

    public static long[] toLongs(byte[] value, int offset) {
        int length = getInt(value, offset);
        return toLongs(value, offset + 4, length);
    }

    public static List<Long> toLongList(byte[] value, int offset) {
        int length = getInt(value, offset);
        long[] values = toLongs(value, offset + 4, length);
        ArrayList<Long> list = new ArrayList<>();
        for (Long v : values) {
            list.add(v);
        }
        return list;
    }

    public static int[] toInts(byte[] value, int offset, int num) {
        int[] values = new int[num];
        int idx = 0;
        for (int i = offset; i < offset + (num * 4); i += 4) {
            values[idx++] = getInt(value, i);
        }
        return values;
    }

    public static int[] toInts(byte[] value, int offset) {
        int length = getInt(value, offset);
        return toInts(value, offset + 4, length);
    }

    public static List<Integer> toIntList(byte[] value, int offset) {
        int length = getInt(value, offset);
        int[] values = toInts(value, offset + 4, length);
        ArrayList<Integer> list = new ArrayList<>();
        for (Integer v : values) {
            list.add(v);
        }
        return list;
    }

    public static short[] toShorts(byte[] value, int offset, int num) {
        short[] values = new short[num];
        int idx = 0;
        for (int i = offset; i < offset + (num * 2); i += 2) {
            values[idx++] = getShort(value, i);
        }
        return values;
    }

    public static short[] toShorts(byte[] value, int offset) {
        int length = getInt(value, offset);
        return toShorts(value, offset + 4, length);
    }

    public static List<Short> toShortList(byte[] value, int offset) {
        int length = getInt(value, offset);
        short[] values = toShorts(value, offset + 4, length);
        ArrayList<Short> list = new ArrayList<>();
        for (short v : values) {
            list.add(v);
        }
        return list;
    }

    public static byte[] toBytes(byte[] value, int offset, int num) {
        byte[] values = new byte[num];
        int idx = 0;
        for (int i = offset; i < offset + num; i++) {
            values[idx++] = value[i];
        }
        return values;
    }

    public static byte[] toBytes(byte[] value, int offset) {
        int length = getInt(value, offset);
        return toBytes(value, offset + 4, length);
    }

    public static List<Byte> toByteList(byte[] value, int offset) {
        int length = getInt(value, offset);
        byte[] bytes = toBytes(value, offset + 4, length);
        ArrayList<Byte> list = new ArrayList<>();
        for (byte v : bytes) {
            list.add(v);
        }
        return list;
    }

    public static double[] toDoubles(byte[] value, int offset, int num) {
        double[] values = new double[num];
        int idx = 0;
        for (int i = offset; i < offset + (num * 8); i += 8) {
            values[idx++] = getDouble(value, i);
        }
        return values;
    }

    public static double[] toDoubles(byte[] value, int offset) {
        int length = getInt(value, offset);
        return toDoubles(value, offset + 4, length);
    }

    public static List<Double> toDoubleList(byte[] value, int offset) {
        int length = getInt(value, offset);
        double[] values = toDoubles(value, offset + 4, length);
        ArrayList<Double> list = new ArrayList<>();
        for (double v : values) {
            list.add(v);
        }
        return list;
    }


    public static float[] toFloats(byte[] value, int offset, int num) {
        float[] values = new float[num];
        int idx = 0;
        for (int i = offset; i < offset + (num * 4); i += 4) {
            values[idx++] = getFloat(value, i);
        }
        return values;

    }

    public static float[] toFloats(byte[] value, int offset) {
        int length = getInt(value, offset);
        return toFloats(value, offset + 4, length);
    }

    public static List<Float> toFloatList(byte[] value, int offset) {
        int length = getInt(value, offset);
        float[] values = toFloats(value, offset + 4, length);
        ArrayList<Float> list = new ArrayList<>();
        for (float v : values) {
            list.add(v);
        }
        return list;
    }

    public static String[] toStrings(byte[] value, int offset, int length) {
        ArrayList<String> values = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            int size = getInt(value, offset);
            byte[] bytes = new byte[size];
            System.arraycopy(value, offset + 4, bytes, 0, size);
            values.add(new String(bytes));
            offset += 4 + size;
        }
        return values.toArray(new String[values.size()]);
    }

    public static String[] toStrings(byte[] value, int offset) {
        int length = getInt(value, offset);
        return toStrings(value, offset + 4, length);
    }

    public static List<String> toStringList(byte[] value, int offset) {
        int length = getInt(value, offset);
        String[] values = toStrings(value, offset + 4, length);
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, values);
        return list;
    }

    public static long getLong(final byte[] b, final int offset) {
        return (b[offset + 0] & 0xFFL) << 56 | (b[offset + 1] & 0xFFL) << 48
                | (b[offset + 2] & 0xFFL) << 40 | (b[offset + 3] & 0xFFL) << 32
                | (b[offset + 4] & 0xFFL) << 24 | (b[offset + 5] & 0xFFL) << 16
                | (b[offset + 6] & 0xFFL) << 8 | (b[offset + 7] & 0xFFL) << 0;
    }

    public static int getInt(final byte[] b, final int offset) {
        return (b[offset + 0] & 0xFF) << 24 | (b[offset + 1] & 0xFF) << 16
                | (b[offset + 2] & 0xFF) << 8 | (b[offset + 3] & 0xFF) << 0;
    }

    public static short getShort(final byte[] b, final int offset) {
        return (short) (b[offset] << 8 | b[offset + 1] & 0xFF);
    }

    public static float getFloat(final byte[] b, final int offset) {
        return Float.intBitsToFloat(getInt(b, offset));
    }

    public static double getDouble(final byte[] b, final int offset) {
        return Double.longBitsToDouble(getLong(b, offset));
    }

    public static String getString(final byte[] b, final int offset) {
        int length = getInt(b, offset);
        byte[] bytes = new byte[length];
        System.arraycopy(b, offset + 4, bytes, 0, length);
        return new String(bytes);
    }

    private static boolean getBoolean(byte b) {
        return b != 0;
    }


    static final Unsafe theUnsafe;

    /** The offset to the first element in a byte array. */
    static final int BYTE_ARRAY_BASE_OFFSET;

    static {
        theUnsafe = (Unsafe) AccessController.doPrivileged(
                new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        try {
                            Field f = Unsafe.class.getDeclaredField("theUnsafe");
                            f.setAccessible(true);
                            return f.get(null);
                        } catch (NoSuchFieldException e) {
                            // It doesn't matter what we throw;
                            // it's swallowed in getBestComparer().
                            throw new Error();
                        } catch (IllegalAccessException e) {
                            throw new Error();
                        }
                    }
                });

        BYTE_ARRAY_BASE_OFFSET = theUnsafe.arrayBaseOffset(byte[].class);

        // sanity check - this should never fail
        if (theUnsafe.arrayIndexScale(byte[].class) != 1) {
            throw new AssertionError();
        }
    }
    /**
     * Lexicographically compare two arrays.
     *
     * @param buffer1 left operand
     * @param buffer2 right operand
     * @param offset1 Where to start comparing in the left buffer
     * @param offset2 Where to start comparing in the right buffer
     * @param length1 How much to compare from the left buffer
     * @param length2 How much to compare from the right buffer
     * @return 0 if equal, < 0 if left is less than right, etc.
     */
    public static int compareTo(byte[] buffer1, int offset1, int length1,
                         byte[] buffer2, int offset2, int length2) {
        // Short circuit equal case
        if (buffer1 == buffer2 &&
                offset1 == offset2 &&
                length1 == length2) {
            return 0;
        }
        int minLength = Math.min(length1, length2);
        int minWords = minLength / Longs.BYTES;
        int offset1Adj = offset1 + BYTE_ARRAY_BASE_OFFSET;
        int offset2Adj = offset2 + BYTE_ARRAY_BASE_OFFSET;

        /*
         * Compare 8 bytes at a time. Benchmarking shows comparing 8 bytes at a
         * time is no slower than comparing 4 bytes at a time even on 32-bit.
         * On the other hand, it is substantially faster on 64-bit.
         */
        for (int i = 0; i < minWords * Longs.BYTES; i += Longs.BYTES) {
            long lw = theUnsafe.getLong(buffer1, offset1Adj + (long) i);
            long rw = theUnsafe.getLong(buffer2, offset2Adj + (long) i);
            long diff = lw ^ rw;

            if (diff != 0) {
                if (!littleEndian) {
                    return lessThanUnsigned(lw, rw) ? -1 : 1;
                }

                // Use binary search
                int n = 0;
                int y;
                int x = (int) diff;
                if (x == 0) {
                    x = (int) (diff >>> 32);
                    n = 32;
                }

                y = x << 16;
                if (y == 0) {
                    n += 16;
                } else {
                    x = y;
                }

                y = x << 8;
                if (y == 0) {
                    n += 8;
                }
                return (int) (((lw >>> n) & 0xFFL) - ((rw >>> n) & 0xFFL));
            }
        }

        // The epilogue to cover the last (minLength % 8) elements.
        for (int i = minWords * Longs.BYTES; i < minLength; i++) {
            int result = UnsignedBytes.compare(
                    buffer1[offset1 + i],
                    buffer2[offset2 + i]);
            if (result != 0) {
                return result;
            }
        }
        return length1 - length2;
    }
    static final boolean littleEndian =
            ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);

    /**
     * Returns true if x1 is less than x2, when both values are treated as
     * unsigned.
     */
    static boolean lessThanUnsigned(long x1, long x2) {
        return (x1 + Long.MIN_VALUE) < (x2 + Long.MIN_VALUE);
    }

    public static enum DataType {
        BYTE(1), SHORT(2), INTEGER(3), LONG(4), FLOAT(5), DOUBLE(6), BOOLEAN(7), STRING(8), REFERENCE(9),
        BYTE_LIST(11), SHORT_LIST(12), INTEGER_LIST(13), LONG_LIST(14), FLOAT_LIST(15),
        DOUBLE_LIST(16), BOOLEAN_LIST(17), STRING_LIST(18), REFERENCE_LIST(19);
        private int id;

        DataType(int id) {
            this.id = id;
        }
        private static final Map<Byte, DataType> idToEnumMap = new HashMap<>();

        static {
            for (DataType type : DataType.values()) {
                idToEnumMap.put(type.getId(), type);
            }
        }

        public byte getId() {
            return (byte) id;
        }

        public static DataType getDataType(byte id) {
            return idToEnumMap.get(id);
        }

        public static DataType getDataType(Class<?> cls) {
            if(Byte.class.isAssignableFrom(cls)) {
                return BYTE;
            } else if (Short.class.isAssignableFrom(cls)) {
                return SHORT;
            } else if (Integer.class.isAssignableFrom(cls)) {
                return INTEGER;
            } else if (Long.class.isAssignableFrom(cls)) {
                return LONG;
            } else if (Float.class.isAssignableFrom(cls)) {
                return FLOAT;
            } else if (Double.class.isAssignableFrom(cls)) {
                return DOUBLE;
            } else if (Boolean.class.isAssignableFrom(cls)) {
                return BOOLEAN;
            } else if (String.class.isAssignableFrom(cls)) {
                return STRING;
            } else if (Reference.class.isAssignableFrom(cls)) {
                return REFERENCE;
            } else if (ReferenceList.class.isAssignableFrom(cls)) {
                return REFERENCE_LIST;
            } else {
                throw new UnsupportedOperationException("Did not recognize " + cls);
            }
        }
    }
    public static class ReferenceList {
        private int sid;
        private ArrayList<String> instances = new ArrayList<>();
        public ReferenceList(int sid) {
            this.sid = sid;
        }

        public int getSid() {
            return sid;
        }

        public void addInstance(String instance) {
            instances.add(instance);
        }

        public List<String> getInstances() {
            return instances;
        }
    }

    public static class Reference {
        private int sid;
        private String instance;

        public Reference(int sid) {
            this.sid = sid;
        }

        public Reference(int sid, String instance) {
            this.sid = sid;
            this.instance = instance;
        }

        public int getSid() {
            return sid;
        }

        public void setInstance(String instance) {
            this.instance = instance;
        }

        public String getInstance() {
            return instance;
        }
    }
}
