/**
 *   Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package org.deephacks.confit.internal.core.runtime.typesafe.impl;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.deephacks.confit.internal.core.runtime.typesafe.ConfigOrigin;
import org.deephacks.confit.internal.core.runtime.typesafe.ConfigValueType;

final class ConfigInt extends ConfigNumber implements Serializable {

    private static final long serialVersionUID = 2L;

    final private int value;

    ConfigInt(ConfigOrigin origin, int value, String originalText) {
        super(origin, originalText);
        this.value = value;
    }

    @Override
    public ConfigValueType valueType() {
        return ConfigValueType.NUMBER;
    }

    @Override
    public Integer unwrapped() {
        return value;
    }

    @Override
    String transformToString() {
        String s = super.transformToString();
        if (s == null)
            return Integer.toString(value);
        else
            return s;
    }

    @Override
    protected long longValue() {
        return value;
    }

    @Override
    protected double doubleValue() {
        return value;
    }

    @Override
    protected ConfigInt newCopy(ConfigOrigin origin) {
        return new ConfigInt(origin, value, originalText);
    }

    // serialization list goes through SerializedConfigValue
    private Object writeReplace() throws ObjectStreamException {
        return new SerializedConfigValue(this);
    }
}
