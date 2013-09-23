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
package org.deephacks.confit.serialization;

import org.deephacks.confit.Config;
import org.deephacks.confit.Id;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.AbstractSchemaProperty;
import org.deephacks.confit.model.Schema.SchemaId;
import org.deephacks.confit.serialization.Conversion.Converter;
import org.deephacks.confit.serialization.Reflections.ClassIntrospector;
import org.deephacks.confit.serialization.Reflections.ClassIntrospector.FieldWrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.deephacks.confit.model.Events.CFG102_NOT_CONFIGURABLE;
import static org.deephacks.confit.model.Events.CFG108_ILLEGAL_MODIFIERS;

final class ClassToSchemaConverter implements Converter<Class<?>, Schema> {
    private Conversion conversion = Conversion.get();

    @Override
    public Schema convert(Class<?> source, Class<? extends Schema> specificType) {
        ClassIntrospector introspector = new ClassIntrospector(source);
        Config config = introspector.getAnnotation(Config.class);
        if (config == null) {
            throw CFG102_NOT_CONFIGURABLE(source);
        }
        SchemaId schemaId = getId(introspector);
        if (schemaId == null) {
            // lookup instance does not have @Id annotations so we create
            // it from the @Config annotation
            schemaId = SchemaId.create(config.name(), config.desc(), true);
        }
        String schemaName = config.name();
        if (schemaName == null || "".equals(schemaName)) {
            schemaName = source.getName();
        }
        Schema schema = Schema.create(schemaId, introspector.getName(), schemaName, config.desc());
        Collection<Object> fields = new ArrayList<>();
        fields.addAll(introspector.getNonStaticFieldList());
        Collection<AbstractSchemaProperty> props = conversion.convert(fields,
                AbstractSchemaProperty.class);
        for (AbstractSchemaProperty abstractProp : props) {
            schema.add(abstractProp);
        }
        return schema;
    }

    private SchemaId getId(ClassIntrospector introspector) {
        List<FieldWrap> ids = introspector.getFieldList(Id.class);
        boolean isSingleton = false;
        if (ids == null || ids.size() == 0) {
            return null;
        } else {
            FieldWrap id = ids.get(0);
            Id anno = (Id) id.getAnnotation().get();
            if ((id.isStatic() && !id.isFinal()) || (id.isFinal() && !id.isStatic())) {
                throw CFG108_ILLEGAL_MODIFIERS(id.getField());
            }
            String name = anno.name();
            if (name == null || "".equals(name)) {
                name = id.getFieldName();
            }
            return SchemaId.create(name, anno.desc(), isSingleton);
        }
    }

    public static final class ConfigClass {
        public String name;
        public String desc;
    }
}
