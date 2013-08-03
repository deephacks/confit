package org.deephacks.confit;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;

public class ConfigChanges {
    private Multimap<Class<?>, ConfigChange<Object>> changes = ArrayListMultimap.create();
    public ConfigChanges() {
    }

    public void add(ConfigChange change) {
        changes.put(change.getChangeClass(), change);
    }

    public Collection<ConfigChange<Object>> getChanges() {
        Collection<ConfigChange<Object>> result = new ArrayList<>();
        for (ConfigChange<?> change : changes.values()) {
            result.add((ConfigChange<Object>) change);
        }
        return result;
    }


    public <T> Collection<ConfigChange<T>> getChanges(Class<T> cls) {
        Collection<ConfigChange<T>> result = new ArrayList<>();
        for (ConfigChange<?> change : changes.get(cls)) {
            result.add((ConfigChange<T>) change);
        }
        return result;
    }

    public int size() {
        return changes.values().size();
    }

    public static class ConfigChange<T> {
        private Optional<T> before;
        private Optional<T> after;
        private Class<?> changeClass;

        private ConfigChange(Optional<T> before, Optional<T> after){
            this.before = before;
            this.after = after;
            if (before().isPresent()) {
                changeClass = before().get().getClass();
            } else {
                changeClass = after().get().getClass();
            }
        }

        public Class<?> getChangeClass() {
            return changeClass;
        }

        public static <T> ConfigChange created(T after) {
            return new ConfigChange(Optional.absent(), Optional.of(after));
        }

        public static <T> ConfigChange<T> deleted(T before) {
            return new ConfigChange(Optional.of(before), Optional.absent());
        }

        public static <T> ConfigChange<T> updated(T before, T after) {
            return new ConfigChange(Optional.of(before), Optional.of(after));
        }

        public Optional<T> before() {
            return before;
        }

        public Optional<T> after() {
            return after;
        }

    }
}
