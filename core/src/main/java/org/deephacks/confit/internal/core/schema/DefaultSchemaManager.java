package org.deephacks.confit.internal.core.schema;

import org.deephacks.confit.Config;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.serialization.Conversion;
import org.deephacks.confit.spi.SchemaManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.deephacks.confit.model.Events.CFG101_SCHEMA_NOT_EXIST;
import static org.deephacks.confit.model.Events.CFG102_NOT_CONFIGURABLE;

/**
 * In memory SchemaManager.
 */
public class DefaultSchemaManager extends SchemaManager {
    private static final HashMap<String, Schema> NAME_TO_SCHEMA = new HashMap<>();
    private static final HashMap<Class<?>, Schema> CLASS_TO_SCHEMA = new HashMap<>();
    private static final Conversion CONVERSION = Conversion.get();
    public DefaultSchemaManager() {
    }

    @Override
    public synchronized Map<String, Schema> getSchemas() {
        return NAME_TO_SCHEMA;
    }

    @Override
    public void setSchema(Collection<Bean> beans) {
        setSchema(beans, new ArrayList<BeanId>());
    }

    private void setSchema(Collection<Bean> beans, List<BeanId> seen) {

        for (Bean bean : beans) {
            if (contains(seen, bean.getId())) {
                continue;
            }
            seen.add(bean.getId());
            Schema schema = NAME_TO_SCHEMA.get(bean.getId().getSchemaName());
            if (schema == null) {
                throw CFG101_SCHEMA_NOT_EXIST(bean.getId().getSchemaName());
            }
            bean.set(schema);
            for (BeanId id : bean.getReferences()) {
                schema = NAME_TO_SCHEMA.get(id.getSchemaName());
                id.set(schema);
                Bean refBean = id.getBean();
                if (refBean != null) {
                    setSchema(Arrays.asList(refBean), seen);
                }
            }
        }
    }

    private boolean contains(List<BeanId> seen, BeanId id) {
        for (BeanId anId : seen) {
            if (anId == id) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Schema getSchema(String schemaName) {
        Schema schema = NAME_TO_SCHEMA.get(schemaName);
        if (schema == null) {
            throw CFG101_SCHEMA_NOT_EXIST(schemaName);
        }
        return schema;
    }

    @Override
    public Schema getSchema(Class<?> cls) {
        Schema schema = CLASS_TO_SCHEMA.get(cls);

        if(schema == null) {
            register(cls);
            schema = CLASS_TO_SCHEMA.get(cls);
        }
        return schema;
    }

    @Override
    public void register(Class<?>... classes) {
        for (Class<?> cls : classes) {
            if (CLASS_TO_SCHEMA.get(cls) != null) {
                continue;
            }
            Config config = cls.getAnnotation(Config.class);
            if (config == null) {
                throw CFG102_NOT_CONFIGURABLE(cls);
            }
            String schemaName = config.name();
            if (schemaName == null || "".equals(schemaName)) {
                schemaName = cls.getName();
            }
            Schema schema = CONVERSION.convert(cls, Schema.class);
            CLASS_TO_SCHEMA.put(cls, schema);
            NAME_TO_SCHEMA.put(schemaName, schema);
            for (Class<?> refCls : schema.getReferenceSchemaTypes()) {
                if (CLASS_TO_SCHEMA.get(refCls) != null) {
                    register(cls);
                }
            }
        }
    }

    @Override
    public Schema remove(Class<?> cls) {
        return CLASS_TO_SCHEMA.remove(cls);
    }

    @Override
    public void validateSchema(Collection<Bean> beans) {
        SchemaValidator.validateSchema(beans);
    }

    @Override
    public Object convertBean(Bean bean) {
        Schema schema = getSchema(bean.getId().getSchemaName());
        return CONVERSION.convert(bean, schema.getClassType());
    }

    @Override
    public Collection<Object> convertBeans(Collection<Bean> beans) {
        ArrayList<Object> objects = new ArrayList<>();
        for (Bean bean : beans) {
            objects.add(convertBean(bean));
        }
        return objects;
    }

    @Override
    public Collection<Bean> convertObjects(Collection<Object> objects) {
        return CONVERSION.convert(objects, Bean.class);
    }

    @Override
    public Bean convertObject(Object object) {
        return CONVERSION.convert(object, Bean.class);
    }

}
