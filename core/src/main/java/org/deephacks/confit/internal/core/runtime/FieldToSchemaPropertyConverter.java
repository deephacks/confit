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
package org.deephacks.confit.internal.core.runtime;

import com.google.common.base.Optional;
import org.deephacks.confit.Config;
import org.deephacks.confit.Id;
import org.deephacks.confit.Index;
import org.deephacks.confit.internal.core.runtime.ClassIntrospector.FieldWrap;
import org.deephacks.confit.internal.core.runtime.ClassToSchemaConverter.ConfigClass;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema.AbstractSchemaProperty;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.model.Schema.SchemaPropertyRefMap;
import org.deephacks.confit.spi.Conversion;
import org.deephacks.confit.spi.Conversion.ConversionException;
import org.deephacks.confit.spi.Conversion.Converter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.deephacks.confit.model.Events.CFG104_UNSUPPORTED_PROPERTY;
import static org.deephacks.confit.model.Events.CFG109_ILLEGAL_MAP;

public final class FieldToSchemaPropertyConverter implements
        Converter<FieldWrap, AbstractSchemaProperty> {
    private Conversion conversion = Conversion.get();

    @Override
    public AbstractSchemaProperty convert(FieldWrap source,
            Class<? extends AbstractSchemaProperty> specificType) {
        if (source.isMap()) {
            List<Class<?>> types = source.getMapParamTypes();
            if (!String.class.equals(types.get(0))) {
                throw CFG109_ILLEGAL_MAP(source.getFieldName());
            }
            if (!types.get(1).isAnnotationPresent(Config.class)) {
                throw CFG109_ILLEGAL_MAP(source.getFieldName());
            }
            return convertReferences(source);
        }
        Class<?> type = source.getType();
        if (type.isAnnotationPresent(Config.class)) {
            return convertReferences(source);
        } else {
            return convertSimple(source);
        }
    }

    private AbstractSchemaProperty convertSimple(FieldWrap source) {
        Optional<Annotation> annotation = source.getAnnotation();
        ConfigClass configClass = new ConfigClass();
        if (annotation.isPresent()) {
            Config config = (Config) annotation.get();
            configClass.name = config.name();
            configClass.desc = config.desc();
        } else {
            configClass.name = source.getFieldName();
            configClass.desc = "";
        }
        String name = configClass.name;
        String desc = configClass.desc;
        String fieldName = source.getFieldName();
        boolean indexed = source.isAnnotationPresent(Index.class);
        if (name == null || "".equals(name)) {
            name = fieldName;
        }
        Class<?> type = source.getType();
        validateField(source);
        try {
            if (source.isCollection()) {
                Collection<String> converted = conversion.convert(source.getDefaultValues(),
                        String.class);
                List<String> defaultValues = new ArrayList<>(converted);

                return SchemaPropertyList.create(name, fieldName, type.getName(), desc, source
                        .isFinal(), source.getEnums(), defaultValues, source.getCollRawType()
                        .getName(), indexed);
            } else {

                return SchemaProperty.create(name, fieldName, type.getName(), desc,
                        source.isFinal(), source.getEnums(),
                        conversion.convert(source.getDefaultValue(), String.class), indexed);
            }
        } catch (ConversionException e) {
            throw CFG104_UNSUPPORTED_PROPERTY(String.class, name, type);
        }
    }

    private AbstractSchemaProperty convertReferences(FieldWrap source) {
        Optional<Annotation> optional = source.getAnnotation();
        ConfigClass config = new ConfigClass();
        if (optional.isPresent()) {
            Config cfg = (Config) optional.get();
            config.name = cfg.name();
            config.desc = cfg.desc();
        } else {
            config.name = source.getFieldName();
            config.desc = "";
        }
        String name = config.name;
        String desc = config.desc;
        String fieldName = source.getFieldName();
        boolean indexed = source.isAnnotationPresent(Index.class);
        if (name == null || "".equals(name)) {
            name = fieldName;
        }
        Class<?> type = source.getType();
        if (source.isCollection()) {
            return SchemaPropertyRefList.create(name, fieldName, getSchemaName(type), type, desc,
                    source.isFinal(), source.getCollRawType().getName(), indexed);
        } else if (source.isMap()) {
            // type is contained in parameterized value of the map
            type = source.getMapParamTypes().get(1);
            return SchemaPropertyRefMap.create(name, fieldName, getSchemaName(type), type, desc,
                    source.isFinal(), source.getMapRawType().getName(), indexed);
        } else {
            return SchemaPropertyRef.create(name, fieldName, getSchemaName(type), type, desc,
                    source.isFinal(), isSingleton(type), indexed);
        }
    }

    private String getSchemaName(Class<?> type) {
        Config configurable = type.getAnnotation(Config.class);
        String schemaName = configurable.name();
        if (schemaName == null || "".equals(schemaName)) {
            schemaName = type.getName();
        }
        return schemaName;
    }

    private boolean isSingleton(Class<?> field) {
        ClassIntrospector introspector = new ClassIntrospector(field);
        return introspector.getFieldList(Id.class).size() == 0;
    }

    private void validateField(FieldWrap field) {
        if (field.isStatic() && !field.isFinal()) {
            // non-final static @Property not supported.
            throw Events.CFG108_ILLEGAL_MODIFIERS(field.getField());
        }
        if (field.isTransient()) {
            // transient @Property not supported.
            throw Events.CFG108_ILLEGAL_MODIFIERS(field.getField());
        }
        if (field.isPrimitive()) {
            throw new IllegalStateException("Fields cannot be primitive at the moment: " + field.getField());
        }

    }
}
