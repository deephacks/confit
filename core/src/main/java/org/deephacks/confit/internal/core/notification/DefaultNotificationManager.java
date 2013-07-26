package org.deephacks.confit.internal.core.notification;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.deephacks.confit.ConfigChanges;
import org.deephacks.confit.spi.NotificationManager;


public class DefaultNotificationManager extends NotificationManager  {
    private static Multimap<Class<?>, Observer> observers = ArrayListMultimap.create();

    public void register(Observer observer) {
        for (Class<?> observedClass : observer.getObservedClasses()) {
            observers.put(observedClass, observer);
        }
    }

    @Override
    public void fire(ConfigChanges<?> changes) {
        for (Observer observer : observers.get(changes.getChangeClass())) {
            try {
                observer.notify(changes);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
