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

import org.deephacks.confit.Id;
import org.deephacks.confit.internal.core.Reflections;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.model.Schema.SchemaPropertyRefMap;
import org.deephacks.confit.spi.Conversion;
import org.deephacks.confit.spi.Conversion.Converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BeanToObjectConverter implements Converter<Bean, Object> {
    private Conversion conversion = Conversion.get();
    private ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<String, Class<?>>();

    @Override
    public Object convert(Bean source, Class<? extends Object> specificType) {
        final Object instance;
        try {
            instance = newInstance(specificType);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
        Map<String, Object> valuesToInject = new HashMap<>();
        Map<BeanId, Object> instanceCache = new HashMap<>();
        return convert(source, instance, valuesToInject, instanceCache);
    }

    private Object convert(Bean source, Object instance, Map<String, Object> valuesToInject,
            Map<BeanId, Object> instanceCache) {
        instanceCache.put(source.getId(), instance);
        convertProperty(source, valuesToInject);
        convertPropertyList(source, valuesToInject);
        convertPropertyRef(source, valuesToInject, instanceCache);
        convertPropertyRefList(source, valuesToInject, instanceCache);
        convertPropertyRefMap(source, valuesToInject, instanceCache);
        Schema schema = source.getSchema();
        if (!schema.getId().isSingleton()) {
            // do not try to inject get id: the field is static final
            valuesToInject.put(getIdField(instance.getClass()), source.getId().getInstanceId());
        }
        inject(instance, valuesToInject);
        return instance;
    }

    private void inject(Object instance, Map<String, Object> values) {
        List<Field> fields = findFields(instance.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = values.get(field.getName());
            if (value == null) {
                continue;
            }
            try {
                field.set(instance, value);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    private void convertPropertyRefMap(Bean source, Map<String, Object> values,
            Map<BeanId, Object> instanceCache) {
        for (SchemaPropertyRefMap prop : source.getSchema().get(SchemaPropertyRefMap.class)) {
            List<BeanId> beans = source.getReference(prop.getName());
            if (beans == null) {
                continue;
            }
            Map<Object, Object> c = newMap(loadClass(prop.getMapType()));
            for (BeanId beanId : beans) {
                Bean b = beanId.getBean();
                if (b != null) {
                    Object beanInstance = instanceCache.get(beanId);
                    if (beanInstance == null) {
                        try {
                            beanInstance = newInstance(loadClass(b.getSchema().getType()));
                        } catch (Exception e) {
                            throw new UnsupportedOperationException(e);
                        }
                        beanInstance = convert(b, beanInstance, new HashMap<String, Object>(),
                                instanceCache);
                    }
                    c.put(beanId.getInstanceId(), beanInstance);
                }
            }
            values.put(prop.getFieldName(), c);
        }
    }

    private void convertPropertyRefList(Bean source, Map<String, Object> values,
            Map<BeanId, Object> instanceCache) {
        for (SchemaPropertyRefList prop : source.getSchema().get(SchemaPropertyRefList.class)) {
            List<BeanId> references = source.getReference(prop.getName());
            if (references == null) {
                continue;
            }
            Collection<Object> c = newCollection(loadClass(prop.getCollectionType()));
            for (BeanId beanId : references) {
                Bean b = beanId.getBean();
                if (b != null) {
                    Object beanInstance = instanceCache.get(beanId);
                    if (beanInstance == null) {
                        String type = b.getSchema().getType();
                        try {
                            beanInstance = newInstance(loadClass(type));
                        } catch (Exception e) {
                            throw new UnsupportedOperationException(e);
                        }
                        beanInstance = convert(b, beanInstance, new HashMap<String, Object>(),
                                instanceCache);
                    }
                    c.add(beanInstance);
                }
            }
            values.put(prop.getFieldName(), c);
        }
    }

    private void convertPropertyRef(Bean source, Map<String, Object> values,
            Map<BeanId, Object> instanceCache) {
        for (SchemaPropertyRef prop : source.getSchema().get(SchemaPropertyRef.class)) {
            BeanId id = source.getFirstReference(prop.getName());
            if (id == null) {
                continue;
            }
            Bean ref = id.getBean();
            if (ref == null) {
                continue;
            }
            Schema refSchema = ref.getSchema();
            SchemaPropertyRef schemaRef = source.getSchema().get(SchemaPropertyRef.class,
                    prop.getName());
            Object beanInstance = instanceCache.get(id);
            if (beanInstance == null) {
                try {
                    beanInstance = newInstance(loadClass(refSchema.getType()));
                } catch (Exception e) {
                    throw new UnsupportedOperationException(e);
                }
                beanInstance = convert(ref, beanInstance, new HashMap<String, Object>(),
                        instanceCache);
            }

            values.put(schemaRef.getFieldName(), beanInstance);

        }
    }

    private void convertPropertyList(Bean source, Map<String, Object> values) {
        for (SchemaPropertyList prop : source.getSchema().get(SchemaPropertyList.class)) {
            List<String> vals = source.getValues(prop.getName());
            String field = prop.getFieldName();

            if (vals == null) {
                continue;
            }
            Collection<Object> c = newCollection(loadClass(prop.getCollectionType()));
            for (String val : vals) {
                Object converted = conversion.convert(val, loadClass(prop.getType()));
                c.add(converted);
            }

            values.put(field, c);
        }
    }

    private void convertProperty(Bean source, Map<String, Object> values) {
        for (SchemaProperty prop : source.getSchema().get(SchemaProperty.class)) {
            String value = source.getSingleValue(prop.getName());
            String field = prop.getFieldName();
            Object converted = conversion.convert(value, loadClass(prop.getType()));
            values.put(field, converted);
        }
    }

    private static String getIdField(Class<?> clazz) {
        for (Field field : findFields(clazz)) {
            field.setAccessible(true);
            Annotation annotation = field.getAnnotation(Id.class);
            if (annotation != null) {
                return field.getName();
            }
        }
        throw new RuntimeException("Class [" + clazz + "] does not decalare @Id.");
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> newCollection(Class<?> clazz) {
        if (!clazz.isInterface()) {
            try {
                return (Collection<Object>) clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (List.class.isAssignableFrom(clazz)) {
            return new ArrayList<>();
        } else if (Set.class.isAssignableFrom(clazz)) {
            return new HashSet<>();
        }
        throw new UnsupportedOperationException("Class [" + clazz + "] is not supported.");
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> newMap(Class<?> clazz) {
        if (!clazz.isInterface()) {
            try {
                return (Map<Object, Object>) clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return new HashMap<>();
        } else if (ConcurrentMap.class.isAssignableFrom(clazz)) {
            return new ConcurrentHashMap<>();
        }
        throw new UnsupportedOperationException("Class [" + clazz + "] is not supported.");
    }

    private Class<?> loadClass(String className) {
        Class<?> clazz = classCache.get(className);
        if (clazz != null) {
            return clazz;
        }
        clazz = forName(className);
        classCache.put(className, clazz);
        return clazz;
    }

    public static <T> T newInstance(Class<T> type) throws InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return Reflections.newInstance(type);
    }

    public static Class<?> forName(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find list field (including private) on a specific class. Searches list
     * super-classes up to {@link Object}.
     *
     * @param clazz
     *            Class to inspect
     * @return list found fields.
     */
    public static List<Field> findFields(final Class<?> clazz) {
        List<Field> foundFields = new ArrayList<>();
        Class<?> searchType = clazz;
        while (!Object.class.equals(searchType) && (searchType != null)) {
            Field[] fields = searchType.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                foundFields.add(field);
            }
            searchType = searchType.getSuperclass();
        }
        return foundFields;
    }
}
