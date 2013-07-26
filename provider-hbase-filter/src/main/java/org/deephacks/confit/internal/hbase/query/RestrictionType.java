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
package org.deephacks.confit.internal.hbase.query;

import com.google.common.collect.Maps;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseBetween;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseEquals;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseGreaterThan;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseLessThan;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.HBaseStringContains;
import org.deephacks.confit.internal.hbase.query.RestrictionBuilder.QualifierRestriction;

import java.util.Map;

/**
 * Enumeration of all Restriction types that may be evaluated on the server-side (HBase RegionServer).
 *
 * Used during serialization and deserialization to pass Restriction between client and server.
 */
public enum RestrictionType {

    StringContains(HBaseStringContains.class),
    Equals(HBaseEquals.class),
    GreaterThan(HBaseGreaterThan.class),
    LessThan(HBaseLessThan.class),
    Between(HBaseBetween.class);

    RestrictionType(Class<? extends QualifierRestriction> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends QualifierRestriction> getRestrictionClass() {
        return clazz;
    }

    private final Class<? extends QualifierRestriction> clazz;

    private static final Map<Class<? extends QualifierRestriction>,RestrictionType> classToEnumMap = Maps.newHashMapWithExpectedSize(3);

    static {
        for (RestrictionType type : RestrictionType.values()) {
            classToEnumMap.put(type.clazz, type);
        }
    }

    public static RestrictionType valueOf(QualifierRestriction restriction) {
        RestrictionType type = classToEnumMap.get(restriction.getClass());
        if (type == null) {
            throw new IllegalArgumentException("No RestrictionType for " + restriction.getClass());
        }
        return type;
    }

    public QualifierRestriction newInstance() {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
