package org.deephacks.confit.internal.core;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LookupProvider {
    protected final ConcurrentHashMap<Class<?>, Object> objectRegistry = new ConcurrentHashMap<>();
    protected final SystemProperties properties = SystemProperties.instance();

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
     * ServiceLoaderLookup is responsible for handling standard java service loader lookupPrefered.
     */
    static class ServiceLoaderLookup extends LookupProvider {

        public ServiceLoaderLookup() {

        }

        public final <T> T lookup(Class<T> clazz) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            for (T standardJavaProvider : java.util.ServiceLoader.load(clazz, cl)) {
                // return first provider found. May need more elaborate mechanism in future.
                if (standardJavaProvider == null) {
                    continue;
                }
                // return the provider that was first found.
                return standardJavaProvider;
            }
            return null;
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> clazz) {
            ArrayList<T> found = new ArrayList<>();
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            for (T o : java.util.ServiceLoader.load(clazz, cl)) {
                // return first provider found. May need more elaborate mechanism in future.
                found.add(o);
            }

            if (found != null && found.size() != 0) {
                T object = (T) lookupPrefered(clazz, found);
                if (object != null) {
                    return Lists.newArrayList(object);
                }
                return new ArrayList<>();
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
                // get first instance found
                return CDI.current().select(clazz).get();
            } catch (Exception e) {
                // may fail if CDI was not setup correctly
                // which is acceptable so return nothing..
                // fix: new Weld().initialize() at bootstrap
                return null;
            }
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> clazz) {
            ArrayList found = new ArrayList<>();
            if (!cdiEnabled) {
                T object = (T) lookupPrefered(clazz, found);
                if (object != null) {
                    return Lists.newArrayList(object);
                }
                return new ArrayList<>();
            }
            // return all instances found
            try {
                found = Lists.newArrayList(CDI.current().select(clazz));
                if (found != null && found.size() != 0) {
                    T object = (T) lookupPrefered(clazz, found);
                    if (object != null) {
                        return Lists.newArrayList(object);
                    }
                    return new ArrayList<>();
                }
                return new ArrayList<>();
            } catch (Exception e) {
                // may fail if CDI was not setup correctly
                // which is acceptable so return nothing..
                // fix: new Weld().initialize() at bootstrap

                // we may have CDI API without impl on classpath
                // so try to load a preferred instance if possible
                Optional<String> preferred = properties.get(clazz.getName());
                if (preferred.isPresent()) {
                    try {
                        Object preferredInstance = Reflections.newInstance(preferred.get());
                        return Lists.newArrayList(clazz.cast(preferredInstance));
                    } catch (Exception ee) {
                        throw new IllegalStateException("Could not find preferred instance ["+preferred+"] as specified property file.");
                    }

                }
                return new ArrayList<>();
            }
        }
    }

    Object lookupPrefered(Class<?> clazz, List<?> instances) {
        Optional<String> preferred = properties.get(clazz.getName());
        if (preferred.isPresent()) {
            Object instance = getPreferredInstance(instances, preferred.get());
            if (instance != null) {
                return instance;
            }
            return clazz.cast(Reflections.newInstance(preferred.get()));
        }
        Object instance = getPreferredInstance(instances, null);
        if (instance != null) {
            return instance;
        }

        Collection<?> managers = lookupAll(clazz);
        if (managers.size() == 1) {
            return managers.iterator().next();
        } else {
            return Lookup.get().lookup(clazz);
        }
    }

    private <T> T getPreferredInstance(List<T> instances, String prefered) {
        LinkedList<T> preferedInstances = new LinkedList<>();
        if (instances == null) {
            return null;
        }
        for (T instance : instances) {
            if (Strings.isNullOrEmpty(prefered)) {
                String className = instance.getClass().getName().toLowerCase();
                // make sure that default instances are picked last if
                // more than one implementation is found on classpath
                if (className.contains("default")) {
                    preferedInstances.addLast(instance);
                } else {
                    preferedInstances.addFirst(instance);
                }
            } else if (instance.getClass().getName().equals(prefered)) {
                return instance;
            }
        }
        return preferedInstances.peekFirst();
    }



}