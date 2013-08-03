package org.deephacks.confit.test.cdi;

import org.deephacks.confit.Config;
import org.deephacks.confit.ConfigScope;

@Config(name = "CdiSingletonConfig", desc = "A cdi enabled lookup configuration")
@ConfigScope
public class CdiSingletonConfig {

    @Config(desc = "value")
    private String value = "value";

    public CdiSingletonConfig() {

    }

    public CdiSingletonConfig(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
