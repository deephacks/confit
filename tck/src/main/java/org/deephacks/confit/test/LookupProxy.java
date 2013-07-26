package org.deephacks.confit.test;

import java.lang.reflect.Method;

/**
 * The Lookup class is need by the tck to force different implementations of certain
 * interfaces. But the Lookup class is located in core and the core depends on this
 * package for executing tests according to the tck. This introduce a circular dependency
 * which this class solves by proxying the actual Lookup object.
 */
public class LookupProxy {
    private static final String LOOKUP_CLASS_NAME = "org.deephacks.confit.internal.core.Lookup";
    private static final Object LOOKUP;
    private static final Method LOOKUP_REGISTER_METHOD;
    private static final Method LOOKUP_LOOKUP_METHOD;

    static {
        try {
            Class<?> lookupClass = Class.forName(LOOKUP_CLASS_NAME);
            Method get = lookupClass.getDeclaredMethod("get");
            LOOKUP = get.invoke(null);
            LOOKUP_REGISTER_METHOD = lookupClass.getDeclaredMethod("register", Class.class, Object.class);
            LOOKUP_LOOKUP_METHOD = lookupClass.getDeclaredMethod("lookup", Class.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public static void register(Class<?> clazz, Object object) {
        try {
            LOOKUP_REGISTER_METHOD.invoke(LOOKUP, clazz, object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T lookup(Class<T> clazz) {
        try {
            return (T) LOOKUP_LOOKUP_METHOD.invoke(LOOKUP, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}