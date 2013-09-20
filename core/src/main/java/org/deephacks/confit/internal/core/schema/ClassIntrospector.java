package org.deephacks.confit.internal.core.schema;

import com.google.common.base.Optional;
import org.deephacks.confit.Config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.deephacks.confit.internal.core.schema.Reflections.*;

class ClassIntrospector {
    private Class<?> clazz;
    private Map<String, Field> fields;

    public ClassIntrospector(Class<?> clazz) {
        this.clazz = clazz;
        fields = findFields(clazz);
    }

    /**
     * Get the class name of the class.
     * @return
     */
    public String getName() {
        return clazz.getName();
    }

    public Class<?> getTarget() {
        return clazz;
    }

    /**
     * Get class level annotation for class.
     *
     * @param annotation
     * @return
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        return clazz.getAnnotation(annotation);
    }

    public <T extends Annotation> List<FieldWrap> getFieldList(Class<T> clazz) {
        ArrayList<FieldWrap> wrap = new ArrayList<>();
        for (Field f : fields.values()) {
            if (f.isAnnotationPresent(clazz)) {
                wrap.add(new FieldWrap(f, f.getAnnotation(clazz)));
            }
        }
        return wrap;
    }

    public Collection<FieldWrap> getNonStaticFieldList() {
        ArrayList<FieldWrap> wrap = new ArrayList<>();
        for (Field f : fields.values()) {
            if (Modifier.isFinal(f.getModifiers()) && Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            wrap.add(new FieldWrap(f));
        }
        return wrap;
    }

    public <T extends Annotation> Map<String, FieldWrap> getFieldMap(Class<T> clazz) {
        HashMap<String, FieldWrap> wrap = new HashMap<>();
        for (Field f : fields.values()) {
            if (f.isAnnotationPresent(clazz)) {
                wrap.put(f.getName(), new FieldWrap(f, f.getAnnotation(clazz)));
            }
        }
        return wrap;
    }



    public static class FieldWrap {
        private Optional<Annotation> annotation;
        private Field field;
        private boolean isCollection = false;
        private boolean isMap = false;
        private Object defaultDeclaringInstance;
        private boolean primitive;
        private boolean array;

        public FieldWrap(Field f, Annotation annotation) {
            this.field = f;
            this.annotation = Optional.of(annotation);
            this.isCollection = Collection.class.isAssignableFrom(f.getType());
            this.isMap = Map.class.isAssignableFrom(f.getType());
        }
        public FieldWrap(Field f) {
            this.field = f;
            Annotation config = f.getAnnotation(Config.class);
            if (config == null) {
                this.annotation = Optional.absent();
            } else {
                this.annotation = Optional.of(config);
            }
            this.isCollection = Collection.class.isAssignableFrom(f.getType());
            this.isMap = Map.class.isAssignableFrom(f.getType());
        }

        public Field getField() {
            return field;
        }

        public Optional<Annotation> getAnnotation() {
            return annotation;
        }

        public String getFieldName() {
            return field.getName();
        }

        public Class<?> getType() {
            if (!isCollection) {
                return field.getType();
            }
            List<Class<?>> p = getParameterizedType(field);
            if (p.size() == 0) {
                throw new UnsupportedOperationException("Collection of field [" + field
                        + "] does not have parameterized arguments, which is not allowed.");
            }
            return p.get(0);
        }

        public List<Class<?>> getMapParamTypes() {
            if (!isMap) {
                throw new UnsupportedOperationException("Field [" + field + "] is not a map.");
            }
            List<Class<?>> p = getParameterizedType(field);
            if (p.size() == 0) {
                throw new UnsupportedOperationException("Map of field [" + field
                        + "] does not have parameterized arguments, which is not allowed.");
            }
            return p;
        }

        public boolean isCollection() {
            return isCollection;
        }

        public boolean isMap() {
            return isMap;
        }

        public boolean isFinal() {
            return Modifier.isFinal(field.getModifiers());
        }

        public boolean isStatic() {
            return Modifier.isStatic(field.getModifiers());
        }

        public boolean isTransient() {
            return Modifier.isTransient(field.getModifiers());
        }

        public boolean isAnnotationPresent(Class<? extends Annotation> cls) {
            return field.isAnnotationPresent(cls);
        }

        public List<String> getEnums() {
            if (!isCollection) {
                if (field.getType().isEnum()) {
                    List<String> s = new ArrayList<>();
                    for (Object o : field.getType().getEnumConstants()) {
                        s.add(o.toString());
                    }
                    return s;
                } else {
                    return new ArrayList<>();
                }
            }
            List<Class<?>> p = getParameterizedType(field);
            if (p.size() == 0) {
                throw new UnsupportedOperationException("Collection of field [" + field
                        + "] does not have parameterized arguments, which is not allowed.");
            }
            if (p.get(0).isEnum()) {
                List<String> s = new ArrayList<>();
                for (Object o : p.get(0).getEnumConstants()) {
                    s.add(o.toString());
                }
                return s;
            }
            return new ArrayList<>();

        }

        /**
         * Return the raw collection type
         * @return
         */
        public Class<?> getCollRawType() {
            if (!isCollection) {
                throw new UnsupportedOperationException("This field is not a collection.");
            }
            return field.getType();
        }

        /**
         * Return the raw map type
         * @return
         */
        public Class<?> getMapRawType() {
            if (!isMap) {
                throw new UnsupportedOperationException("This field is not a map.");
            }
            return field.getType();
        }

        public boolean isPrimitive() {
            return field.getType().isPrimitive();
        }

        public Object getDefaultValue() {
            if (defaultDeclaringInstance == null) {
                try {
                    defaultDeclaringInstance = newInstance(field.getDeclaringClass());
                } catch (InstantiationException e) {
                    throw new UnsupportedOperationException("Cannot access default values "
                            + "from fields of class which cannot be constructed.", e);
                } catch (IllegalAccessException e) {
                    throw new UnsupportedOperationException("Cannot access default values "
                            + "from fields of class which cannot be constructed.", e);
                } catch (InvocationTargetException e) {
                    throw new UnsupportedOperationException("Cannot access default values "
                            + "from fields of class which cannot be constructed.", e);
                } catch (NoSuchMethodException e) {
                    throw new UnsupportedOperationException("Cannot access default values "
                            + "from fields of class which cannot be constructed.", e);
                }
            }
            try {

                return field.get(defaultDeclaringInstance);
            } catch (IllegalArgumentException e) {
                throw new UnsupportedOperationException("Cannot access default values "
                        + "from fields of instances which cannot be accessed.", e);

            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException("Cannot access default values "
                        + "from fields of class which cannot be accessed.", e);
            }
        }

        public Object getStaticValue() {
            try {
                return field.get(null);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Cannot access values from fields that arent static.");
            }
        }

        @SuppressWarnings("unchecked")
        public Collection<Object> getDefaultValues() {
            if (!isCollection) {
                throw new UnsupportedOperationException("This field is not a collection.");
            }
            return (Collection<Object>) getDefaultValue();
        }

        public Object getValue(Object source) {
            try {
                return field.get(source);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void checkNotPublic() {
            if(Modifier.isPublic(field.getModifiers())) {
                throw new UnsupportedOperationException(
                        "Field ["+field+"] is public. Public fields are not allowed since it " +
                                "breaks the API of having cached proxies.");
            }
        }

        public boolean isArray() {
            return field.getType().isArray();
        }
    }

    public <T extends Annotation> List<Field> getFieldsAnnotatedWith(Class<T> clazz) {
        ArrayList<Field> f = new ArrayList<>();
        for (Field field : fields.values()) {
            if (field.isAnnotationPresent(clazz)) {
                f.add(field);
            }
        }
        return f;

    }

}