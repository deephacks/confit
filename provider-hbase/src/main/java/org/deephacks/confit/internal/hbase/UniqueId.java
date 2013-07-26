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

import com.google.common.base.Strings;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage lookup and creation of unique IDs. IDs are mapped to small unique ids,
 * in order to make hbase queries more efficient.
 *
 * IDs are can be looked up in HBase and cached forever, if needed.
 *
 * IDs can be of type:
 *
 *  - 1 byte      : 256 ids
 *  - 2 byte short: 65536 ids
 *  - 4 byte int  : 4 294 967 296 ids
 *  - 8 byte long : enough :)
 *
 * @author Kristoffer Sjogren
 */
public class UniqueId {

    /** mapping name to id. */
    public static final byte[] UID_NAME_FAMILY = "n".getBytes();
    /** mapping id to name. */
    public static final byte[] UID_ID_FAMILY = "i".getBytes();
    /** qualifier */
    private static final byte[] UID_QUALIFIER = "value".getBytes();
    /** size of the id (in bytes). */
    private final int width;
    /** Row key of the special row used to track the max ID already assigned. */
    private static final byte[] MAXID_ROW = { 0 };
    /** if mapping between id and name should be cached */
    private final boolean shouldCache;
    /** cache mapping name to id */
    private final ConcurrentHashMap<String, byte[]> idCache = new ConcurrentHashMap<String, byte[]>();
    /** cache mapping id to name */
    private final ConcurrentHashMap<String, String> nameCache = new ConcurrentHashMap<String, String>();
    /** hbase table */
    private final HTable htable;
    /** charset needed to correctly convert Strings to byte arrays and back. */
    private static final Charset CHARSET = Charset.forName("ISO-8859-1");

    public UniqueId(byte[] table, int width, Configuration conf, boolean cache) {
        this.width = width;
        this.shouldCache = cache;
        if (width != 1 && width != 2 && width != 4 && width != 8) {
            throw new IllegalArgumentException(
                    "Width must fit in either byte, short, int or long [" + width + "].");
        }
        try {
            this.htable = new HTable(conf, table);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getMaxWidth() {
        if (width == 1) {
            return new byte[] { -1 };
        } else if (width == 2) {
            return Bytes.fromShort((short) -1);
        } else if (width == 4) {
            return Bytes.fromInt(-1);
        } else {
            return Bytes.fromLong(-1L);
        }
    }

    public byte[] getMinWidth() {
        if (width == 1) {
            return new byte[] { 0 };
        } else if (width == 2) {
            return Bytes.fromShort((short) 0);
        } else if (width == 4) {
            return Bytes.fromInt(0);
        } else {
            return Bytes.fromLong(0);
        }
    }

    public byte[] getId(final String name) {
        byte[] id = idCache.get(name);
        if (id != null) {
            return id;
        } else {
            id = getIdFromHBase(name);
            if (shouldCache) {
                idCache.put(name, id);
                nameCache.put(new String(id, CHARSET), name);
            }
        }
        return id;
    }

    public String getName(final byte[] id) {
        String strid = new String(id, CHARSET);
        String name = nameCache.get(strid);
        if (!Strings.isNullOrEmpty(name)) {
            return name;
        } else {
            name = getNameFromHBase(id);
            if (shouldCache) {
                idCache.put(name, id);
                nameCache.put(strid, name);
            }
            if (Strings.isNullOrEmpty(name)) {
                throw new IllegalStateException("Could not map id " + Arrays.toString(id)
                        + " to a name from table: " + new String(htable.getTableName()));
            }

        }
        return name;
    }

    private String getNameFromHBase(byte[] id) {
        if (id.length != width) {
            throw new IllegalArgumentException("Cannot map name to ids other than (" + width
                    + " bytes).");
        }
        try {
            final Get getName = new Get(id);
            final Result r = htable.get(getName);
            byte[] name = r.getValue(UID_NAME_FAMILY, UID_QUALIFIER);
            if (name != null) {
                return new String(name, CHARSET);
            }
            throw new IllegalArgumentException("Failed mapping id [" + toValue(id) + "] to a name.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getIdFromHBase(String name) {
        byte[] id;
        try {
            final Get getId = new Get(name.getBytes(CHARSET));
            final Result r = htable.get(getId);
            id = r.getValue(UID_ID_FAMILY, UID_QUALIFIER);
            if (id != null) {
                return id;
            }
            final Get getMax = new Get(MAXID_ROW);
            final byte[] currentMaxId = htable.get(getMax).getValue(UID_ID_FAMILY, UID_QUALIFIER);
            id = increaseCounter(currentMaxId);

            // update counter
            final Put updateMax = new Put(MAXID_ROW);
            updateMax.add(UID_ID_FAMILY, UID_QUALIFIER, id);
            htable.put(updateMax);
            // add id
            final Put addId = new Put(id);
            addId.add(UID_NAME_FAMILY, UID_QUALIFIER, name.getBytes(CHARSET));
            htable.put(addId);
            // add name
            final Put addName = new Put(name.getBytes(CHARSET));
            addName.add(UID_ID_FAMILY, UID_QUALIFIER, id);
            htable.put(addName);
            htable.flushCommits();
            return id;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] increaseCounter(final byte[] currentMaxId) {

        if (width == 1) {
            byte counter;
            if (currentMaxId != null) {
                // increase counter
                counter = currentMaxId[0];
            } else {
                counter = 0;
            }
            counter++;

            return new byte[] { counter };
        } else if (width == 2) {
            short counter;
            if (currentMaxId != null) {
                // increase counter
                counter = Bytes.getShort(currentMaxId);
            } else {
                counter = 0;
            }
            counter++;

            return Bytes.fromShort(counter);
        } else if (width == 4) {
            int counter;
            if (currentMaxId != null) {
                // increase counter
                counter = Bytes.getInt(currentMaxId);
            } else {
                counter = 0;
            }
            counter++;
            return Bytes.fromInt(counter);
        } else if (width == 8) {
            long counter;
            if (currentMaxId != null) {
                // increase counter
                counter = Bytes.getLong(currentMaxId);
            } else {
                counter = 0;
            }
            counter++;
            return Bytes.fromLong(counter);
        } else {
            throw new IllegalArgumentException("Failure getting value, invalid byte length ["
                    + currentMaxId.length + "]");
        }
    }

    public long toValue(byte[] id) {
        if (id.length == 1) {
            return id[0];
        } else if (id.length == 2) {
            return Bytes.getShort(id);
        } else if (id.length == 4) {
            return Bytes.getInt(id);
        } else if (id.length == 8) {
            return Bytes.getLong(id);
        } else {
            throw new IllegalArgumentException("Failure getting value, invalid byte length ["
                    + id.length + "]");
        }
    }

}
