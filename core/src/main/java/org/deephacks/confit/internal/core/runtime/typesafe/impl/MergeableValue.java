package org.deephacks.confit.internal.core.runtime.typesafe.impl;

import org.deephacks.confit.internal.core.runtime.typesafe.ConfigMergeable;
import org.deephacks.confit.internal.core.runtime.typesafe.ConfigValue;

interface MergeableValue extends ConfigMergeable {
    // converts a Config to its root object and a ConfigValue to itself
    ConfigValue toFallbackValue();
}
