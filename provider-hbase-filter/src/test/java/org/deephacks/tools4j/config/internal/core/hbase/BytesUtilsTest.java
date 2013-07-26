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


import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.deephacks.confit.internal.hbase.BytesUtils.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class BytesUtilsTest {

    @org.junit.Test
    public void add() {
        int max = 10;
        byte[] bytes = createBytes(max);
        for (int i = 0; i < max; i++) {
            int idx = i * 4;
            int number = Bytes.getInt(new byte[] { bytes[idx + 0], bytes[idx + 1], bytes[idx + 2],
                    bytes[idx + 3] });
            assertThat(i, CoreMatchers.is(number));
        }
    }

    @org.junit.Test
    public void remove() {
        int max = 10;
        byte[] bytes = createBytes(max);
        List<Integer> shuffle = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            shuffle.add(i);
        }
        Collections.shuffle(shuffle);
        int length = bytes.length;
        for (int i = 0; i < max; i++) {
            bytes = BytesUtils.remove(bytes, shuffle.get(i));
            assertThat(bytes.length, CoreMatchers.is(length - (i * 4) - 4));
        }

    }

    // shuffle an array that should be sorted when done.
    public byte[] createBytes(int max) {
        List<Integer> shuffle = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            shuffle.add(i);
        }
        byte[] bytes = new byte[0];
        Collections.shuffle(shuffle);
        for (int i = 0; i < shuffle.size(); i++) {
            bytes = BytesUtils.add(bytes, shuffle.get(i));
        }
        return bytes;
    }
    @Test
    public void test_longs() {
        long[] l = new long[] {1, 2, 3};
        byte[] bytes = toBytes(l);
        assertArrayEquals(l, toLongs(bytes, 0, 3));
    }

    @Test
    public void test_ints() {
        int[] i = new int[] {1, 2, 3};
        byte[] bytes = toBytes(i);
        assertArrayEquals(i, toInts(bytes, 0, 3));
    }
    @Test
    public void test_shorts() {
        short[] s = new short[] {1, 2, 3};
        byte[] bytes = toBytes(s);
        assertArrayEquals(s, toShorts(bytes, 0, 3));

    }
    @Test
    public void test_bytes() {
        byte[] b = new byte[] {1, 2, 3};
        byte[] bytes = toBytes(b);
        assertArrayEquals(b, toBytes(bytes, 0, 3));
    }
    @Test
    public void test_doubles() {
        double[] d = new double[] {1.0, 2.0, 3.0};
        byte[] bytes = toBytes(d);
        assertArrayEquals(d, toDoubles(bytes, 0, 3), 0.1);

    }
    @Test
    public void test_floats() {
        float[] f = new float[] {1.0f, 2.0f, 3.0f};
        byte[] bytes = toBytes(f);
        assertArrayEquals(f, toFloats(bytes, 0, 3), 0.1f);
    }
    @Test
    public void test_booleans() {
        boolean[] b = new boolean[] {true, false, true};
        byte[] bytes = toBytes(b);
        assertTrue(Arrays.equals(b, toBooleans(bytes, 0, bytes.length)));
    }

    @Test
    public void test_strings() {
        String[] strings = new String[] {"a", "bb", "ccc"};
        byte[] bytes = toBytes(strings);
        String[] result = toStrings(bytes, 0);
        assertArrayEquals(strings, result);
    }

    @Test
    public void test_compareTo() {
        byte[] b1 = new byte[] {5, 6, 7};
        byte[] b2 = new byte[] {1, 2, 3, 4, 5, 6 ,7, 8, 9};
         assertThat(compareTo(b1, 0, b1.length, b2, 4, b1.length), is(0));
    }

    @Test
    public void test_write_byte() {
        byte value = 123;
        ByteArrayOutputStream bytes = write(value);
        Object result = read(bytes);
        assertEquals(value, result);
    }

    @Test
    public void test_write_short() {
        short value = 12321;
        ByteArrayOutputStream bytes = write(value);
        Object result = read(bytes);
        assertEquals(value, result);
    }

    @Test
    public void test_write_int() {
        int value = 12312134;
        ByteArrayOutputStream bytes = write(value);
        Object result = read(bytes);
        assertEquals(value, result);
    }

    @Test
    public void test_write_long() {
        long value = 1231231244214L;
        ByteArrayOutputStream bytes = write(value);
        Object result = read(bytes);
        assertEquals(value, result);
    }

    @Test
    public void test_write_float() {
        float value = 1128.128356f;
        ByteArrayOutputStream bytes = write(value);
        Object result = read(bytes);
        assertEquals(value, result);
    }

    @Test
    public void test_write_double() {
        double value = 11.12123483569712364;
        ByteArrayOutputStream bytes = write(value);
        Object result = read(bytes);
        assertEquals(value, result);
    }

    @Test
    public void test_write_boolean() {
        boolean value = true;
        ByteArrayOutputStream bytes = write(value);
        Object result = read(bytes);
        assertEquals(value, result);
    }

    @Test
    public void test_write_string() {
        String value = "abcdefghijklmnopqrstuvxyz";
        ByteArrayOutputStream bytes = write(value);
        Object result = read(bytes);
        assertEquals(value, result);
    }

    private Object read(ByteArrayOutputStream bytes) {
        DataInput in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        return BytesUtils.read(in);
    }

    private ByteArrayOutputStream write(Object value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        BytesUtils.write(out, value);
        return bytes;
    }

}
