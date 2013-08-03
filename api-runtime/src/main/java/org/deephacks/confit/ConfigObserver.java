package org.deephacks.confit;

/**
 * Configuration observers are notified when configuration changes occur.
 * The order in which observers are notified is unspecified.
 *
 * Every observer will receive each notification once and any exception thrown
 * by an observer will be ignored, no retries. Observer notification failures will
 * not affect notification delivery to another observer.
 */
public interface ConfigObserver {

    /**
     * Called when configuration changes have been validated, cached and
     * committed to storage.
     *
     * @param changes changes that have been made
     */
    public void notify(ConfigChanges changes);

}
