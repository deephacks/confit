package org.deephacks.confit.internal.jaxrs;

import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.model.Schema.SchemaPropertyRefMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JaxrsBeans {
    private Collection<JaxrsBean> beans = new ArrayList<>();

    public JaxrsBeans() {

    }

    public JaxrsBeans(Collection<Bean> beans) {
        addAll(beans);
    }

    public void add(Bean bean) {
        this.beans.add(new JaxrsBean(bean));
    }

    public void addAll(Collection<Bean> beans) {
        for (Bean bean : beans) {
            this.beans.add(new JaxrsBean(bean));
        }
    }

    public Collection<JaxrsBean> getBeans() {
        return beans;
    }

    public List<Bean> toBeans(Map<String, Schema> schemas) {
        ArrayList<Bean> result = new ArrayList<>();
        for (JaxrsBean bean : beans) {
            Schema schema = schemas.get(bean.getSchemaName());
            result.add(bean.toBean(schema));
        }
        return result;
    }

    public static class JaxrsBean {
        private String id;
        private String schemaName;
        private Map<String, List<String>> properties = new HashMap<>();

        public JaxrsBean() {

        }

        public JaxrsBean(Bean bean) {
            this.schemaName = bean.getId().getSchemaName();
            this.id = bean.getId().getInstanceId();
            for (String name : bean.getPropertyNames()) {
                List<String> values = bean.getValues(name);
                if (values == null || values.isEmpty()) {
                    continue;
                }
                properties.put(name, values);
            }
            for (String name : bean.getReferenceNames()) {
                List<BeanId> refs = bean.getReferences();
                if (refs == null || refs.isEmpty()) {
                    continue;
                }
                List<String> values = new ArrayList<>();
                for (BeanId id : refs) {
                    values.add(id.getInstanceId());
                }
                properties.put(name, values);
            }
        }

        public String getSchemaName() {
            return schemaName;
        }

        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, List<String>> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, List<String>> properties) {
            this.properties = properties;
        }

        public Bean toBean(Schema schema) {
            final BeanId id;
            if (schema.getId().isSingleton()) {
                id = BeanId.createSingleton(schema.getName());
            } else {
                id = BeanId.create(getId(), getSchemaName());
            }
            Bean bean = Bean.create(id);
            Map<String, List<String>> props = getProperties();
            for (String name : schema.getPropertyNames()) {
                List<String> values = props.get(name);
                if (values == null) {
                    continue;
                }
                bean.addProperty(name, values);
            }
            for (String name : schema.getReferenceNames()) {
                List<String> values = props.get(name);
                if (values == null) {
                    continue;
                }

                SchemaPropertyRef ref = schema.get(SchemaPropertyRef.class, name);
                String schemaName = null;
                if (ref != null) {
                    schemaName = ref.getSchemaName();
                }

                SchemaPropertyRefList refList = schema.get(SchemaPropertyRefList.class, name);
                if (refList != null) {
                    schemaName = refList.getSchemaName();
                }
                SchemaPropertyRefMap refMap = schema.get(SchemaPropertyRefMap.class, name);
                if (refMap != null) {
                    schemaName = refMap.getSchemaName();
                }

                for (String value : values) {
                    bean.addReference(name, BeanId.create(value, schemaName));
                }
            }
            return bean;
        }
    }
    public static class JaxrsBeanId {
        private String schemaName;
        private String instanceId;

        public JaxrsBeanId() {

        }

        public JaxrsBeanId(String schemaName, String instanceId) {
            this.schemaName  = schemaName;
            this.instanceId  = instanceId;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public BeanId toBeanId() {
            return BeanId.create(instanceId, schemaName);
        }
    }
}
