package org.deephacks.confit.internal.core.schema;

import com.google.common.collect.Lists;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.spi.Conversion;

import java.util.Collection;

public class ConversionUtils {

    private static Conversion CONVERSION = Conversion.get();
    static {
        CONVERSION.register(new BeanToObjectConverter());
        CONVERSION.register(new ObjectToBeanConverter());
        CONVERSION.register(new ClassToSchemaConverter());
        CONVERSION.register(new FieldToSchemaPropertyConverter());
    }

    public static Collection<Bean> toBeans(Object... objects) {
        return CONVERSION.convert(Lists.newArrayList(objects), Bean.class);
    }

    public static Bean toBean(Object object) {
        return CONVERSION.convert(object, Bean.class);
    }

}
