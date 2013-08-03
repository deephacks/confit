package org.deephacks.confit.internal.core.schema;

import com.google.common.base.Optional;
import org.deephacks.confit.Config;
import org.deephacks.confit.Id;
import org.deephacks.confit.internal.core.schema.ClassIntrospector.FieldWrap;
import org.deephacks.confit.internal.core.schema.ClassToSchemaConverter.ConfigClass;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.Conversion;
import org.deephacks.confit.spi.Conversion.Converter;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.deephacks.confit.model.Events.CFG102_NOT_CONFIGURABLE;

class ObjectToBeanConverter implements Converter<Object, Bean> {
    Conversion conversion = Conversion.get();

    @Override
    public Bean convert(Object source, Class<? extends Bean> specificType) {
        ClassIntrospector i = new ClassIntrospector(source.getClass());
        Bean bean = Bean.create(getBeanId(source));

        for (FieldWrap field : i.getNonStaticFieldList()) {
            field.checkNotPublic();
            Object value = field.getValue(source);
            if (value == null) {
                continue;
            }
            addProperty(value, bean, field);
        }

        Schema schema = conversion.convert(source.getClass(), Schema.class);
        bean.set(schema);
        return bean;
    }

    @SuppressWarnings("unchecked")
    private void addProperty(Object value, Bean bean, FieldWrap fieldwrap) {
        Optional<Annotation> optional = fieldwrap.getAnnotation();
        ConfigClass field = new ConfigClass();
        if (!optional.isPresent()) {
            field.name = fieldwrap.getFieldName();
            field.desc = "";
        } else {
            Config config = (Config) optional.get();
            field.name = config.name();
            field.desc = config.desc();
        }
        String name = field.name;
        if (name == null || "".equals(name)) {
            name = fieldwrap.getFieldName();
        }
        if (fieldwrap.isCollection()) {
            Class<?> paramClass = fieldwrap.getType();
            Collection<Object> values = (Collection<Object>) value;
            if (paramClass.isAnnotationPresent(Config.class)) {
                for (Object object : values) {
                    bean.addReference(name, getRecursiveBeanId(object));
                }
            } else {
                bean.addProperty(name, conversion.convert(values, String.class));
            }
        } else if (fieldwrap.isMap()) {
            Class<?> paramClass = fieldwrap.getMapParamTypes().get(1);
            Map<String, Object> values = (Map<String, Object>) value;
            if (paramClass.isAnnotationPresent(Config.class)) {
                for (Object object : values.values()) {
                    bean.addReference(name, getRecursiveBeanId(object));
                }
            }
        } else {
            if (value.getClass().isAnnotationPresent(Config.class)) {
                bean.addReference(name, getRecursiveBeanId(value));
            } else {
                String converted = conversion.convert(value, String.class);
                bean.setProperty(name, converted);
            }
        }
    }

    private BeanId getBeanId(Object bean) {
        Config config = bean.getClass().getAnnotation(Config.class);
        if (config == null) {
            throw CFG102_NOT_CONFIGURABLE(bean.getClass());
        }
        String schemaName = config.name();
        if (schemaName == null || "".equals(schemaName.trim())) {
            schemaName = bean.getClass().getName();
        }
        ClassIntrospector i = new ClassIntrospector(bean.getClass());
        List<FieldWrap> ids = i.getFieldList(Id.class);
        if (ids == null || ids.size() != 1) {
            // a lookup
            return BeanId.createSingleton(schemaName);
        }
        FieldWrap id = ids.get(0);
        BeanId targetId;
        Object idValue = id.getValue(bean);
        if (idValue == null) {
            throw Events.CFG107_MISSING_ID();
        }
        targetId = BeanId.create(id.getValue(bean).toString(), schemaName);
        return targetId;
    }

    private BeanId getRecursiveBeanId(Object bean) {
        BeanId targetId = getBeanId(bean);
        Bean targetBean = conversion.convert(bean, Bean.class);
        targetBean.set(conversion.convert(bean.getClass(), Schema.class));
        targetId.setBean(targetBean);
        return targetId;
    }
}