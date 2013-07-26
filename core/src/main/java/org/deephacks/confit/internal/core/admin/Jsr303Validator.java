package org.deephacks.confit.internal.core.admin;

import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.spi.Conversion;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * When beans are created or updated, it is the responsibility of the
 * validator to maintain data integrity by enforcing validation
 * constraints and reject operations that violate these rules.
 * </p>
 * This validator does JSR 303, Bean Validation and will not try to do schema
 * validation, ie check data types or referential integrity etc.
 * <p>
 * Validation will only be peformed if JSR 303 1.0 Bean Validation API
 * and compliant implementation are available on classpath.
 * </p>
 *
 * @author Kristoffer Sjogren
 */
final class Jsr303Validator {
    private Validator validator;
    private Conversion conversion = Conversion.get();
    private ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    /**
     * Validate a collection of bean instances. This method is called
     * when beans are provisioned from an administrative context.
     * <p>
     * Beans can correlate their respective validation constraints using
     * the schema name.
     * </p>
     *
     * @param beans to be validated.
     * @throws AbortRuntimeException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void validate(Collection<Bean> beans) throws AbortRuntimeException {
        if (validator == null) {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        }

        for (Bean bean : beans) {
            Class genclazz = loadClass(bean.getSchema().getType());
            Object beanToValidate = conversion.convert(bean, genclazz);
            Set<ConstraintViolation<Object>> violations = validator.validate(beanToValidate);
            String msg = "";
            for (ConstraintViolation<Object> v : violations) {
                msg = msg + v.getPropertyPath() + " " + v.getMessage();
            }
            if (!"".equals(msg.trim())) {
                throw Events.CFG309_VALIDATION_ERROR(msg);
            }
        }
    }

    private Class<?> loadClass(String className) {
        Class<?> clazz = classCache.get(className);
        if (clazz != null) {
            return clazz;
        }
        clazz = forName(className);
        classCache.put(className, clazz);
        return clazz;
    }

    public static Class<?> forName(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
