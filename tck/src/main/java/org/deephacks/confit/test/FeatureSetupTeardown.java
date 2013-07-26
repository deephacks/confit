package org.deephacks.confit.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is to be set on classes that handle junit test setup
 * (such as @Before, @BeforeClass, @After) for a specific implementation
 * of an interface or feature. 
 * 
 * FeatureTestBuilder use this annotation to automatically invoke these 
 * test setup classes when this specific implementation is targeted for test.
 * 
 * This relieves users from knowing/caring about how to do test setup/teardown 
 * of specific implementations.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FeatureSetupTeardown {

    /**
     * @return the service interface that the implementation implements.
     */
    Class<?> value();
}