package org.deephacks.confit.spi;

import com.google.common.base.Optional;
import org.deephacks.confit.ConfigChanges;
import org.deephacks.confit.ConfigChanges.ConfigChange;
import org.deephacks.confit.ConfigObserver;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Events;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Responsible for sending notifications to observers when configuration is
 * created, deleted or updated. Each notification provide state information
 * before and after a change was made to a particular instance of a specific
 * type.
 */
public abstract class NotificationManager {
    protected static final BeanManager beanManager = BeanManager.lookup();
    protected static final SchemaManager schemaManager = SchemaManager.lookup();
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
     * Fire a notification for a collection of beans that have been created.
     *
     * @param beans created beans
     */
    public final void fireCreate(Collection<Bean> beans) {
        ConfigChanges changes = new ConfigChanges();
        for (Bean bean : beans) {
            changes.add(ConfigChange.created(bean));
        }
        fire(changes);
    }

    /**
     * Create a changes object from a delete operation.
     *
     * @param schemaName schemaName of deleted instances
     * @param instanceIds id of deleted instances
     * @return changes object
     */
    public final ConfigChanges deleted(String schemaName, Collection<String> instanceIds) {
        ConfigChanges changes = new ConfigChanges();
        for (String instanceId : instanceIds) {
            BeanId id = BeanId.create(instanceId, schemaName);
            Optional<Bean> before = beanManager.getEager(id);
            if (!before.isPresent()) {
                throw Events.CFG304_BEAN_DOESNT_EXIST(id);
            }
            Bean bean = before.get();
            schemaManager.setSchema(Arrays.asList(bean));
            changes.add(ConfigChange.deleted(bean));
        }
        return changes;
    }

    /**
     * Fire a notification for a collection of beans that have been deleted.
     *
     * @param beans deleted beans
     */
    public final void fireDelete(List<Bean> beans) {
        ConfigChanges changes = new ConfigChanges();
        for (Bean bean : beans) {
            changes.add(ConfigChange.deleted(bean));
        }
        fire(changes);
    }

    /**
     * Create a changes object from an update operation.
     *
     * @param after state after the update have been made
     * @return changes object
     */
    public final ConfigChanges updated(Collection<Bean> after) {
        ConfigChanges changes = new ConfigChanges();
        for (Bean bean : after) {
            Optional<Bean> optional = beanManager.getEager(bean.getId());
            if (!optional.isPresent()) {
                throw Events.CFG304_BEAN_DOESNT_EXIST(bean.getId());
            }
            Bean before = optional.get();
            schemaManager.setSchema(Arrays.asList(before));
            changes.add(ConfigChange.updated(before, bean));
        }
        return changes;
    }
}
