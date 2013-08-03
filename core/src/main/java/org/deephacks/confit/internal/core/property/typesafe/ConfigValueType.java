/**
 *   Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package org.deephacks.confit.internal.core.property.typesafe;

/**
 * The type of a configuration value (following the <a
 * href="http://json.org">JSON</a> type schema).
 */
public enum ConfigValueType {
    OBJECT, LIST, NUMBER, BOOLEAN, NULL, STRING
}
