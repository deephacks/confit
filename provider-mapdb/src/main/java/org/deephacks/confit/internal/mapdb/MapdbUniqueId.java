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
package org.deephacks.confit.internal.mapdb;

import org.deephacks.confit.serialization.UniqueIds;
import org.deephacks.confit.spi.Lookup;
import org.mapdb.TxMaker;

public class MapdbUniqueId extends UniqueIds {
    private MapDB mapDB;

    public MapdbUniqueId() {
        super(true);
        this.mapDB = new MapDB(Lookup.get().lookup(TxMaker.class));
    }

    @Override
    protected String getSchemaNameFromStorage(int id) {
        return mapDB.getNameFromStorage(id);
    }

    @Override
    protected int getSchemaIdFromStorage(String name) {
        return mapDB.getIdFromStorage(name).intValue();
    }

    @Override
    protected String getInstanceNameFromStorage(long id) {
        return mapDB.getNameFromStorage(id);
    }

    @Override
    protected long getInstanceIdFromStorage(String name) {
        return mapDB.getIdFromStorage(name);
    }
}
