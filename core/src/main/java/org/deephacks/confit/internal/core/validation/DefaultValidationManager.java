package org.deephacks.confit.internal.core.validation;

import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.spi.ValidationManager;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * </p>
 * This validator does JSR 303, Bean Validation and will not try to do schema
 * validation, ie check data types or referential integrity etc.
 * <p>
 * Validation will only be performed if JSR 303 1.0 Bean Validation API
 * and compliant implementation are available on classpath.
 * </p>
 */
public class DefaultValidationManager extends ValidationManager {
    private Validator validator;

    /** API class for JSR 303 1.0 bean validation */
    public static final String JSR303_1_0_CLASSNAME = "javax.validation.Validation";

    public DefaultValidationManager() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass(JSR303_1_0_CLASSNAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
    @Override
    public void validate(Collection<Object> beans) {
        if (validator == null) {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        }

        for (Object bean : beans) {
            Set<ConstraintViolation<Object>> violations = validator.validate(bean);
            String msg = "";
            for (ConstraintViolation<Object> v : violations) {
                msg = msg + v.getPropertyPath() + " " + v.getMessage();
            }
            if (!"".equals(msg.trim())) {
                throw Events.CFG309_VALIDATION_ERROR(msg);
            }
        }
    }

    @Override
    public void validate(Object bean) {
        validate(Arrays.asList(bean));
    }
}
