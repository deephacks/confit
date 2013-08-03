package org.deephacks.confit.spi;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Lookup is responsible for solving the problem of dynamic service discovery in different
 * environments like standard Java SE ServiceLoader, CDI, Spring, OSGi etc.
 *
 * Service providers register themselves and clients query for a suitable provider,
 * without knowing how lookup is performed. The purpose is to achieve modularity and
 * separation between components.
 *
 */
public class Lookup extends LookupProvider {
    private ArrayList<LookupProvider> lookupProviders = new ArrayList<>();
    private static Lookup LOOKUP;
    private static PropertyManager propertyManager;
    static {
        propertyManager = Lookup.get().lookup(PropertyManager.class);
    }

    protected Lookup() {
    }

    /**
     * Acquire the Lookup registry.
     *
     * @return The lookup registry.
     */
    public static Lookup get() {
        if (LOOKUP != null) {
            return LOOKUP;
        }
        synchronized (Lookup.class) {
            // allow for override of the Lookup.class
            String overrideClassName = System.getProperty(Lookup.class.getName());
            ClassLoader l = Thread.currentThread().getContextClassLoader();
            try {
                if (overrideClassName != null && !"".equals(overrideClassName)) {
                    LOOKUP = (Lookup) Class.forName(overrideClassName, true, l).newInstance();
                } else {
                    LOOKUP = new Lookup();
                }
            } catch (Exception e) {
                // ignore
            }
            // ServiceLoader and CDI are used by defaults, important that
            // CDI is used first so that beans are enabled for injection
            CdiLookup cdiLookup = new CdiLookup();
            LOOKUP.lookupProviders.add(cdiLookup);
            ServiceLoaderLookup serviceLoaderLookup = new ServiceLoaderLookup();
            LOOKUP.lookupProviders.add(serviceLoaderLookup);
            // Use ServiceLoader to find other LookupProviders
            Collection<LookupProvider> providers = serviceLoaderLookup
                    .lookupAll(LookupProvider.class);
            LOOKUP.lookupProviders.addAll(providers);
        }

        return LOOKUP;
    }

    @Override
    public <T> Collection<T> lookupAll(Class<T> clazz) {
        ArrayList<T> result = new ArrayList<>();
        for (LookupProvider lp : lookupProviders) {
            result.addAll(lp.lookupAll(clazz));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> T lookup(Class<T> clazz) {
        Object object = objectRegistry.get(clazz);
        if (object != null) {
            return (T) object;
        }
        for (LookupProvider lp : lookupProviders) {
            Collection<T> result = lp.lookupAll(clazz);
            if (result.isEmpty()) {
                continue;
            }
            T prefered = lookupPrefered(clazz, result);
            return prefered != null ? prefered : result.iterator().next();
        }
        return lookupPrefered(clazz, new ArrayList<T>());
    }

    @SuppressWarnings("unchecked")
    public <T> T lookup(Class<T> clazz, Class<? extends T> defaultClass) {
        T instance = lookup(clazz);
        if (instance != null) {
            return instance;
        }
        try {
            return defaultClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void register(Class<T> clazz, T object) {
        objectRegistry.put(clazz, object);
    }

    public void registerLookup(LookupProvider provider) {
        lookupProviders.add(provider);
    }

    public void unregisterLookup(LookupProvider provider) {
        lookupProviders.remove(provider);
    }

    public String toString() {
        return Objects.toStringHelper(Lookup.class).add("LOOKUP", LOOKUP.getClass().getName())
                .add("lookupProviders", lookupProviders).toString();
    }

    <T> T lookupPrefered(Class<T> clazz, Collection<T> instances) {
        final Optional<String> preferred;
        if (propertyManager != null) {
            preferred = propertyManager.get(clazz.getName());
        } else {
            preferred = Optional.absent();
        }
        return getPreferredInstance(instances, preferred);
    }

    static <T> T getPreferredInstance(Collection<T> instances, Optional<String> preferred) {
        LinkedList<T> preferredInstances = new LinkedList<>();
        if (instances == null || instances.size() == 0) {
            return null;
        }
        for (T instance : instances) {
            if (preferred.isPresent() && instance.getClass().getName().equals(preferred.get())) {
                return instance;
            } else {
                if (instance.getClass().getName().toLowerCase().contains("default")) {
                    preferredInstances.addLast(instance);
                } else {
                    preferredInstances.addFirst(instance);
                }
            }
        }
        if (preferredInstances.isEmpty()) {
            throw new IllegalArgumentException("Could not find preferred instance [" + preferred.get() + "] " +
                    "among available instances [" + instances + "].");
        }
        return preferredInstances.peekFirst();
    }

    static Object newInstance(String className) {
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

    static Class<?> forName(String className) {
        try {
            Class<?> primitive = ALL_PRIMITIVE_TYPES.get(className);
            return primitive != null ? primitive : Thread.currentThread()
                    .getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}