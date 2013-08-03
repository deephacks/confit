package org.deephacks.confit.spi;

import com.google.common.collect.Lists;

import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LookupProvider {
    protected final ConcurrentHashMap<Class<?>, Object> objectRegistry = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> RECURSION_SHORTCIRCUIT = new ThreadLocal<>();
    /**
    * Look up an object matching a given interface.
    *
    * @param clazz The type of the object we want to lookupPrefered.
    * @return The object, if found, otherwise null.
    */
    public abstract <T> T lookup(Class<T> clazz);

    /**
    * Look up a list of objects that match a given interface.
    *
    * @param clazz The type of the object we want to lookupPrefered.
    * @return The object(s), if found, otherwise null.
    */
    public abstract <T> Collection<T> lookupAll(Class<T> clazz);

    /**
     * ServiceLoaderLookup is responsible for handling standard java service loader lookup.
     */
    static class ServiceLoaderLookup extends LookupProvider {

        public ServiceLoaderLookup() {

        }

        public final <T> T lookup(Class<T> clazz) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Iterator<T> iterator = java.util.ServiceLoader.load(clazz, cl).iterator();

            while (iterator.hasNext()) {
                try {
                    // return first provider found.
                    return iterator.next();
                } catch (ServiceConfigurationError e) {
                    // treat lookup failures as if the implementation is unavailable
                }
            }
            return null;
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> clazz) {
            ArrayList<T> found = new ArrayList<>();
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Iterator<T> iterator = java.util.ServiceLoader.load(clazz, cl).iterator();

            while (iterator.hasNext()) {
                try {
                    found.add(iterator.next());
                } catch (ServiceConfigurationError e) {
                    // treat lookup failures as if the implementation is unavailable
                }
            }
            return found;
        }
    }

    static class CdiLookup extends LookupProvider {
        private final String CDI_CLASS = "javax.enterprise.inject.spi.CDI";
        /** use CDI only if available on classpath */
        private final boolean cdiEnabled;

        public CdiLookup() {
            Class<?> cls = null;
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                cls = cl.loadClass(CDI_CLASS);
            } catch (Exception e) {
                // ignore
            }
            cdiEnabled = cls != null;
        }

        public final <T> T lookup(Class<T> clazz) {
            if (!cdiEnabled) {
                return null;
            }
            try {
                // lookup first instance found
                return CDI.current().select(clazz).get();
            } catch (Exception e) {
                if (e instanceof CreationException) {
                    throw e;
                }
                // may fail if CDI was not setup correctly
                // which is acceptable so return nothing..
                // fix: new Weld().initialize() at bootstrap
                return null;
            }
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> clazz) {
            if (!cdiEnabled) {
                return Lists.newArrayList();
            }
            // return all instances found
            try {
                if (clazz.getName().equals(RECURSION_SHORTCIRCUIT.get())) {
                    return new ArrayList<>();
                } else {
                    RECURSION_SHORTCIRCUIT.set(clazz.getName());
                    return Lists.newArrayList(CDI.current().select(clazz));
                }
            } catch (Exception e) {
                if (e instanceof CreationException) {
                    throw e;
                }
                // may fail if CDI was not setup correctly
                // which is acceptable so return nothing..
                // fix: new Weld().initialize() at bootstrap
                return new ArrayList<>();
            } finally {
                RECURSION_SHORTCIRCUIT.remove();
            }
        }
    }

}