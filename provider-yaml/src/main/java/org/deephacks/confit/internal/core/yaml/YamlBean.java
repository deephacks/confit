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

import com.google.common.base.Strings;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlBean {
    private Bean bean;
    private Map<String, Object> beanProperties = new HashMap<>();

    public YamlBean(Bean bean) {
        this.bean = bean;
    }

    public YamlBean(BeanId id, Map<String, Object> bean) {
        this.bean = Bean.create(id);
        this.beanProperties = bean;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        result.put(bean.getId().getInstanceId(), map);
        for (String propName : bean.getPropertyNames()) {
            List<String> values = bean.getValues(propName);
            map.put(propName, values);
        }
        for (String propName : bean.getReferenceNames()) {
            List<BeanId> values = bean.getReference(propName);
            if(values != null && values.size() > 0) {
                map.put(propName + " @ " + values.get(0).getSchemaName(), convertToString(values));
            }
        }

        return result;
    }
    public List<String> convertToString(List<BeanId> ids) {
        List<String> result = new ArrayList<>();
        for (BeanId id : ids) {
            result.add(id.getInstanceId());
        }
        return result;
    }

    public Bean toBean() {
        for (String propName : beanProperties.keySet()) {
            Object values = beanProperties.get(propName);
            if(values == null){
                continue;
            }
            int idx = propName.indexOf("@");
            String propertyName = propName;
            String schemaName = null;
            if(idx > 0){
                propertyName = propName.substring(0, idx - 1);
                schemaName = propName.substring(idx + 2, propName.length());
            }

            if(Collection.class.isAssignableFrom(values.getClass())){
                Collection valueList = (Collection) values;
                if(!Strings.isNullOrEmpty(schemaName)){
                    bean.addReference(propertyName, toBeanIds(valueList, schemaName));
                } else {
                    bean.addProperty(propName, valueList);
                }
            } else {
                Collection<String> valueList = new ArrayList<>();
                valueList.add(values.toString());
                if(!Strings.isNullOrEmpty(schemaName)){
                    bean.addReference(propertyName, toBeanIds(valueList, schemaName));
                } else {
                    bean.addProperty(propName, valueList);
                }
            }
        }
        return bean;
    }

    public List<BeanId> toBeanIds(Collection<String> ids, String schemaName) {
        List<BeanId> result = new ArrayList<>();
        for (String id : ids) {
            result.add(BeanId.create(id, schemaName));
        }
        return result;
    }
}
