package org.deephacks.confit.internal.berkeley;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.spi.serialization.BeanSerialization;

@Entity
public class BBean {

    @PrimaryKey
    private BBeanId id;

    private byte[] data;

    private BBean() {

    }

    private BBean(byte[] data) {
        this.data = data;
    }

    public BBean(BBeanId id, Bean bean, BeanSerialization serialization) {
        this.id = id;
        this.data = serialization.write(bean);
    }

    public byte[] getData() {
        return data;
    }
}
