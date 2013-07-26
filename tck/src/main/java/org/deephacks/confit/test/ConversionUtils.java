package org.deephacks.confit.test;

import com.google.common.collect.Lists;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.spi.Conversion;

import java.util.Collection;

public class ConversionUtils {

    private static Conversion CONVERSION = Conversion.get();


    public static Collection<Bean> toBeans(Object... objects) {
        return CONVERSION.convert(Lists.newArrayList(objects), Bean.class);
    }

    public static Bean toBean(Object object) {
        return CONVERSION.convert(object, Bean.class);
    }

}
