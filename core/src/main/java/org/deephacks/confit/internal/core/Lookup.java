package org.deephacks.confit.internal.core;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Lookup is responsible for solving the problem of dynamic service discovery. Service providers
 * register themselves and clients query for a suitable provider, without knowing how lookupPrefered is
 * performed. The purpose is to achieve modularity and separation between components.
 */
public class Lookup extends LookupProvider {
    private ArrayList<LookupProvider> lookupProviders = new ArrayList<>();
    private static Lookup LOOKUP;
    protected Lookup() {
    }

    /**
     * Aquire the Lookup registry.
     * @return The lookupPrefered registry.
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
            return result.iterator().next();
        }
        return null;
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

}