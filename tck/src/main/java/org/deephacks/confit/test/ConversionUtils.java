package org.deephacks.confit.test;

import com.google.common.collect.Lists;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.serialization.Conversion;
import org.deephacks.confit.spi.SchemaManager;

import java.util.Arrays;
import java.util.Collection;

public class ConversionUtils {

    private static Conversion CONVERSION = Conversion.get();
    private static final SchemaManager schemaManager = SchemaManager.lookup();

    public static Collection<Bean> toBeans(Object... objects) {
        final Collection<Bean> beans = CONVERSION.convert(Lists.newArrayList(objects), Bean.class);
        schemaManager.setSchema(beans);
        return beans;
    }

    public static Bean toBean(Object object) {
        final Bean bean = CONVERSION.convert(object, Bean.class);
        schemaManager.setSchema(Arrays.asList(bean));
        return bean;
    }

}
