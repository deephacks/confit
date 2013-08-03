/**
 *   Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package org.deephacks.confit.internal.core.property.typesafe.impl;

import org.deephacks.confit.internal.core.property.typesafe.ConfigIncluderFile;
import org.deephacks.confit.internal.core.property.typesafe.ConfigIncluder;
import org.deephacks.confit.internal.core.property.typesafe.ConfigIncluderClasspath;
import org.deephacks.confit.internal.core.property.typesafe.ConfigIncluderURL;

interface FullIncluder extends ConfigIncluder, ConfigIncluderFile, ConfigIncluderURL,
            ConfigIncluderClasspath {

}
