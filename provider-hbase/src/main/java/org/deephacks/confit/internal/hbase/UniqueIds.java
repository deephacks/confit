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

/**
 * Holder class for lookup of instance ids, schema and property names.
 */
public class UniqueIds {
    /** maps bean instance id to unique number */
    private UniqueId uiid;

    /** maps bean schema name to unique number */
    private UniqueId usid;

    /** maps bean property name to unique number */
    private UniqueId upid;

    public UniqueIds(UniqueId uiid, UniqueId usid, UniqueId upid) {
        this.uiid = uiid;
        this.usid = usid;
        this.upid = upid;
    }

    /**
     * @return unique id for schema names.
     */
    public UniqueId getUsid() {
        return usid;
    }
    /**
     * @return unique id for bean instance ids.
     */
    public UniqueId getUiid() {
        return uiid;
    }

    /**
     * @return unique id for bean property names.
     */
    public UniqueId getUpid() {
        return upid;
    }
}
