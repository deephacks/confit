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
    public static final String PROPERTY_FILE_PROPERTY = "application.conf";
    private static final HashMap<String, Schema> schemas = new HashMap<>();
    private final Config config;

    private SystemProperties(File file) {
        if(!file.exists()) {
            throw new IllegalArgumentException("File does not exist [" + file.getAbsolutePath() + "]");
        }
        this.config = ConfigFactory.parseFile(file);
    }

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
        return new SystemProperties();
    }

    public static SystemProperties instance(File file) {
        return new SystemProperties(file);
    }

    public Optional<String> get(String name) {
        try {
            Object object = config.getAnyRef(name).toString();
            if (object == null) {
                return Optional.absent();
            } else {
                return Optional.of(object.toString());
            }
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public void registerSchema(Schema schema) {
        schemas.put(schema.getName(), schema);
    }

    public List<Bean> list(Schema schema) {
        List<Bean> beans = new ArrayList<>();
        Config schemaConfig;
        try {
            String schemaName = schema.getName().replaceAll("\\$", "\\.");
            schemaConfig = config.getConfig(schemaName);
            if (schemaConfig == null) {
                return beans;
            }
        } catch (Exception e) {
            return beans;
        }
        if (schema.getId().isSingleton()) {
            final Bean bean = Bean.create(BeanId.createSingleton(schema.getName()));
            beans.add(bean);
            constructBean(schemaConfig, schema, bean);
        } else {
            for (Object instanceId : schemaConfig.root().keySet()) {
                Config instance = schemaConfig.getConfig(instanceId.toString());
                final Bean bean = Bean.create(BeanId.create(instanceId.toString(), schema.getName()));
                beans.add(bean);
                constructBean(instance, schema, bean);
            }
        }
        return beans;
    }

    private void constructBean(Config properties, Schema schema, Bean bean) {
        bean.set(schemas.get(bean.getId().getSchemaName()));
        for (String propertyName : schema.getPropertyNames()) {
            Object value;
            try {
                value = properties.getAnyRef(propertyName);
            } catch (Exception e) {
                continue;
            }
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
            Object value;
            try {
                value = properties.getAnyRef(propertyName);
            } catch (Exception e) {
                value = null;
            }
            String schemaName = schema.getReferenceSchemaName(propertyName);
            if (schemaName == null) {
                throw new IllegalArgumentException("Schema [" + schema.getName() + "] does not have reference typed property [" + propertyName + "].");
            }
            Schema refSchema = schemas.get(schemaName);
            if (refSchema == null) {
                throw Events.CFG101_SCHEMA_NOT_EXIST(schemaName);
            }
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

    Optional<Bean> get(BeanId beanId) {
        Schema schema = schemas.get(beanId.getSchemaName());
        if (schema == null) {
            throw Events.CFG101_SCHEMA_NOT_EXIST(beanId.getSchemaName());
        }
        try {
            String schemaName = schema.getName().replaceAll("\\$", "\\.");
            Config schemaConfig = config.getConfig(schemaName);
            Bean bean;
            if (schema.getId().isSingleton()) {
                bean = Bean.create(BeanId.createSingleton(schema.getName()));
                constructBean(schemaConfig, schema, bean);
                return Optional.of(bean);
            } else {
                if (schemaConfig == null) {
                    return Optional.absent();
                }
                bean = Bean.create(beanId);
                try {
                    for (Object instanceId : schemaConfig.root().keySet()) {
                        if (instanceId.equals(beanId.getInstanceId())) {
                            Config instance = schemaConfig.getConfig(instanceId.toString());
                            bean = Bean.create(BeanId.create(instanceId.toString(), schema.getName()));
                            constructBean(instance, schema, bean);
                            return Optional.of(bean);
                        }
                    }
                } catch (Exception e) {
                    return Optional.absent();
                }
            }
            return Optional.absent();
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