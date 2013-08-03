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
package org.deephacks.confit.internal.core.yaml;


import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.PropertyManager;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.deephacks.confit.model.Events.*;

public class YamlBeanManager extends BeanManager {
    static final String YAML_BEAN_FILE_STORAGE_DIR_PROP = "config.spi.bean.yaml.dir";
    static final String YAML_BEAN_FILE_NAME = "bean.yaml";
    static final String YAML_EMPTY_FILE = "{}";
    static File file;
    private static Yaml yaml = new Yaml();

    public YamlBeanManager() {
        PropertyManager propertyManager = PropertyManager.lookup();
        propertyManager.get(YAML_BEAN_FILE_STORAGE_DIR_PROP);
        String dirValue = propertyManager.get(YAML_BEAN_FILE_STORAGE_DIR_PROP).or(System.getProperty("java.io.tmpdir"));
        file = new File(new File(dirValue), YAML_BEAN_FILE_NAME);
    }

    @Override
    public Optional<Bean> getEager(BeanId id) {
        Map<BeanId, Bean> all = readValuesAsMap();
        return getEagerly(id, all);
    }

    private Optional<Bean> getEagerly(BeanId id, Map<BeanId, Bean> all) {
        Bean result = all.get(id);
        if (result == null) {
            return Optional.absent();
        }
        // bean found, initalize references.
        for (BeanId ref : result.getReferences()) {
            if (ref.getBean() != null) {
                continue;
            }
            Bean refBean = all.get(ref);
            if (refBean == null) {
                throw CFG301_MISSING_RUNTIME_REF(result.getId(), ref);
            }
            ref.setBean(refBean);
            getEagerly(ref, all);
        }
        return Optional.of(result);
    }

    @Override
    public Optional<Bean> getLazy(BeanId id) throws AbortRuntimeException {
        Map<BeanId, Bean> all = readValuesAsMap();
        Bean bean = all.get(id);
        if (bean == null) {
            return Optional.absent();
        }
        for (BeanId ref : bean.getReferences()) {
            Bean refBean = all.get(ref);
            if (refBean == null) {
                throw CFG301_MISSING_RUNTIME_REF(ref);
            }
            ref.setBean(refBean);
        }
        return Optional.of(bean);
    }

    /**
     * The direct, but no further, successors that references this bean will also be
     * fetched and initalized with their direct, but no further, predecessors.
     */
    @Override
    public Map<BeanId, Bean> getBeanToValidate(Collection<Bean> beans) throws AbortRuntimeException {
        Map<BeanId, Bean> beansToValidate = new HashMap<>();
        for (Bean bean : beans) {
            Map<BeanId, Bean> predecessors = new HashMap<>();
            // beans read from xml storage will only have their basic properties initalized...
            Map<BeanId, Bean> all = readValuesAsMap();
            // ... but we also need set the direct references/predecessors for beans to validate
            Map<BeanId, Bean> beansToValidateSubset = getDirectSuccessors(bean, all);
            beansToValidateSubset.put(bean.getId(), bean);
            for (Bean toValidate : beansToValidateSubset.values()) {
                predecessors.putAll(getDirectPredecessors(toValidate, all));
            }

            for (Bean predecessor : predecessors.values()) {
                for (BeanId ref : predecessor.getReferences()) {
                    Bean b = all.get(ref);
                    if (b == null) {
                        throw CFG301_MISSING_RUNTIME_REF(predecessor.getId());
                    }
                    ref.setBean(b);
                }
            }
            for (Bean toValidate : beansToValidateSubset.values()) {
                // list references of beansToValidate should now
                // be available in predecessors.
                for (BeanId ref : toValidate.getReferences()) {
                    Bean predecessor = predecessors.get(ref);
                    if (predecessor == null) {
                        throw new IllegalStateException("Bug in algorithm. Reference [" + ref
                                + "] of [" + toValidate.getId()
                                + "] should be available in predecessors.");
                    }
                    ref.setBean(predecessor);
                }
            }
            beansToValidate.putAll(predecessors);
        }
        return beansToValidate;
    }

    private Map<BeanId, Bean> getDirectPredecessors(Bean bean, Map<BeanId, Bean> all) {
        Map<BeanId, Bean> predecessors = new HashMap<>();
        for (BeanId ref : bean.getReferences()) {
            Bean predecessor = all.get(ref);
            if (predecessor == null) {
                throw CFG304_BEAN_DOESNT_EXIST(ref);
            }
            predecessors.put(predecessor.getId(), predecessor);
        }
        return predecessors;
    }

    private Map<BeanId, Bean> getDirectSuccessors(Bean bean, Map<BeanId, Bean> all) {
        Map<BeanId, Bean> successors = new HashMap<>();
        for (Bean b : all.values()) {
            List<BeanId> refs = b.getReferences();
            if (refs.contains(bean.getId())) {
                successors.put(b.getId(), b);
            }
        }
        return successors;
    }

    @Override
    public Optional<Bean> getSingleton(String schemaName) throws IllegalArgumentException {
        Map<BeanId, Bean> all = readValuesAsMap();
        for (Bean bean : all.values()) {
            if (bean.getId().getSchemaName().equals(schemaName)) {
                if (!bean.getId().isSingleton()) {
                    throw new IllegalArgumentException("Schema [" + schemaName
                            + "] is not a lookup.");
                }
                BeanId singletonId = bean.getId();
                return getEagerly(singletonId, all);
            }
        }
        return Optional.of(Bean.create(BeanId.createSingleton(schemaName)));
    }

    @Override
    public Map<BeanId, Bean> list(String name) {
        Map<BeanId, Bean> all = readValuesAsMap();
        Map<BeanId, Bean> result = new HashMap<>();
        for (Bean b : all.values()) {
            if (b.getId().getSchemaName().equals(name)) {
                Optional<Bean> bean = getEagerly(b.getId(), all);
                if (bean.isPresent()) {
                    result.put(bean.get().getId(), bean.get());
                }
            }
        }
        return result;
    }

    @Override
    public Map<BeanId, Bean> list(String schemaName, Collection<String> ids)
            throws AbortRuntimeException {
        List<Bean> beans = readValues();
        Map<BeanId, Bean> result = new HashMap<>();
        for (Bean bean : beans) {
            String schema = bean.getId().getSchemaName();
            if (!schema.equals(schemaName)) {
                continue;
            }
            for (String id : ids) {
                if (bean.getId().getInstanceId().equals(id)) {
                    result.put(bean.getId(), bean);
                }
            }
        }
        return result;
    }

    @Override
    public void create(Bean bean) {
        Map<BeanId, Bean> values = readValuesAsMap();
        checkReferencesExist(bean, values);
        if (!bean.isDefault()) {
            checkUniquness(bean, values);
            values.put(bean.getId(), bean);
        } else {
            Bean stored = values.get(bean.getId());
            if (stored == null) {
                values.put(bean.getId(), bean);
            }
        }
        writeValues(values);
    }

    @Override
    public void create(Collection<Bean> set) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        // first check uniquness towards storage
        for (Bean bean : set) {
            checkUniquness(bean, beans);
        }
        // TODO: check that provided beans are unique among themselves.

        // references may not exist in storage, but are provided
        // as part of the transactions, so add them before validating references.
        for (Bean bean : set) {
            beans.put(bean.getId(), bean);
        }
        for (Bean bean : set) {
            checkReferencesExist(bean, beans);
        }
        writeValues(beans);

    }

    @Override
    public void createSingleton(BeanId singleton) {
        Map<BeanId, Bean> values = readValuesAsMap();
        Bean bean = Bean.create(singleton);
        try {
            checkUniquness(bean, values);
        } catch (AbortRuntimeException e) {
            // ignore and return silently.
            return;
        }
        values.put(singleton, bean);
        writeValues(values);
    }

    @Override
    public void set(Bean bean) {
        Map<BeanId, Bean> values = readValuesAsMap();
        Bean existing = values.get(bean.getId());
        if (existing == null) {
            throw CFG304_BEAN_DOESNT_EXIST(bean.getId());

        }
        checkReferencesExist(bean, values);
        checkInstanceExist(bean, values);
        values.put(bean.getId(), bean);
        writeValues(values);
    }

    @Override
    public void set(Collection<Bean> set) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        // TODO: check that provided beans are unique among themselves.

        // references may not exist in storage, but are provided
        // as part of the transactions, so add them before validating references.
        for (Bean bean : set) {
            Bean existing = beans.get(bean.getId());
            if (existing == null) {
                throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
            }
            beans.put(bean.getId(), bean);
        }
        for (Bean bean : set) {
            checkReferencesExist(bean, beans);
        }

        writeValues(beans);
    }

    @Override
    public void merge(Bean bean) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        Bean b = beans.get(bean.getId());
        if (b == null) {
            throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
        }
        replace(b, bean, beans);
        writeValues(beans);
    }

    @Override
    public void merge(Collection<Bean> bean) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        for (Bean replace : bean) {
            Bean target = beans.get(replace.getId());
            if (target == null) {
                throw Events.CFG304_BEAN_DOESNT_EXIST(replace.getId());
            }
            replace(target, replace, beans);
        }
        writeValues(beans);
    }

    private void replace(Bean target, Bean replace, Map<BeanId, Bean> all) {
        if (target == null) {
            // bean did not exist in storage, create it.
            target = replace;
        }
        checkReferencesExist(replace, all);
        for (String name : replace.getPropertyNames()) {
            List<String> values = replace.getValues(name);
            if (values == null || values.size() == 0) {
                // null/empty indicates a remove/reset-to-default op
                target.remove(name);
            } else {
                target.setProperty(name, replace.getValues(name));
            }

        }

        for (String name : replace.getReferenceNames()) {
            List<BeanId> values = replace.getReference(name);
            if (values == null || values.size() == 0) {
                // null/empty indicates a remove/reset-to-default op
                target.remove(name);
            } else {
                target.setReferences(name, values);
            }

        }
    }

    @Override
    public Bean delete(BeanId id) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        checkNoReferencesExist(id, beans);
        checkDeleteDefault(beans.get(id));
        Bean deleted = beans.remove(id);
        writeValues(beans);
        return deleted;
    }

    @Override
    public Collection<Bean> delete(String schemaName, Collection<String> instanceIds) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        ArrayList<Bean> deleted = new ArrayList<>();
        for (String instance : instanceIds) {
            checkDeleteDefault(beans.get(BeanId.create(instance, schemaName)));
            checkNoReferencesExist(BeanId.create(instance, schemaName), beans);
            Bean bean = beans.remove(BeanId.create(instance, schemaName));
            deleted.add(bean);
        }
        writeValues(beans);
        return deleted;
    }

    @Override
    public BeanQuery newQuery(Schema schema) {
        throw new UnsupportedOperationException("");
    }

    private static void checkNoReferencesExist(BeanId deleted, Map<BeanId, Bean> storage) {
        Collection<BeanId> hasReferences = new ArrayList<>();
        for (Bean b : storage.values()) {
            if (hasReferences(b, deleted)) {
                hasReferences.add(b.getId());
            }
        }
        if (hasReferences.size() > 0) {
            throw CFG302_CANNOT_DELETE_BEAN(Arrays.asList(deleted));
        }
    }

    private static void checkReferencesExist(final Bean bean, final Map<BeanId, Bean> storage) {

        ArrayList<BeanId> allRefs = new ArrayList<>();
        for (String name : bean.getReferenceNames()) {
            if (bean.getReference(name) == null) {
                // the reference is about to be removed.
                continue;
            }
            for (BeanId beanId : bean.getReference(name)) {
                allRefs.add(beanId);
            }
        }

        Collection<BeanId> missingReferences = new ArrayList<>();

        for (BeanId beanId : allRefs) {
            if (beanId.getInstanceId() == null) {
                continue;
            }
            Bean b = storage.get(beanId);
            if (b == null) {
                missingReferences.add(beanId);
            }
        }
        if (missingReferences.size() > 0) {
            throw CFG301_MISSING_RUNTIME_REF(bean.getId(), missingReferences);
        }
    }

    private static void checkInstanceExist(Bean bean, Map<BeanId, Bean> storage) {
        Collection<Bean> beans = storage.values();
        for (Bean existingBean : beans) {
            if (existingBean.getId().equals(bean.getId())) {
                return;
            }
        }
        throw CFG304_BEAN_DOESNT_EXIST(bean.getId());

    }

    private static void checkUniquness(Bean bean, Map<BeanId, Bean> storage) {
        Collection<Bean> beans = storage.values();

        for (Bean existing : beans) {
            if (bean.getId().equals(existing.getId())) {
                throw CFG303_BEAN_ALREADY_EXIST(bean.getId());
            }
        }
    }

    private static void checkDeleteDefault(Bean bean) {
        if (bean == null) {
            return;
        }
        if (bean.isDefault()) {
            throw CFG311_DEFAULT_REMOVAL(bean.getId());
        }
    }

    /**
     * Returns the a list of property names of the target bean that have
     * references to the bean id.
     */
    private static boolean hasReferences(Bean target, BeanId reference) {
        for (String name : target.getReferenceNames()) {
            for (BeanId ref : target.getReference(name)) {
                if (ref.equals(reference)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Bean> readValues() {
        ArrayList<Bean> result = new ArrayList<>();
        try {
            String input = Files.toString(file, Charsets.UTF_8);
            Map<Object, Object> map = (Map<Object, Object>) yaml.load(input);
            for (Object schemaName : map.keySet()) {
                Object rawBeans = map.get(schemaName);
                Collection<Object> beans = new ArrayList<>();
                if(Collection.class.isAssignableFrom(rawBeans.getClass())) {
                    beans = (Collection<Object>) rawBeans;
                } else {
                    beans.add(rawBeans);
                }
                for (Object bean : beans) {
                    Map<String, Map<String, Object>> beanObject = (Map<String, Map<String, Object>>) bean;
                    String instanceId = beanObject.keySet().iterator().next();
                    BeanId id = BeanId.create(instanceId, schemaName.toString());
                    YamlBean yamlBean = new YamlBean(id, beanObject.get(instanceId));
                    result.add(yamlBean.toBean());
                }

            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
        return result;

    }

    private Map<BeanId, Bean> readValuesAsMap() {
        List<Bean> beans = readValues();
        Map<BeanId, Bean> map = new HashMap<>();
        for (Bean bean : beans) {
            map.put(bean.getId(), bean);
        }
        return map;

    }

    private void writeValues(Map<BeanId, Bean> map) {
        writeValues(new ArrayList<Bean>(map.values()));
    }

    private void writeValues(List<Bean> beans) {
        File dir = getStorageDir();
        if (!dir.exists()) {
            try {
                dir.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Multimap<String, YamlBean> transformed = ArrayListMultimap.create();
        for (Bean bean : beans) {
            transformed.put(bean.getId().getSchemaName(), new YamlBean(bean));
        }
        Map<String, List<Map<String, Object>>> yamlBeans = new HashMap<>();
        for (String schemaName : transformed.keySet()) {
            for (YamlBean bean : transformed.get(schemaName)){
                Map<String, Object> yaml = bean.toMap();
                List<Map<String, Object>> list = yamlBeans.get(schemaName);
                if(list == null) {
                    list = new ArrayList<>();
                }
                list.add(yaml);
                yamlBeans.put(schemaName, list);
            }
        }

        String output = yaml.dumpAsMap(yamlBeans);
        File file = new File(dir, YAML_BEAN_FILE_NAME);
        try {
            Files.write(output.getBytes(), file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static File getStorageDir() {
        return file.getParentFile();
    }
}
