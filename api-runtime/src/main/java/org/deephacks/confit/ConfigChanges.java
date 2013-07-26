package org.deephacks.confit;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

public class ConfigChanges<T> {
    private Multimap<Class<?>, ConfigChange<T>> changes = ArrayListMultimap.create();
    private Class<?> changeClass;
    public ConfigChanges() {
    }

    public void add(ConfigChange<T> change) {
        changeClass = change.getChangeClass();
        changes.put(change.getChangeClass(), change);
    }

    public Class<?> getChangeClass() {
        return changeClass;
    }

    public Collection<ConfigChange<T>> getChanges() {
        return changes.values();
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
