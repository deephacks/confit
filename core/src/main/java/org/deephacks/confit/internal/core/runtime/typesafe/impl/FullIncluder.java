/**
 *   Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package org.deephacks.confit.internal.core.runtime.typesafe.impl;

import org.deephacks.confit.internal.core.runtime.typesafe.ConfigIncluder;
import org.deephacks.confit.internal.core.runtime.typesafe.ConfigIncluderClasspath;
import org.deephacks.confit.internal.core.runtime.typesafe.ConfigIncluderFile;
import org.deephacks.confit.internal.core.runtime.typesafe.ConfigIncluderURL;

interface FullIncluder extends ConfigIncluder, ConfigIncluderFile, ConfigIncluderURL,
            ConfigIncluderClasspath {

}
