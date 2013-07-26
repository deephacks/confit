package org.deephacks.confit.internal.core;

import com.google.common.base.Optional;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.SchemaManager;

import java.util.HashMap;
import java.util.Map;

/**
 * In memory SchemaManager.
 */
public class DefaultSchemaManager extends SchemaManager {
    private static final HashMap<String, Schema> schemas = new HashMap<>();

    public DefaultSchemaManager() {
    }

    @Override
    public synchronized  Map<String, Schema> getSchemas() {
        return schemas;
    }

    @Override
    public Optional<Schema> getSchema(String schemaName) {
        Schema schema = schemas.get(schemaName);
        if (schema == null) {
            return Optional.absent();
        }
        return Optional.of(schema);
    }

    @Override
    public void registerSchema(Schema... schema) {
        for (Schema s : schema) {
            schemas.put(s.getName(), s);
        }
    }

    @Override
    public void removeSchema(String schemaName) {
        schemas.remove(schemaName);
    }
}
