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

import com.google.common.base.Strings;

import java.util.concurrent.ConcurrentHashMap;

public abstract class UniqueId {

    /** size of the id (in bytes). */
    protected final int width;
    /** if mapping between id and name should be cached */
    protected final boolean shouldCache;
    /** cache mapping name to id */
    private final ConcurrentHashMap<String, Long> idCache = new ConcurrentHashMap<>();
    /** cache mapping id to name */
    private final ConcurrentHashMap<Long, String> nameCache = new ConcurrentHashMap<>();

    public UniqueId(int width, boolean cache) {
        this.width = width;
        this.shouldCache = cache;
        if (width != 1 && width != 2 && width != 4 && width != 8) {
            throw new IllegalArgumentException(
                    "Width must fit in either byte, short, int or long [" + width + "].");
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

    public long getId(final String name) {
        Long id = idCache.get(name);
        if (id != null) {
            return id;
        } else {
            id = getIdFromStorage(name);
            if (shouldCache) {
                idCache.put(name, id);
                nameCache.put(id, name);
            }
        }
        return id;
    }

    public String getName(final long id) {
        String name = nameCache.get(id);
        if (!Strings.isNullOrEmpty(name)) {
            return name;
        } else {
            name = getNameFromStorage(id);
            if (shouldCache) {
                idCache.put(name, id);
                nameCache.put(id, name);
            }
            if (Strings.isNullOrEmpty(name)) {
                throw new IllegalStateException("Could not map id " + id + " to a name");
            }

        }
        return name;
    }

    protected long toValue(byte[] id) {
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

    protected abstract String getNameFromStorage(long id);

    protected abstract Long getIdFromStorage(String name);

}
