package org.deephacks.confit.spi;

import org.deephacks.confit.ConfigChanges;
import org.deephacks.confit.ConfigChanges.ConfigChange;
import org.deephacks.confit.ConfigObserver;
import org.deephacks.confit.model.Bean;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsible for sending notifications to observers when configuration is
 * created, deleted or updated. Each notification provide state information
 * before and after a change was made to a particular instance of a specific
 * type.
 */
public abstract class NotificationManager {
    protected static final Set<ConfigObserver> observers = new HashSet<>();

    private static Lookup lookup = Lookup.get();

    /**
     * Lookup the most suitable NotificationManager available.
     *
     * @return NotificationManager.
     */
    public static NotificationManager lookup() {
        return lookup.lookup(NotificationManager.class);
    }

    /**
     * Register an observer interested in configuration state changes.
     *
     * @param observer the instance to receive notifications
     */
    public void register(ConfigObserver observer) {
        observers.add(observer);
    }

    /**
     * Called after configuration changes have been validated, committed to
     * storage and cached. The NotificationManager should send notifications to
     * observers when this method is called.
     *
     * Changes are delivered as Bean instances and should be converted to fully
     * initialized objects when delivered to observers.
     *
     * @param changes state changes
     */
    public abstract void fire(ConfigChanges changes);

    /**
     * Fire a change notification for beans have been successfully created.
     *
     * @param beans was created
     */
    public final void fireCreate(Collection<Bean> beans) {
        ConfigChanges changes = new ConfigChanges();
        for (Bean bean : beans) {
            changes.add(ConfigChange.created(bean));
        }
        fire(changes);
    }

    /**
     * Fire a change notification for beans have been successfully deleted.
     *
     * @param beans was deleted
     */
    public final void fireDelete(Collection<Bean> beans) {
        ConfigChanges changes = new ConfigChanges();
        for (Bean bean : beans) {
            changes.add(ConfigChange.deleted(bean));
        }
        fire(changes);
    }

    /**
     * Fire a change notification for beans have been successfully updated.
     *
     * @param beans was updated
     */
    public final void fireUpdated(Collection<Bean> beans) {
        ConfigChanges changes = new ConfigChanges();
        for (Bean bean : beans) {
            changes.add(ConfigChange.updated(bean, bean));
        }
        fire(changes);
    }
}
