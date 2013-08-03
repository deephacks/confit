package org.deephacks.confit.internal.core.property.typesafe.impl;

import org.deephacks.confit.internal.core.property.typesafe.ConfigMergeable;
import org.deephacks.confit.internal.core.property.typesafe.ConfigValue;

interface MergeableValue extends ConfigMergeable {
    // converts a Config to its root object and a ConfigValue to itself
    ConfigValue toFallbackValue();
}
