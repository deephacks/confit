package org.deephacks.confit.internal.core.notification;

import org.deephacks.confit.ConfigChanges;
import org.deephacks.confit.ConfigChanges.ConfigChange;
import org.deephacks.confit.ConfigObserver;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.spi.NotificationManager;

/**
 * Default notification manager that forwards notifications to observers
 * that have been registered.
 */
public class DefaultNotificationManager extends NotificationManager  {

    @Override
    public void fire(ConfigChanges changes) {
        ConfigChanges objectChanges = new ConfigChanges();
        for (ConfigChange<?> change : changes.getChanges()) {
            if (change.before().isPresent() && change.after().isPresent() ) {
                Object after = schemaManager.convertBean((Bean) change.after().get());
                Object before = schemaManager.convertBean((Bean) change.before().get());
                objectChanges.add(ConfigChange.updated(before, after));
            } else if (change.before().isPresent() && !change.after().isPresent()) {
                Object before = schemaManager.convertBean((Bean) change.before().get());
                objectChanges.add(ConfigChange.deleted(before));
            } else if (!change.before().isPresent() && change.after().isPresent()) {
                Object after = schemaManager.convertBean((Bean) change.after().get());
                objectChanges.add(ConfigChange.created(after));
            } else {
                throw new IllegalArgumentException("ConfigChanges are invalid.");
            }
        }

        for (ConfigObserver observer : observers) {
            try {
                observer.notify(objectChanges);
            } catch (Throwable e) {
                // ignore exceptions to guarantee notification delivery
                // to other observers.
            }
        }
    }
}
