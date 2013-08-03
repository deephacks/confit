package org.deephacks.confit.spi;

import com.google.common.base.Optional;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Schema;

import java.util.List;
import java.util.Map;

/**
 * Responsible for reading configuration from property files using an unspecified format.
 *
 * Configuration read from property files is used for bootstrap and fallback for configuration
 * that do not exist in the BeanManager.
 */
public abstract class PropertyManager {

    /**
     * Lookup the most suitable PropertyManager available.
     *
     * @return PropertyManager.
     */
    public static PropertyManager lookup() {
        return Lookup.get().lookup(PropertyManager.class);
    }

    /**
     * Get a simple string property.
     *
     * @param name name of the property.
     * @return value if it exist.
     */
    public abstract Optional<String> get(String name);

    /**
     * List all beans of a specific Schema.
     *
     * @param schema schema to list
     * @param schemas all schemas currently available
     * @return all beans found
     */
    public abstract List<Bean> list(Schema schema, Map<String, Schema> schemas);

}
