package org.deephacks.confit.examples.validation;


import com.google.common.base.Optional;
import org.deephacks.confit.Config;
import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.Id;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.model.AbortRuntimeException;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * JSR 303 validation. Note that a javax.validation + implementation
 * must be available on classpath.
 */
public class ValidationExample {
    private static ConfigContext config = ConfigContext.get();
    private static AdminContext admin = AdminContext.get();
    static {
        // optional
        config.register(C.class);
    }

    public static void main(String[] args) {
        simpleConstraint();
        customConstraint();
    }

    private static void simpleConstraint() {
        System.out.println("simpleConstraint");
        try {
            // this call will fail because the value is null
            admin.createObject(new C("1"));
            throw new IllegalStateException("Validation constraint violated.");
        } catch (AbortRuntimeException e) {
            // success
        }
        // try again and set value
        C one = new C("1");
        one.setValue("value");
        admin.createObject(one);

        Optional<C> optional = config.get("1", C.class);
        assertThat(optional.get().getValue(), is("value"));
        admin.deleteObject(one);
    }

    public static void customConstraint() {
        System.out.println("customConstraint");
        C one = new C("1");
        try {
            // this call will fail because the custom constraint is violated
            one.setValue("invalid custom constraint");
            admin.createObject(one);
            throw new IllegalStateException("Custom constraint violated.");
        } catch (AbortRuntimeException e) {
            // success
        }
    }

    /**
     * Configurable class with constraints
     */

    @Config
    // custom constraints can be used
    @SomeConstraint
    public static class C {

        @Id
        private String id;

        // add a not null constraint to this field
        @NotNull
        private String value;

        private C () {

        }

        public C (String id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * A custom constraint
     */

    @Documented
    @Target({ TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = CustomConstraint.class)
    public @interface SomeConstraint {

        String message() default "";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

    }

    public static class CustomConstraint implements ConstraintValidator<SomeConstraint, C> {

        @Override
        public void initialize(SomeConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(C value, ConstraintValidatorContext context) {
            if ( value.getValue() == null) {
                return true;
            }
            if (value.getValue().equals("value")) {
                return true;
            }
            context.buildConstraintViolationWithTemplate("value must be 'null' or 'value'")
                    .addConstraintViolation();
            return false;
        }
    }
}
