package org.deephacks.confit.serialization;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.deephacks.confit.Config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * General purpose class for commonly used reflection operations.
 *
 * @author Kristoffer Sjogren
 */
public final class Reflections {
    private static final Map<String, Class<?>> ALL_PRIMITIVE_TYPES = new HashMap<>();
    private static final Map<String, Class<?>> ALL_PRIMITIVE_NUMBERS = new HashMap<>();
    static {
        for (Class<?> primitiveNumber : Arrays.asList(byte.class, short.class,
                int.class, long.class, float.class, double.class)) {
            ALL_PRIMITIVE_NUMBERS.put(primitiveNumber.getName(), primitiveNumber);
            ALL_PRIMITIVE_TYPES.put(primitiveNumber.getName(), primitiveNumber);
        }
        for (Class<?> primitive : Arrays.asList(char.class, boolean.class)) {
            ALL_PRIMITIVE_TYPES.put(primitive.getName(), primitive);
        }
    }

    public static Class<?> forName(String className) {
        try {
            Class<?> primitive = ALL_PRIMITIVE_TYPES.get(className);
            return primitive != null ? primitive : Thread.currentThread()
                    .getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPrimitiveNumber(Class<?> type) {
        return ALL_PRIMITIVE_NUMBERS.get(type.getName()) != null;
    }

    /**
     * Get list superclasses and interfaces recursively.
     *
     * @param clazz The class to start the search with.
     *
     * @return List of list super classes and interfaces of {@code clazz}. The list contains the class itself! The empty
     *         list is returned if {@code clazz} is {@code null}.
     */
    public static List<Class<?>> computeClassHierarchy(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        computeClassHierarchy(clazz, classes);
        return classes;
    }

    /**
     * Get list superclasses and interfaces recursively.
     *
     * @param clazz The class to start the search with.
     * @param classes List of classes to which to add list found super classes and interfaces.
     */
    private static void computeClassHierarchy(Class<?> clazz, List<Class<?>> classes) {
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            if (classes.contains(current)) {
                return;
            }
            classes.add(current);
            for (Class<?> currentInterface : current.getInterfaces()) {
                computeClassHierarchy(currentInterface, classes);
            }
        }
    }

    public static List<Class<?>> computeEnclosingClasses(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        computeEnclosingClasses(clazz, classes);
        return classes;
    }

    private static void computeEnclosingClasses(Class<?> clazz, List<Class<?>> classes) {
        for (Class<?> current = clazz; current != null; current = current.getEnclosingClass()) {
            if (classes.contains(current)) {
                return;
            }
            classes.add(current);
            for (Class<?> currentInterface : current.getInterfaces()) {
                computeEnclosingClasses(currentInterface, classes);
            }
        }
    }

    public static <T> T newInstance(Class<T> type) throws InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Constructor<?> c;
        if(Modifier.isStatic(type.getModifiers())) {
            c = type.getDeclaredConstructor(new Class[] {});
        } else {
            try {
                Class<?> enclosing = type.getEnclosingClass();
                if(type.getName().contains("$") && enclosing != null) {
                    throw new IllegalArgumentException("Non-static inner classes are not supported: " + type);
                }
            } catch (Exception e) {
                // this may occur for byte code generated proxies
                throw new IllegalArgumentException("Non-static inner classes are not supported: " + type);
            }
            c = type.getDeclaredConstructor();
        }
        c.setAccessible(true);
        return type.cast(c.newInstance());
    }

    public static Object newInstance(String clazzName, String value) {
        try {
            Class<?> type = forName(clazzName);
            Class<?> enclosing = type.getEnclosingClass();
            if (enclosing == null) {
                Constructor<?> c = type.getConstructor(String.class);
                c.setAccessible(true);
                return type.cast(c.newInstance(value));
            }
            Object o = enclosing.newInstance();
            Constructor<?> c = type.getDeclaredConstructor(enclosing, String.class);
            c.setAccessible(true);
            return type.cast(c.newInstance(o, value));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object newInstance(String className) {
        try {
            Class<?> type = forName(className);
            Class<?> enclosing = type.getEnclosingClass();
            if (enclosing == null) {
                Constructor<?> c = type.getDeclaredConstructor();
                c.setAccessible(true);
                return c.newInstance();
            }
            Object o = enclosing.newInstance();
            Constructor<?> cc = type.getDeclaredConstructor(enclosing);
            cc.setAccessible(true);
            return cc.newInstance(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> getComponentType(T[] a) {
        Class<?> k = a.getClass().getComponentType();
        return (Class<? extends T>) k; // unchecked cast
    }

    public static <T> T[] newArray(T[] a, int size) {
        return newArray(getComponentType(a), size);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(Class<? extends T> k, int size) {
        if (k.isPrimitive())
            throw new IllegalArgumentException("Argument cannot be primitive: " + k);
        Object a = java.lang.reflect.Array.newInstance(k, size);
        return (T[]) a; // unchecked cast
    }

    public static <T> T cast(Class<T> clazz, Object object) {
        if (object == null) {
            return null;
        }
        if (clazz.isAssignableFrom(object.getClass())) {
            return clazz.cast(object);
        }
        return null;
    }

    /**
     * Find list field (including private) on a specific class. Searches list
     * super-classes up to {@link Object}. If the same field name is found in
     * a superclass it is ignore.
     *
     * @param clazz
     *            Class to inspect
     * @return list found fields.
     */
    public static Map<String, Field> findFields(final Class<?> clazz) {
        Map<String, Field> foundFields = new HashMap<>();
        Class<?> searchType = clazz;
        while (!Object.class.equals(searchType) && (searchType != null)) {
            Field[] fields = searchType.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if(foundFields.get(field.getName()) == null) {
                    foundFields.put(field.getName(), field);
                }
            }
            searchType = searchType.getSuperclass();
        }
        return foundFields;
    }

    public static Field findField(final Class<?> cls, String fieldName) {
        Class<?> searchType = cls;
        while (!Object.class.equals(searchType) && (searchType != null)) {
            Field field = null;
            try {
                field = searchType.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // ignore
            }
            if(field != null) {
                field.setAccessible(true);
                return field;
            }
            searchType = searchType.getSuperclass();
        }
        throw new RuntimeException("Could not find field " + fieldName + " on " + cls);
    }

    public static Multimap<Class<? extends Annotation>, Field> findFieldsAnnotations(
            List<Field> fields) {
        Multimap<Class<? extends Annotation>, Field> fieldAnnotations = ArrayListMultimap.create();
        for (Field field : fields) {
            field.setAccessible(true);
            Annotation[] annotations = field.getAnnotations();
            for (Annotation annotation : annotations) {
                fieldAnnotations.put(annotation.annotationType(), field);
            }
        }
        return fieldAnnotations;

    }

    /**
     * Returns the parameterized type of a field, if exists. Wild cards, type
     * variables and raw types will be returned as an empty list.
     * <p>
     * If a field is of type Set<String> then java.lang.String is returned.
     * </p>
     * <p>
     * If a field is of type Map<String, Integer> then [java.lang.String,
     * java.lang.Integer] is returned.
     * </p>
     *
     * @param field
     * @return A list of classes of the parameterized type.
     */
    public static List<Class<?>> getParameterizedType(final Field field) {
        Type type = field.getGenericType();

        if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {

            // the field is it a raw type and does not have generic type
            // argument. Return empty list.
            return new ArrayList<Class<?>>();
        }

        ParameterizedType ptype = (ParameterizedType) type;
        Type[] targs = ptype.getActualTypeArguments();
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (Type aType : targs) {

            if (Class.class.isAssignableFrom(aType.getClass())) {
                classes.add((Class<?>) aType);
            } else if (WildcardType.class.isAssignableFrom(aType.getClass())) {
                // wild cards are not handled by this method
            } else if (TypeVariable.class.isAssignableFrom(aType.getClass())) {
                // type variables are not handled by this method
            }
        }
        return classes;
    }

    /**
     * Returns the parameterized type of a class, if exists. Wild cards, type
     * variables and raw types will be returned as an empty list.
     * <p>
     * If a field is of type Set<String> then java.lang.String is returned.
     * </p>
     * <p>
     * If a field is of type Map<String, Integer> then [java.lang.String,
     * java.lang.Integer] is returned.
     * </p>
     *
     * @param ownerClass the implementing target class to check against
     * @param ownerClass generic interface to resolve the type argument from
     * @return A list of classes of the parameterized type.
     */
    public static List<Class<?>> getParameterizedType(final Class<?> ownerClass,
            Class<?> genericSuperClass) {
        Type[] types = null;
        if (genericSuperClass.isInterface()) {
            types = ownerClass.getGenericInterfaces();
        } else {
            types = new Type[] { ownerClass.getGenericSuperclass() };
        }

        List<Class<?>> classes = new ArrayList<>();
        for (Type type : types) {

            if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {
                // the field is it a raw type and does not have generic type
                // argument. Return empty list.
                return new ArrayList<Class<?>>();
            }

            ParameterizedType ptype = (ParameterizedType) type;
            Type[] targs = ptype.getActualTypeArguments();

            for (Type aType : targs) {

                classes.add(extractClass(ownerClass, aType));
            }
        }
        return classes;
    }

    public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
        try {
            Method method = clazz.getMethod(methodName, args);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... paramTypes) {
        try {
            return clazz.getConstructor(paramTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static Class<?> extractClass(Class<?> ownerClass, Type arg) {
        if (arg instanceof ParameterizedType) {
            return extractClass(ownerClass, ((ParameterizedType) arg).getRawType());
        } else if (arg instanceof GenericArrayType) {
            throw new UnsupportedOperationException("GenericArray types are not supported.");
        } else if (arg instanceof TypeVariable) {
            throw new UnsupportedOperationException("GenericArray types are not supported.");
        }
        return (arg instanceof Class ? (Class<?>) arg : Object.class);
    }

    /**
     * TODO: Need to rewrite this mess!
     */
    public static class ClassIntrospector {
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

        /**
         * TODO: Need to rewrite this mess!
         */
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
}