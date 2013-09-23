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

import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HBeanRowCollector is responsible for collecting references when traversing
 * references eagerly and detecting circular references.
 */
public class HBeanRowCollector {
    /** the inital rows that was queried for */
    private final Map<HBeanRow, HBeanRow> inital = new HashMap<>();
    /** collected references  */
    private final Map<HBeanRow, HBeanRow> references = new HashMap<>();

    /**
     * We need to keep track of the intial query that was made.
     */
    public HBeanRowCollector(final Set<HBeanRow> rows) {
        for (HBeanRow row : rows) {
            inital.put(row, row);
        }
    }

    public void addReferences(Set<HBeanRow> refs) {
        for (HBeanRow row : refs) {
            if (!inital.containsKey(row)) {
                references.put(row, row);
            }
        }
    }

    /**
     * Filter out the rows that we have not yet visited.
     */
    public Set<HBeanRow> filterUnvisted(Set<HBeanRow> rows) {
        Set<HBeanRow> unvisted = new HashSet<>();
        for (HBeanRow row : rows) {
            if (!references.containsKey(row) && !inital.containsKey(row)) {
                unvisted.add(row);
            }
        }
        return unvisted;
    }

    /**
     * Convert the collected rows into a hierarchy of beans where
     * list references are initalized.
     */
    public List<Bean> getBeans() {
        Map<BeanId, Bean> referenceMap = new HashMap<>();
        List<Bean> result = new ArrayList<>();

        for (HBeanRow row : inital.keySet()) {
            Bean bean = row.getBean();
            result.add(bean);
            referenceMap.put(bean.getId(), bean);
        }
        for (HBeanRow row : references.keySet()) {
            Bean bean = row.getBean();
            referenceMap.put(bean.getId(), bean);
        }
        for (Bean bean : referenceMap.values()) {
            for (BeanId id : bean.getReferences()) {
                Bean ref = referenceMap.get(id);
                id.setBean(ref);
            }
        }
        return result;
    }

    public List<Bean> getAllBeans() {
        Map<BeanId, Bean> referenceMap = new HashMap<>();
        List<Bean> result = new ArrayList<>();

        for (HBeanRow row : inital.keySet()) {
            Bean bean = row.getBean();
            result.add(bean);
            referenceMap.put(bean.getId(), bean);
        }
        for (HBeanRow row : references.keySet()) {
            Bean bean = row.getBean();
            referenceMap.put(bean.getId(), bean);
            result.add(bean);
        }
        for (Bean bean : referenceMap.values()) {
            for (BeanId id : bean.getReferences()) {
                Bean ref = referenceMap.get(id);
                id.setBean(ref);
            }
        }
        return result;
    }

    public Set<HBeanRow> getRows() {
        return inital.keySet();
    }

    public Map<HBeanRow, HBeanRow> getRowMap() {
        return inital;
    }

}
