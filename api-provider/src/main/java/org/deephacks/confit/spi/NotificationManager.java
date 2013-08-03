package org.deephacks.confit.spi;

import org.deephacks.confit.ConfigChanges;
import org.deephacks.confit.ConfigChanges.ConfigChange;
import org.deephacks.confit.model.Bean;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class NotificationManager {
    private static Conversion conversion = Conversion.get();

    private static Lookup lookup = Lookup.get();

    /**
     * Lookup the most suitable NotificationManager available.
     *
     * @return NotificationManager.
     */
    public static NotificationManager lookup() {
        return lookup.lookup(NotificationManager.class);
    }

    public abstract void register(Observer observer);

    public abstract void fire(ConfigChanges<?> changes);

    public final void fireCreate(Collection<Bean> beans) {
        ConfigChanges beanChanges = new ConfigChanges();
        ConfigChanges objectChanges = new ConfigChanges();
        for (Bean bean : beans) {
            beanChanges.add(ConfigChange.created(bean));
            Object object = conversion.convert(bean, bean.getSchema().getClassType());
            objectChanges.add(ConfigChange.created(object));
        }
        fire(beanChanges);
        Map<Class<?>, ConfigChanges> typed = arrangeTyped(objectChanges);
        for (Class<?> cls : typed.keySet() ) {
            fire(typed.get(cls));
        }
    }

    public final void fireDelete(Collection<Bean> beans) {
        ConfigChanges changes = new ConfigChanges();
        ConfigChanges objectChanges = new ConfigChanges();
        for (Bean bean : beans) {
            changes.add(ConfigChange.deleted(bean));
            Object object = conversion.convert(bean, bean.getSchema().getClassType());
            objectChanges.add(ConfigChange.created(object));
        }
        fire(changes);
        Map<Class<?>, ConfigChanges> typed = arrangeTyped(objectChanges);
        for (Class<?> cls : typed.keySet() ) {
            fire(typed.get(cls));
        }
    }

    public final void fireUpdated(Collection<Bean> beans) {
        ConfigChanges changes = new ConfigChanges();
        ConfigChanges objectChanges = new ConfigChanges();
        for (Bean bean : beans) {
            changes.add(ConfigChange.updated(bean, bean));
            Object object = conversion.convert(bean, bean.getSchema().getClassType());
            objectChanges.add(ConfigChange.created(object));
        }
        fire(changes);
        Map<Class<?>, ConfigChanges> typed = arrangeTyped(objectChanges);
        for (Class<?> cls : typed.keySet() ) {
            fire(typed.get(cls));
        }
    }

    private Map<Class<?>, ConfigChanges> arrangeTyped(ConfigChanges<?> changes) {
        Map<Class<?>, ConfigChanges> typed = new HashMap<>();
        for (ConfigChange change : changes.getChanges()) {
            ConfigChanges cc = typed.get(change.getChangeClass());
            if (cc == null) {
                cc = new ConfigChanges();
                typed.put(change.getChangeClass(), cc);
            }
            cc.add(change);
        }
        return typed;
    }

    public final static class Observer {
        private Object instance;
        private HashMap<Class<?>, Method> observerMethods = new HashMap<>();

        public Observer(Object observer) {
            this.instance = observer;
            for (Method method : observer.getClass().getDeclaredMethods()) {
                method.setAccessible(true);
                Class<?>[] params = method.getParameterTypes();

                if (params.length != 1) {
                    continue;
                }

                if (ConfigChanges.class.isAssignableFrom(params[0])) {
                    Class<?> changeClass = getConfigChangeParameter(method);
                    if (changeClass != null) {
                        observerMethods.put(changeClass, method);
                    }
                }
            }
        }


        public Collection<Class<?>> getObservedClasses() {
            return observerMethods.keySet();
        }

        public void notify(ConfigChanges changes) {
            Collection<ConfigChange> c = changes.getChanges();
            Class<?> changeClass = c.iterator().next().getChangeClass();
            Method observerMethod = observerMethods.get(changeClass);
            if (observerMethod == null) {
                return;
            }
            try {
                observerMethod.invoke(instance, changes);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        private Class<?> getConfigChangeParameter(Method method) {
            Type[] types =  method.getGenericParameterTypes();
            for (Type type : types) {
                if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {
                    return null;
                }
                ParameterizedType ptype = (ParameterizedType) type;
                Type[] targs = ptype.getActualTypeArguments();
                for (Type aType : targs) {
                    return (Class<?>) aType;
                }
            }
            return null;
        }

        private Class<?> extractClass(Class<?> ownerClass, Type arg) {
            if (arg instanceof ParameterizedType) {
                return extractClass(ownerClass, ((ParameterizedType) arg).getRawType());
            } else if (arg instanceof GenericArrayType) {
                throw new UnsupportedOperationException("GenericArray types are not supported.");
            } else if (arg instanceof TypeVariable) {
                throw new UnsupportedOperationException("GenericArray types are not supported.");
            }
            return (arg instanceof Class ? (Class<?>) arg : Object.class);
        }
    }
}
