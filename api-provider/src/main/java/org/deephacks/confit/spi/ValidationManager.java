package org.deephacks.confit.spi;

import com.google.common.base.Optional;

import java.util.Collection;

/**
 * <p>
 * When beans are created or updated, it is the responsibility of the
 * validator to maintain data integrity by enforcing validation
 * constraints and reject operations that violate these rules.
 * </p>
 * This validator does JSR 303, Bean Validation and will not try to do schema
 * validation, ie check data types or referential integrity etc.
 * <p>
 * Validation will only be peformed if JSR 303 1.0 Bean Validation API
 * and compliant implementation are available on classpath.
 * </p>
 *
 * @author Kristoffer Sjogren
 */
public abstract class ValidationManager {
    private static Lookup lookup = Lookup.get();

    public static Optional<ValidationManager> lookup() {
        ValidationManager manager = lookup.lookup(ValidationManager.class);
        if (manager != null) {
            return Optional.of(manager);
        } else {
            return Optional.absent();
        }
    }

    public abstract void validate(Collection<Object> beans);

    public abstract void validate(Object bean);

}
