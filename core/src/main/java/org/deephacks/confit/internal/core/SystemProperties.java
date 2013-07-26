package org.deephacks.confit.internal.core;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.deephacks.confit.internal.core.runtime.typesafe.Config;
import org.deephacks.confit.internal.core.runtime.typesafe.ConfigException;
import org.deephacks.confit.internal.core.runtime.typesafe.ConfigFactory;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.deephacks.confit.model.Events.CFG301_MISSING_RUNTIME_REF;

/**
 * SystemProperties provides a way for reading simple properties.
 *
 * Properties will be read from multiple sources using a fall-back mechanism. The first
 * source to return a valid value will be returned.
 *
 * Properties are read in the following order:
 *
 * 1) System.getProperty
 * 2) User home level file
 * 3) System level file
 * 4) Class path file
 *
 */
public class SystemProperties {
    private static final SystemProperties INSTANCE = new SystemProperties();
    public static final String PROPERTY_FILE_PROPERTY = "application.conf";
    private static final HashMap<String, Schema> schemas = new HashMap<>();
    private final Config config;

    private SystemProperties() {
        String value = System.getProperty(PROPERTY_FILE_PROPERTY);
        final Config config;
        if (!Strings.isNullOrEmpty(value) && new File(value).exists()) {
            config = ConfigFactory.parseFile(new File(value));
        } else {
            config = ConfigFactory.load();
        }
        this.config = config;
    }

    public static SystemProperties instance() {
        return INSTANCE;
    }

    public Optional<String> get(String name) {
        try {
            Object object = config.getAnyRef(name).toString();
            if (object == null) {
                return Optional.absent();
            } else {
                return Optional.of(object.toString());
            }
        } catch (ConfigException e) {
            return Optional.absent();
        }
    }

    public void registerSchema(Schema schema) {
        schemas.put(schema.getName(), schema);
    }

    public List<Bean> list(Schema schema) {
        List<Bean> beans = new ArrayList<>();
        Map<String, Map<String, Object>> instances;
        try {
            instances = (Map) config.getAnyRef(schema.getName());
            if (instances == null) {
                return beans;
            }
        } catch (ConfigException e) {
            return beans;
        }
        if (schema.getId().isSingleton()) {
            final Bean bean = Bean.create(BeanId.createSingleton(schema.getName()));
            Map<String, Object> properties = (Map) instances;
            beans.add(bean);
            constructBean(properties, schema, bean);
        } else {
            for (String instanceId : instances.keySet()) {
                final Map<String, Object> properties = instances.get(instanceId);
                final Bean bean = Bean.create(BeanId.create(instanceId, schema.getName()));
                bean.set(schema);
                beans.add(bean);
                constructBean(properties, schema, bean);
            }
        }
        return beans;
    }

    private void constructBean(Map<String, Object> properties, Schema schema, Bean bean) {
        bean.set(schemas.get(bean.getId().getSchemaName()));
        for (String propertyName : schema.getPropertyNames()) {
            Object value = properties.get(propertyName);
            if (value == null) {
                continue;
            }
            if (value instanceof Collection) {
                bean.addProperty(propertyName, toStrings((Collection) value));
            } else {
                bean.addProperty(propertyName, value.toString());
            }
        }
        for (String propertyName : schema.getReferenceNames()) {
            Object value = properties.get(propertyName);
            Schema refSchema = schemas.get(schema.getReferenceSchemaName(propertyName));
            if (refSchema.getId().isSingleton()) {
                BeanId id = BeanId.createSingleton(refSchema.getName());
                Optional<Bean> optional = get(id);
                if (!optional.isPresent()) {
                    throw CFG301_MISSING_RUNTIME_REF(id);
                }
                Bean b = optional.get();
                b.set(schemas.get(b.getId().getSchemaName()));
                id.setBean(b);
                bean.addReference(propertyName, id);
            }
            if (value == null) {
                continue;
            }
            if (value instanceof Collection) {
                Collection<String> stringValues = toStrings((Collection) value);
                List<BeanId> beanIds = new ArrayList<>();
                for (String id : stringValues) {
                    BeanId beanId = BeanId.create(id, refSchema.getName());
                    Optional<Bean> ref = get(beanId);
                    if (!ref.isPresent()) {
                        throw CFG301_MISSING_RUNTIME_REF(beanId);
                    }
                    Bean b = ref.get();
                    b.set(schemas.get(b.getId().getSchemaName()));
                    beanId.setBean(b);
                    beanIds.add(beanId);
                }
                bean.addReference(propertyName, beanIds);
            } else {
                BeanId id = BeanId.create(value.toString(), refSchema.getName());
                Optional<Bean> idBean = get(id);
                if (!idBean.isPresent()) {
                    throw Events.CFG301_MISSING_RUNTIME_REF(id);
                }
                Bean b = idBean.get();
                b.set(schemas.get(b.getId().getSchemaName()));
                id.setBean(b);
                bean.addReference(propertyName, id);
            }
        }
    }

    private Optional<Bean> get(BeanId beanId) {
        Schema schema = schemas.get(beanId.getSchemaName());
        if (schema == null) {
            throw Events.CFG101_SCHEMA_NOT_EXIST(beanId.getSchemaName());
        }
        Map<String, Map<String, Object>> instances = (Map) config.getAnyRef(schema.getName());
        try {
            final Bean bean;
            if (schema.getId().isSingleton()) {
                bean = Bean.create(BeanId.createSingleton(schema.getName()));
                constructBean((Map) instances, schema, bean);
            } else {
                if (instances == null) {
                    return Optional.absent();
                }
                bean = Bean.create(beanId);
                Map<String, Object> properties = instances.get(beanId.getInstanceId());
                constructBean(properties, schema, bean);
            }
            return Optional.of(bean);
        } catch (ConfigException e) {
            return Optional.absent();
        }
    }

    private Collection<String> toStrings(Collection<Object> values) {
        ArrayList<String> stringValues = new ArrayList<>();
        for (Object value : values) {
            if (value != null) {
                stringValues.add(value.toString());
            }
        }
        return stringValues;
    }
}