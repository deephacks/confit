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
package org.deephacks.confit.internal.jpa;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Beans are connected by references as a graph.
 *
 * Query for beans begin by finding references for each level (i.e depth)
 * of beans. When list references are found, a final query for list bean's
 * properties is made.
 *
 * The purpose of this class is to both collect and cache data from these
 * queries.
 *
 * This helps removing redundant queries and assembling properties and
 * references into (a graph of) beans when list queries are finished.
 *
 * @author Kristoffer Sjogren
 */
public class JpaBeanQueryAssembler {
    /**
     * Contains the bean ids of the beans that were queries for.
     */
    private Set<BeanId> beansQuery = new HashSet<>();
    /**
     * Contain list beans references.
     */
    private Multimap<BeanId, JpaRef> refs = ArrayListMultimap.create();
    /**
     * Contain list beans that have been found as part of the query.
     */
    private Map<BeanId, Bean> beans = new HashMap<>();

    /**
     * Beans we want to find and initalize.
     */
    public JpaBeanQueryAssembler(Set<BeanId> query) {
        this.beansQuery = query;
    }

    /**
     * Add properties to appropriate bean found from a partial
     * query of bean properties.
     */
    public void addProperties(List<JpaProperty> queryProperties) {
        for (JpaProperty prop : queryProperties) {
            Bean bean = putIfAbsent(prop.getId());
            if (!JpaProperty.BEAN_MARKER_PROPERTY_NAME.equals(prop.getPropertyName())) {
                bean.addProperty(prop.getPropertyName(), prop.getValue());
            }
        }
    }

    /**
     * Add references found from a partial query of bean references.
     */
    public void addRefs(Multimap<BeanId, JpaRef> queryRefs) {
        refs.putAll(queryRefs);
        for (BeanId id : refs.keySet()) {
            putIfAbsent(id);
            for (JpaRef ref : refs.get(id)) {
                putIfAbsent(ref.getTarget());
            }
        }
    }

    /**
     * Add references found from a partial query of bean references.
     */
    public void addRefs(Set<BeanId> queryRefs) {
        for (BeanId id : queryRefs) {
            putIfAbsent(id);
        }
    }

    /**
     * Return list bean ids that the assembler is currently aware of.
     */
    public Set<BeanId> getIds() {
        Set<BeanId> ids = new HashSet<>();
        ids.addAll(beansQuery);
        ids.addAll(beans.keySet());
        return ids;
    }

    /**
     * Check if the assembler already is aware of a particular
     * bean.
     */
    public boolean contains(BeanId id) {
        return beans.containsKey(id);
    }

    /**
     * Add a empty bean based on id if it does not already exist.
     */
    private Bean putIfAbsent(BeanId id) {
        Bean bean = beans.get(id);
        if (bean == null) {
            bean = Bean.create(id);
            beans.put(id, bean);
        }
        return bean;
    }

    /**
     * Assemble beans and initalize their properties and references
     * from what have been provided.
     *
     * @return the beans that were provided in the inital query.
     */
    public List<Bean> assembleBeans() {
        connectReferences();

        if (beansQuery.size() == 0) {
            // if no specific beans where requested (such as query for a
            // specific schema) return what is available.
            return new ArrayList<>(beans.values());
        }
        List<Bean> initalQuery = new ArrayList<>();
        for (BeanId id : beansQuery) {
            Bean bean = beans.get(id);
            if (bean != null) {
                initalQuery.add(beans.get(id));
            }
        }
        return initalQuery;
    }

    /**
     * Assemble beans and initalize references from what have been provided.
     */
    private void connectReferences() {
        // ready to associate initalized beans with references
        for (Bean bean : beans.values()) {
            for (JpaRef ref : refs.get(bean.getId())) {
                BeanId target = ref.getTarget();
                Bean targetBean = beans.get(target);
                target.setBean(targetBean);
                bean.addReference(ref.getPropertyName(), target);
            }
        }
    }
}
