package org.deephacks.confit.spi;

import com.google.common.base.Optional;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Schema;

import java.util.List;
import java.util.Map;

public abstract class PropertyManager {

    /**
     * Lookup the most suitable PropertyManager available.
     *
     * @return PropertyManager.
     */
    public static PropertyManager lookup() {
        return Lookup.get().lookup(PropertyManager.class);
    }

    public abstract Optional<String> get(String name);

    public abstract List<Bean> list(Schema schema, Map<String, Schema> schemas);

}
