package org.deephacks.confit.examples.basic;

import com.google.common.base.Optional;
import org.deephacks.confit.Config;
import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.Id;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.model.AbortRuntimeException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Most basic example of conf-it. Classes are treated as configuration if they
 * have the Config annotation. By default all fields are configurable, except
 * static and final fields.
 *
 * Configurable class are required to have getter methods for its properties.
 *
 * Fields can be annotated with the Config config annotation to get the property
 * a different name and a description.
 *
 */
public class BasicExample {
    // ConfigContext is the application interface for fetching configuration.
    private static ConfigContext config = ConfigContext.get();

    // AdminContext is the administrative interface for creating configuration.
    private static AdminContext admin = AdminContext.get();

    static {
        // this is optional - classes are registered automatically when
        // accessed from ConfigContext. But if AdminContext is accessed before,
        // then it wont know about the classes and throw an exception
        // (unless registered before)
        //
        // In CDI - this method is invoked automatically by the container.
        config.register(A.class, B.class);
    }

    public static void main(String[] args) {
        basicProperties();
        basicInstances();
        references();
        customDataType();
    }

    /**
     * A is treated as a singleton instance because it does not have a field with
     * Id annotation.
     */

    // default schema name is the fully qualified class name.
    @Config
    public static class A {

        // properties can have default values
        private String value = "value";

        private Double decimal;

        // Properties can be enums.
        private TimeUnit timeUnit;

        // we can create custom data types that can be represented as strings
        private DurationTime duration;

        // We can reference other instances.
        private List<B> references;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Double getDecimal() {
            return decimal;
        }

        public void setDecimal(Double decimal) {
            this.decimal = decimal;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }

        public List<B> getReferences() {
            return references;
        }

        public void setReferences(List<B> references) {
            this.references = references;
        }

        public DurationTime getDuration() {
            return duration;
        }

        public void setDuration(DurationTime duration) {
            this.duration = duration;
        }
    }

    /**
     * B is a configurable that may have many instances.
     */

    // Set the schema name of this instance to 'B'.
    @Config(name = "B")
    public static class B {
        // the identification of a particular instance.
        @Id
        private String id;

        private String value;

        // must have no-args constructor
        private B() {

        }

        public B(String id) {
            this.id = id;
        }

        public B(String id, String value) {
            this(id);
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Custom data type that can be used as configuration. This class would
     * of course have some more logic in order to parse the duration and provide
     * type safe getter methods.
     */
    public static class DurationTime {
        private String duration;

        public DurationTime(String duration){
            this.duration = duration;
        }

        public String toString() {
            return duration;
        }
    }


    public static void basicProperties() {
        System.out.println("basicProperties");
        A singleton = config.get(A.class);
        // the default value is available
        assertThat(singleton.getValue(), is("value"));

        // create an A instance and set some values
        // remember this is a singleton, only one instance may exist.
        singleton = new A();
        singleton.setValue("newValue");
        singleton.setTimeUnit(TimeUnit.MICROSECONDS);
        singleton.setDecimal(0.1213);
        admin.createObject(singleton);

        // assert that values are set
        singleton = config.get(A.class);
        assertThat(singleton.getValue(), is("newValue"));
        assertThat(singleton.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat(singleton.getDecimal(), is(0.1213));

        // delete
        admin.deleteObject(singleton);
    }

    public static void basicInstances() {
        System.out.println("basicInstances");
        // create a few instances and give them an id and value
        List<B> instances = Arrays.asList(new B("1", "v1"), new B("2", "v2"), new B("3", "v3"));
        admin.createObjects(instances);

        // list all B instances
        instances = config.list(B.class);
        assertThat(instances.size(), is(3));
        for (B b : instances) {
            assertThat(b.getValue(), is("v" + b.getId()));
        }

        // get and delete individual instances

        Optional<B> optional = config.get("1", B.class);
        assertTrue(optional.isPresent());
        B one = optional.get();
        assertThat(one.getValue(), is("v" + one.getId()));
        admin.deleteObject(one);

        optional = config.get("2", B.class);
        assertTrue(optional.isPresent());
        B two = optional.get();
        assertThat(two.getValue(), is("v" + two.getId()));
        admin.deleteObject(two);


        optional = config.get("3", B.class);
        assertTrue(optional.isPresent());
        B three = optional.get();
        assertThat(three.getValue(), is("v" + three.getId()));
        admin.deleteObject(three);
    }

    private static void references() {
        System.out.println("references");

        A singleton = new A();
        List<B> instances = Arrays.asList(new B("1", "v1"), new B("2", "v2"), new B("3", "v3"));
        singleton.setReferences(instances);

        // create instances first
        admin.createObjects(instances);
        admin.createObject(singleton);

        singleton = config.get(A.class);
        // check that all instances are available
        for (B b : singleton.getReferences()) {
            assertThat(b.getValue(), is("v" + b.getId()));

            try {
                // we cannot delete instances that are
                // referenced by other instances
                admin.deleteObject(b);
                throw new IllegalStateException("Referential integrity violated");
            } catch (AbortRuntimeException e) {
                // success
            }
        }
        admin.deleteObject(singleton);
    }

    private static void customDataType() {
        System.out.println("customDataType");

        A singleton = new A();
        // 15 hour duration
        DurationTime time = new DurationTime("PT15H");
        singleton.setDuration(time);
        admin.createObject(singleton);

        singleton = config.get(A.class);
        assertThat(singleton.getDuration().toString(), is(time.toString()));
    }

}
