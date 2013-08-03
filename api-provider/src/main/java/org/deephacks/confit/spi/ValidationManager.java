package org.deephacks.confit.spi;

import com.google.common.base.Optional;

import java.util.Collection;

/**
 * When beans are created or updated, it is the responsibility of the validator to
 * maintain data integrity by enforcing validation constraints and reject operations
 * that violate these rules.
 *
 */
public abstract class ValidationManager {
    private static Lookup lookup = Lookup.get();

    /**
     * Lookup the most suitable ValidationManager available.
     *
     * @return ValidationManager.
     */
    public static Optional<ValidationManager> lookup() {
        ValidationManager manager = lookup.lookup(ValidationManager.class);
        if (manager != null) {
            return Optional.of(manager);
        } else {
            return Optional.absent();
        }
    }

    /**
     * Validate a collection of beans. Beans have their references initialized.
     *
     * This method is called when beans are provisioned from an administrative context.
     *
     * @param beans to validate.
     */
    public abstract void validate(Collection<Object> beans);

    /**
     * Validate a beans. Bean references are initialized.
     *
     * This method is called when beans are provisioned from an administrative context.
     *
     * @param bean to validate
     */
    public abstract void validate(Object bean);

}
