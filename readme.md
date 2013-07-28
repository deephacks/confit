![confit](https://raw.github.com/deephacks/confit/master/confit-logo-website.png)

[![Build Status](https://travis-ci.org/deephacks/confit.png?branch=master)](https://travis-ci.org/deephacks/confit)

The mission of the project is to provide a simple, yet feature rich, configuration framework for Java. 
Providing the ability of defining configuration decoupled from how and where to store, retrieve and validate 
configuration, also allowing configuration changes without restarting the application. 

The main driver of the project is simplicity.

* If all you need is a static configuration file in a Java SE application, you can just ignore the other features
and dependencies.

* If you need more advanced configuration capbilities like persistence, runtime changes and notifications
in scalable Java EE applications with massive amounts of configuration, you can easily enable these features.

Conf-it tries to liberate applications to use configuration seamlessly on the terms of their particular environment
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
files, in a relational database using JPA (hibernate or eclipselink) or in HBase (mongodb storage is planned). 
Using either of these storage mechanisms, or switching between them, does not impact client code what so ever.

## Examples

Conf-it comes as a single jar file (only dependency is guava) and is available in Maven Central.

```xml
    <dependency>
      <groupId>org.deephacks</groupId>
      <artifactId>confit</artifactId>
      <version>${version.confit}</version>
    </dependency>
```

### Define

Configurable classes are annotated with the @Config annotation and all fields are configurable as 
long they are not final or static. All basic Java object data types are supported, including List fields 
of these types.

A is a singleton instance, meaning, there can be only one configuration of this class.

```java
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
```

B can have multiple configurations/instances, hence the @Id annotation, which is used to identify instances.

```java
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
```

### Read

Configurable classes are accessed using the ConfigContext, which is the application interface for 
fetching configuration.

```java
    ConfigContext config = ConfigContext.get();
    // get a singleton instance
    A a = config.get(A.class);
    // list all instances
    Collection<B> list = config.list(B.class);
```

### Create

AdminContext is used to create, update and delete configuration.

```java
    AdminContext admin = AdminContext.get();
    A a = new A();
    // set some values on 'a' ...
    
    // create 'a'
    admin.createObject(a);
    
    // we can now read configured values
    a = config.get(A.class);
```

We can also create instances of class B. 

```java    
    B one = new B("1");
    B two = new B("2");
    B three = new B("3");
    
    admin.createObjects(Arrays.asList(one, two, three));
    
    Optional<B> optionalOne = config.get("1", B.class);
    Optional<B> optionalTwo = config.get("2", B.class);
    Optional<B> optionalThree = config.get("3", B.class);
```    

### Update

There are two types of update operations, set and merge. 

* A set operation will replace existing instance entirely with the provided instance
* A merge operation will ignore null fields and only replace fields that are initalized.


```java
    // empty instance
    A a = new A();

    // reset any previous field values instance 'a' may have
    admin.setObject(a);

    a.setValue("someValue");
    // will set field 'value' to 'someValue' and keep other fields untouched
    admin.mergeObject(a);

```


### Delete

Deleting an instance will remove any existing instance. Any references an instance may have are untouched.

```java
    A a = new A();
    admin.deleteObject(a);
```


### References

The last three fields of class A have references to class B, so class A can be created or updated with references to 
instances of class B.

```java
    A a = new A();
    a.setListReferences(one, two, thread);
    admin.createObject(a);
    
    // update references
    a.setListReferences(thread);
    admin.mergeObject(a);
```

AdminContext will do referential checks to make sure that instances exist, or throw an exception otherwise.
An exception will also be thrown if trying to delete instances  referenced by other instances.

```java
    a.setListReferences(four);
    // fails if 'four' have not been created earlier
    admin.createObject(a);

    // fails if 'a' reference 'two'
    admin.deleteObject(two);
```

### Validation

Bean validation 1.1 is supported and is enabled by adding a implementation to classpath, like 
[hibernate-validator](http://www.hibernate.org/subprojects/validator.html) for example.

```xml
    <dependency>
      <groupId>org.hibernate</groupId>
      <artifactId>hibernate-validator</artifactId>
      <version>5.0.0.Final</version>
      <scope>compile</scope>
    </dependency>
```

Instances will now be validated by AdminContext when created, updated or deleted according to the constraints
annotations available on configurable fields. Custom 
[ConstraintValidator](http://docs.jboss.org/hibernate/stable/beanvalidation/api/javax/validation/ConstraintValidator.html) 
can also be used.


```java
    @Config(name = "C")
    public class C {
      @NotNull
      private String value;
      
      @Size(max = 3)
      private List<String> stringList;

      @Size(max = 3)
      private List<B> listReferences;
    }
```


```java
    C c = new C();
    // fails since field 'value' is 'null'
    admin.createObject(c);
```


### Custom field types

Configurable fields are not limited to simple Java types. 

```java
    public class DurationTime {
      private int hours;
      private int minutes;
      private long seconds;
      private String duration;
      
      public DurationTime(String duration) {
        // like PT15H for a 15 hour period
        this.duration = duration;
      }
      
      public DurationTime(int hours, int minutes, int seconds) {
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
      }
      
      // other methods ...
      
      public String toString() {
        return duration;
      }
    }
```    

This class can be used as a configurable field because DurationTime have a default String constructor and a toString
method that return a String representation that can be used to construct a DurationTime instance. 

There are also ways to have configurable fields of classes that that doesnt have a default String constructor.


### Default configuration file


Configuration can be bootstrapped using the [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) 
file format. A file called application.conf will be loaded by default, if available on classpath. 
It is possible to change the location of this file by setting a system property. This file is read-only and 
will not be modified by AdminContext. 

Instead, the file works as a fallback that is used only if no configuration is available for a particular class. 
Configuration provisioned by AdminContext takes precedence.

This is an example of application.conf, containing configuration of class A and B mentioned earlier.


    # class A, singleton no instances defined.
    A {
      value = someValue
      decimal = 1.243453
      customType = PT15H
      customList = ["http://google.com", "http://github.com"]
      
      # references to instances of class B (below)
      listReferences = [1, 2]
      mapReferences = [2, 3]
    }

    # singleton classes can use a simplified property=value format
    A.value = someValue
    A.customList = ["http://google.com", "http://github.com"]
    A.listReferences = [1, 2]

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


### Persistence using file, database or HBase.

Configuration is stored in memory by default, which may not be desirable in deployments with availability 
and durability requirements. 

Default storage is changed by adding a provider to classpath, which will automatically override the default
in memory implementation. Changing storage implementation does not have any code impact on configurable classes.

As mentioned earlier, any configuration in persistence storage will override instances in the default configuration file.

#### YAML

The simplest persistence module is the YAML provider where configuration is written to a file on disk when created,
updated or updated.

```xml
      <dependency>
        <groupId>org.deephacks</groupId>
        <artifactId>confit-provider-yaml</artifactId>
      </dependency>
```

#### JPA

Configuration can be stored in a database using the JPA module, which has currently been tested with MySQL, 
PostgreSQL and Derby. A JPA 2.0 implementation must also be available on classpath, where hibernate and eclipselink
have been tested.

Here is an example that use hibernate.

```xml
      <dependency>
        <groupId>org.deephacks</groupId>
        <artifactId>confit-provider-jpa20</artifactId>
      </dependency>
      <dependency>
        <groupId>org.hibernate.javax.persistence</groupId>
        <artifactId>hibernate-jpa-2.0-api</artifactId>
        <version>1.0.0.Final</version>
      </dependency>
      <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-entitymanager</artifactId>
        <version>4.0.0.Final</version>        
      </dependency>
```

#### HBase

Another option is to store the configuration in [HBase](http://hbase.apache.org/), which is a distributed, 
scalable, big data store.

```xml
      <dependency>
        <groupId>org.deephacks</groupId>
        <artifactId>confit-provider-hbase</artifactId>
      </dependency>
      <!-- hadoop is also needed for hdfs storage -->
      <dependency>
        <groupId>org.apache.hbase</groupId>
        <artifactId>hbase</artifactId>
        <version>0.94.7</version>
      </dependency>
      <!-- custom hbase filter used for queries -->
      <dependency>
        <groupId>org.deephacks</groupId>
        <artifactId>confit-provider-hbase-filter</artifactId>
        <version>${version.confit}</version>
      </dependency>
```

#### Mongodb

Planned storage implementation.


### Configuration queries, caching and lazy fetching

Configuration may not be accessed quickly enough by the application when having several thousands instances 
exist in storage. Conf-it have several mechanisms to reduce lookup latency. Caching is used to avoid costly disk
operations and in memory queries/indexes can be used to reduce number of instances that are scanned at lookup.

Caching and queries are enabled using the following dependency.

```xml
      <dependency>
        <groupId>org.deephacks</groupId>
        <artifactId>confit-provider-cached</artifactId>
        <version>${version.confit}</version>
      </dependency>
```

* Queries and indexes

In order for applications to do runtime queries, the class fields that are target for such queries must have 
a @Index annotation. This build indexes on targeted fields and allow retrieval of instances matching
particular criterias immediately in constant time.

```java
    @Config(name = "Employee")
    public Employee {
      @Index
      private Double salary;

      @Index
      private String email;
    }

```


Example query using ConfigContext.

```java
   ConfigResultSet<Employee> result = config.newQuery(Employee.class).add(
               and(lessThan("salary", 10000.0),not(contains("email", "gmail")))).retrieve();

   for (Employee e : result) {
     // do something with each instance
   }

```

* Lazy fetching

Iterating through a ConfigResultSet is lazy, meaning that instances are not initalized until the cursor reach
a particular item in the set.

* Off-heap cache and proxies

Instances are stored in caches as they are created and modified. These caches are their maintain data off-heap
in a compressed binary format which reduce heap preassure and GC pauses when there is massive amounts of 
configuration data.

References to other instances are lazy initalized proxies that are not loaded until they are actually touched by
the application.

### Administrative queries and pagination

Having several hundreds or thousands configuration instances can quickly become difficult to administrate.
The AdminContext and the REST interface comes with query and pagination capabilities to make configuration 
management easier.

Example query using AdminContext.

```java
    List<Bean> result = admin.newQuery("Employee")
                             .add(lessThan("salary", 10000.0))
                             .add(contains("email", "gmail"))
                             .setFirstResult(100)
                             .setMaxResults(50)
                             .retrieve();
```

### Notifications

Applications can register observers that are notified whenever configuration is created, updated or deleted.
Notifications are sent after changes have been validated, committed to storage and cached.

An observer method is a non-abstract method which have exactly one parameter of type ConfigChanges annotated with
the CDI annotation [Observes](http://docs.jboss.org/cdi/api/1.1/javax/enterprise/event/Observes.html).

Observers are not required to run in a CDI environment, but will be registered automatically if doing so.

```java
    // How to register an observer in a pure Java SE environment
    config.registerObserver(new Observer());
```

Example of an observer.

```java
    public class Observer {
    
      public void notify(@Observes ConfigChanges changes) {
        
        // iterate changes affecting class A
        for (ConfigChange<A> change : changes.getChanges(A.class)) {
          // if not present: create notification
          Optional<A> before = change.getBefore();
          
          // if not present: delete notification
          Optional<A> after = change.getAfter();
          
          // if both were present: update notification 
          // may compare before and after to take certain actions
        }
        
        // iterate changes affecting class B
        for (ConfigChange<B> change : changes.getChanges(B.class)) {
          // ...
        }
      }
    }
```
### JAX-RS 2.0 administration

Conf-it comes with a REST interface, a JAX-RS 2.0 server endpoint and a AdminContext client proxy
that can communicate with the endpoint with burdening the client with HTTP or JAX-RS details.


```java
    // the client proxy implement AdminContext seamlessly
    AdminContext admin = AdminContextJaxrsProxy.get("localhost", 8080);
    
    admin.createObject(new A());
    admin.createObjects(Arrays.asList(new B("1"), new B("2")));
```

The REST endpoint can be deploy in [Jetty](http://www.eclipse.org/jetty/) using [RestEasy](http://www.jboss.org/resteasy)
(for example). Have a look at the JAX-RS tests.


### Graphical administration using Angular.js

Conf-it comes with a minimal [angular.js](http://angularjs.org/) [bootstrap](http://twitter.github.io/bootstrap/)
application that use the REST endpoint to administrate configuration graphically. 

The interface is generated automatically at runtime by fetching class schemas from the the REST endpoint. 
Classes that are registered through the ConfigContext will appear immediatly in the GUI, at runtime.

It is possible to do the same search queries and pagination as AdminContext in order to get a better overview
of available configuration.

### Sessions

Planned feature that will enable multiple isolated changes to be committed or rollback in the context of a session.

### CDI integration and @Inject

Singleton class configuration can be injected into CDI beans using the @Inject annotation. AdminContext and 
ConfigContext can also be injected. Configuration that have multiple instances cannot be injected at the
moment. 

Configuration is loaded when it is first accessed by the CDI bean, then cached forever.


```java
    public class Service {
       @Inject
       private ConfigContext config;
       
       @Inject
       private A configuration;
       
       public void execute(String identifier) {
            configuration.getValue();
            Optional<B> optional = config.get(identifier, B.class);
           // etc ...
       }
    
    }
```

### Writing custom service providers

Conf-it can be customized by implementing any of its service provider interfaces.

* SchemaManager

Manage schema discovery and storage.

* BeanManager

Stores configuration.

* NotificationManager

Sends notifications to observers.

* SessionManager

Manage sessions and configuration changes before they are committed to the BeanManager.

* CacheManager

Cache configurable instances on behalf of the ConfigContext.


## Licensing

This distribution, as a whole, is licensed under the terms of the Apache License, Version 2.0 (see license.txt).
