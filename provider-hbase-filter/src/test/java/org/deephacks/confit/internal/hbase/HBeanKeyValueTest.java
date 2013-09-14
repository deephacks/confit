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

import org.deephacks.confit.internal.hbase.BytesUtils.ReferenceList;
import org.deephacks.confit.internal.hbase.HBeanKeyValue.HBeanReader;
import org.deephacks.confit.internal.hbase.HBeanKeyValue.HBeanWriter;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class HBeanKeyValueTest {
    private int randomFieldsNum = 100;

    @Test
    public void test_random_write_read() throws Exception {
        for (int i = 0; i < 1000; i++) {
            ArrayList<Integer> ids = new ArrayList<>();
            HBeanWriter writer = new HBeanWriter();

            int stringId = new Random().nextInt();
            ids.add(stringId);
            String stringValue = RandomStringUtils.randomAlphanumeric(255);
            writer.putValue(stringId, stringValue);

            int booleanId = new Random().nextInt();
            ids.add(booleanId);
            boolean booleanValue = random(2) == 0;
            writer.putValue(booleanId, booleanValue);

            int longId = new Random().nextInt();
            ids.add(longId);
            long longValue = new Random().nextLong();
            writer.putValue(longId, longValue);

            int intId = new Random().nextInt();
            ids.add(intId);
            int intValue = new Random().nextInt();
            writer.putValue(intId, intValue);

            int shortId = new Random().nextInt();
            ids.add(shortId);
            short shortValue = (short) (new Random().nextInt() % Short.MAX_VALUE);
            writer.putValue(shortId, shortValue);

            int byteId = new Random().nextInt();
            ids.add(byteId);
            byte byteValue = (byte) (new Random().nextInt() % Byte.MAX_VALUE);
            writer.putValue(byteId, byteValue);

            int floatId = new Random().nextInt();
            ids.add(floatId);
            float floatValue = new Random().nextFloat();
            writer.putValue(floatId, floatValue);

            int doubleId = new Random().nextInt();
            ids.add(doubleId);
            double doubleValue = new Random().nextDouble();
            writer.putValue(doubleId, doubleValue);

            int referencesId = new Random().nextInt();
            ids.add(referencesId);
            Collection<String> instances = randomStrings();
            ReferenceList list = new ReferenceList(referencesId);
            list.getInstances().addAll(instances);
            writer.putValue(referencesId, list);

            int stringsId = new Random().nextInt();
            ids.add(stringsId);
            Collection<String> strings = randomStrings();
            writer.putValues(stringsId, strings, String.class);

            int booleansId = new Random().nextInt();
            ids.add(booleansId);
            Collection<Boolean> booleans = randomBooleans();
            writer.putValues(booleansId, booleans, Boolean.class);

            int longsId = new Random().nextInt();
            ids.add(longsId);
            Collection<Long> longs = randomLongs();
            writer.putValues(longsId, longs, Long.class);

            int intsId = new Random().nextInt();
            ids.add(intsId);
            Collection<Integer> integers = randomInts();
            writer.putValues(intsId, integers, Integer.class);

            int shortsId = new Random().nextInt();
            ids.add(shortsId);
            Collection<Short> shorts = randomShorts();
            writer.putValues(shortsId, shorts, Short.class);

            int bytesId = new Random().nextInt();
            ids.add(bytesId);
            Collection<Byte> bytes = randomBytes();
            writer.putValues(bytesId, bytes, Byte.class);

            int floatsId = new Random().nextInt();
            ids.add(floatsId);
            Collection<Float> floats = randomFloats();
            writer.putValues(floatsId, floats, Float.class);

            int doublesId = new Random().nextInt();
            ids.add(doublesId);
            Collection<Double> doubles = randomDoubles();
            writer.putValues(doublesId, doubles, Double.class);

            byte[] bean = writer.write();

            HBeanReader reader = new HBeanReader(bean);

            Integer[] idsArray = convert(reader.getIds(), Integer.class);
            assertThat(ids, hasItems(idsArray));

            assertEquals(booleanValue, reader.getValue(booleanId));

            assertEquals(longValue, reader.getValue(longId));

            assertEquals(intValue, reader.getValue(intId));

            assertEquals(shortValue, reader.getValue(shortId));

            assertEquals(byteValue, reader.getValue(byteId));

            assertEquals(doubleValue, reader.getValue(doubleId));

            assertEquals(floatValue, reader.getValue(floatId));

            ReferenceList referenceList = (ReferenceList) reader.getValue(referencesId);
            List<String> instanceList = referenceList.getInstances();
            assertThat(instances, hasItems((String[]) instanceList.toArray(new String[instanceList.size()])));

            Collection<String> collection = (Collection<String>) reader.getValue(stringsId);
            assertThat(strings, hasItems(collection.toArray(new String[collection.size()])));

            Boolean[] boolArray = ((Collection<Boolean>) reader.getValue(booleansId)).toArray(new Boolean[0]);
            assertThat(booleans, hasItems(boolArray));

            Long[] longArray = ((Collection<Long>) reader.getValue(longsId)).toArray(new Long[0]);
            assertThat(longs, hasItems(longArray));

            Integer[] intArray = ((Collection<Integer>) reader.getValue(intsId)).toArray(new Integer[0]);
            assertThat(integers, hasItems(intArray));

            Short[] shortArray = ((Collection<Short>) reader.getValue(shortsId)).toArray(new Short[0]);
            assertThat(shorts, hasItems(shortArray));

            Byte[] byteArray = ((Collection<Byte>) reader.getValue(bytesId)).toArray(new Byte[0]);
            assertThat(bytes, hasItems(byteArray));

            Double[] doubleArray = ((Collection<Double>)reader.getValue(doublesId)).toArray(new Double[0]);
            assertThat(doubles, hasItems(doubleArray));

            Float[] floatArray = ((Collection<Float>) reader.getValue(floatsId)).toArray(new Float[0]);
            assertThat(floats, hasItems(floatArray));
        }

    }
    public static <T> T[] convert(final Object array, Class<T> wrapperClass) {
        final int arrayLength = Array.getLength(array);
        final T[] result = (T[]) Array.newInstance(wrapperClass, arrayLength);
        for (int i = 0; i < arrayLength; i++) {
            Array.set(result, i, Array.get(array, i));
        }
        return result;
    }

    public Collection<String> randomStrings() {
        Collection<String> values = new ArrayList<>();
        for(int i = 0; i < random(randomFieldsNum); i++) {
            values.add(RandomStringUtils.randomAlphabetic(random(255)));
        }
        return values;
    }

    public Collection<Boolean> randomBooleans() {
        Collection<Boolean> values = new ArrayList<>();
        boolean[] possible = new boolean[] {true, false};
        for(int i = 0; i < random(randomFieldsNum); i++) {
            values.add(possible[random(2)]);
        }
        return values;
    }

    public Collection<Long> randomLongs() {
        Collection<Long> values = new ArrayList<>();
        for(int i = 0; i < random(randomFieldsNum); i++) {
            values.add(random(Long.MAX_VALUE));
        }
        return values;
    }

    public Collection<Integer> randomInts() {
        Collection<Integer> values = new ArrayList<>();
        for(int i = 0; i < random(randomFieldsNum); i++) {
            values.add(random(Integer.MAX_VALUE));
        }
        return values;
    }

    public Collection<Short> randomShorts() {
        Collection<Short> values = new ArrayList<>();
        for(int i = 0; i < random(randomFieldsNum); i++) {
            values.add((short) random(Integer.MAX_VALUE));
        }
        return values;
    }

    public Collection<Byte> randomBytes() {
        Collection<Byte> values = new ArrayList<>();
        for(int i = 0; i < random(randomFieldsNum); i++) {
            values.add((byte) random(Integer.MAX_VALUE));
        }
        return values;
    }

    public Collection<Float> randomFloats() {
        Collection<Float> values = new ArrayList<>();
        for(int i = 0; i < random(randomFieldsNum); i++) {
            values.add(new Random().nextFloat());
        }
        return values;
    }

    public Collection<Double> randomDoubles() {
        Collection<Double> values = new ArrayList<>();
        for(int i = 0; i < random(randomFieldsNum); i++) {
            values.add(new Random().nextDouble());
        }
        return values;
    }

    public int random(int max) {
        return Math.abs(new Random().nextInt()) % max;
    }

    public long random(long max) {
        return Math.abs(new Random().nextLong()) % max;
    }

}

