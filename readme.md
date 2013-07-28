![confit](https://raw.github.com/deephacks/confit/master/confit-logo-website.png)

[![Build Status](https://travis-ci.org/deephacks/confit.png?branch=master)](https://travis-ci.org/deephacks/confit)

The mission of the project is to provide a simple, yet feature rich, configuration framework for Java. 
Providing the ability of defining configuration decoupled from how and where to store, retrieve and validate 
configuration, also allowing configuration changes without restarting the application. 

The aim is liberate applications to use configuration seamlessly on the terms of their particular environment
without constraining them to Java SE,  EE, OSGi, Spring, CDI or  any other programming model or framework.

## Goals  

* Productivity and Simplicity  
Configuration must be intuitive and non-intrusive, managed in a unified way to support developer 
productivity. Configuration is published and discovered automatically when it become available and is also 
reusable in different contexts without burdening applications with portability issues.

* Predictability and Clarity  
Configuration must be strongly-typed and should express the intents and rules under which circumstances
the application can promise correct behaviour. Violations are handled reliably and does not disturb application
behaviour. Valid changes are applied and exposed to applications in a consistent way.

* Extendability and Portability  
As applications are developed in different shapes and sizes; configuration should enable, not limit, a diversity 
of platforms and technologies.  Applications are supported with the flexibility to  extend and customize a variety 
aspects locally and still be able to manage configuration in a central and unified way.

* Performance and Scalability  
Configuration should not be a limiting factor to application performance. It is quickly accessible to be able 
to meet service-level agreements in environments of scale.

At the moment conf-it comes with provider extensions that allow storing configuration on local disk as 
JSON/YAML/XML or in a relational database using JPA (hibernate or eclipselink) or in HBase. Using either of these 
storage mechanisms, or switching between them, does not impact client code what so ever.

## Examples

There is an [introduction](http://stoffe.deephacks.org/2012/05/07/tools4j-config-part-1-introduction) which is 
a bit outdated at the moment. Will be updated soon.

Conf-it comes as a single jar file (only dependency is guava) and is available in Maven Central.

    <dependency>
      <groupId>org.deephacks</groupId>
      <artifactId>confit</artifactId>
      <version>${version.confit}</version>
    </dependency>

### Define

Configurable classes are annotated with the @Config annotation and all fields are configurable as 
long they are not final or static.

A is a singleton instance, meaning, there can be only one configuration of this class.

    @Config(name = "A")
    public class A {
      private String value;
      private Integer integer;
      private Double decimal;
      private List<String> stringList;
      private List<TimeUnit> enumList;
      private List<URL> customList;
      private DurationTime customType;

      private B singleReference;
      private List<B> listReferences;
      private Map<String, B> mapReferences;
    }

B can have multiple configurations/instances, hence the @Id annotation which identify each instance.

    @Config(name = "B")
    public class B {
      @Id
      private String id;
      
      public (String id) { this.id = id; } 
      
      private Integer integer;
      private Double decimal;
      private List<String> stringList;
      private List<TimeUnit> enumList;
      private List<URL> customList;
      private DurationTime customType;
    }

### Read

We can now access our two configurable classes using the ConfigContext, which is the application interface for 
fetching configuration.

    ConfigContext config = ConfigContext.get();
    A a = config.get(A.class);
    Collection<B> list = config.list(B.class);

### Create

Class A does not have any values because there is no configuration yet. AdminContext is used to provision
configuration.

    AdminContext admin = AdminContext.get();
    A a = new A();
    // set some values on 'a' ...
    
    // create 'a'
    admin.createObject(a);
    
    // we can now read configured values
    a = config.get(A.class);

We can also create instances of class B. 
    
    B one = new B("1");
    B two = new B("2");
    B three = new B("3");
    
    admin.createObjects(Arrays.asList(one, two, three));
    
    Optional<B> optionalOne = config.get("1", B.class);
    Optional<B> optionalTwo = config.get("2", B.class);
    Optional<B> optionalThree = config.get("3", B.class);
    

### References

Notice the last three fields of class A, which are references to B. This means that class A can be 
provisioned with references to instances of class B.

    A a = new A();
    a.setListReferences(one, two, thread);
    admin.createObject(a);

AdminContext will do referential checks to make sure that instances exist, or throw an exception otherwise.
We will also get an exception if we try to delete an instance which is referenced by another instance.

    // this will fail if 'a' reference 'two'
    admin.deleteObject(two);

### Validation

Bean validation 1.1 is supported and to enable it simply add an implementation to classpath like 
[hibernate-validator](http://www.hibernate.org/subprojects/validator.html) for example.


    <dependency>
      <groupId>org.hibernate</groupId>
      <artifactId>hibernate-validator</artifactId>
      <version>5.0.0.Final</version>
      <scope>compile</scope>
    </dependency>

Configurable classes will now be validated AdminContext according to constraints annotations available 
on configurable fields. Custom 
[ConstraintValidator](http://docs.jboss.org/hibernate/stable/beanvalidation/api/javax/validation/ConstraintValidator.html) 
can also be used.


    @Config(name = "C")
    public class C {
      @NotNull
      private String value;
      
      @Size(max = 3)
      private List<String> stringList;

      @Size(max = 3)
      private List<B> listReferences;
    }

### Custom types

TODO


### File configuration


Configuration can be bootstrapped using the [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) 
file format. A file called application.conf will be loaded by default, if available on classpath. This file
is read-only and will not be modified by AdminContext. 

Instead, the file works as a fallback that is used only if no configuration is available for a particular class. 
Configuration provisioned by AdminContext takes precedence.

This is an example of application.conf, containing configuration of class A and B mentioned earlier.


    # class A, singleton no instances defined.
    A {
      value = someValue
      decimal = 1.243453
      customType = PT15H
      customList = ["http://google.com", "http://github.com"]
      listReferences = [1, 2]
      mapReferences = [2, 3]
    }

    # class B, with instances.
    B {

      # instance one
      1 {
        decimal = 12131.13312
        enumList = [SECONDS, HOURS]
      }
      
      # instance two
      2 {
        integer = 1234567
      }
      
      # instance three
      3 {
        stringList = [abc, def, ghi, jkl]
      }      
    }


### Persistence using HBase, JPA or JSON/YAML/XML files.

TODO

### Caching

TODO

### Queries

TODO

### Notifications

TODO

### JAX-RS 2.0 administration

TODO

### Angular.js autogenerated GUI

TODO

### CDI integration and @Inject

TODO

### Writing custom service providers

TODO

## Licensing

This distribution, as a whole, is licensed under the terms of the Apache License, Version 2.0 (see license.txt).
