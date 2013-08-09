![confit](https://raw.github.com/deephacks/confit/master/confit-logo-website.png)

[![Build Status](https://travis-ci.org/deephacks/confit.png?branch=master)](https://travis-ci.org/deephacks/confit)

The mission of the project is to provide a simple (yet feature rich) and typesafe way to configure Java applications.

## The problem

Configuration is often scattered around systems in different files, using vastly different formats,
in different locations, jars, deployment descriptors, maybe a little is stored in the database, the
server/container/platform runtime is configured using other tools. No documentation. Duplication. 

Usually there is no mechanism to protect the system from faulty modifications and mistakes. Even making
changes in the first place is risky, timeconsuming and error-prone. Tracking changes made to running applications
become difficult as they are upgraded.

This makes it very hard to get a clear overview and manage the system as a whole. A problem that grows along
with system size.


## The solution

Conf-it provide a productive, typesafe and non-intrusive way to manage configuration in Java, where runtime changes
and tracking may proceed without restarting the application. Applications are decoupled from how and where to store,
retrieve and validate configuration. 

This allow _reuse_ and _unification_ of how configuration is managed across application boundaries without causing
integration conflicts. Configuration can be used seamlessly based on terms of a particular runtime
environment without being constrained to Java SE, EE, OSGi, CDI, Spring or other programming models or frameworks.

Developers are not forced to learn new technology, query languages or define complex schema models upfront.
Configuration is defined programmatically in Java using simple APIs.

Companies should be able to use their existing storage mechanism(s), like a database, Hadoop/HBase, Cassandra or MongoDB to 
leverage on current investments, internal knowledge and routines (like backup and restore).

* Applications that only need a static configuration file in a simplistic Java SE environment can safely 
ignore more advanced features and dependencies of the framework.

* Distributed and scalable applications that require capabilities like remote runtime administration, 
advanced persistence, clustering and notifications with massive amounts of configuration can easily enable these
kind of features.


## Goals  

* Productivity and Simplicity  
Configuration should be uniform and practical to support developer productivity. Applications should be
allowed to grow without changing how configuration is managed. Configuration is discovered automatically
and can be reused in different runtime environments with good tooling support all through the 
[deployment pipeline](http://martinfowler.com/bliki/DeploymentPipeline.html).

* Predictability and Clarity  
Configuration is strongly-typed and express the intents and rules under which circumstances
the application can promise correct behaviour. Violations are handled reliably and does not disturb application
behaviour. Valid changes are applied and exposed to applications in a consistent way. Developers should feel 
confident, safe and encouraged to make functionality configurable in order to make their application (more) flexible. 

* Extendability and Portability  
As applications are developed in different shapes and sizes; configuration should enable, not limit, a diversity 
of platforms and technologies.  Applications are supported with the flexibility to extend and customize a variety 
aspects locally and still be able to manage configuration in a central and unified way.

* Performance and Scalability  
Configuration should not be a limiting factor to application performance. It is quickly accessible to be able 
to meet service-level agreements in environments of scale.

## Examples

The core framework consist of a single jar file (only dependent on [Guava](http://code.google.com/p/guava-libraries/)) 
and is available in Maven Central.

```xml
    <dependency>
      <groupId>org.deephacks</groupId>
      <artifactId>confit</artifactId>
      <version>${version.confit}</version>
    </dependency>
```

### Define

Configurable classes are annotated with the @Config annotation and all fields will treated as configurable
as long they are not final or static. All basic Java object data types and all enums are supported, including 
List fields of these types.

Class A is a singleton, which means that there can be only one instance of this class.

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

Class B can have multiple instances, hence the @Id annotation which is used to identify instances.

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

* A set operation will replace existing instance entirely with the provided instance.
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

Deleting an instance does not cascade with regards to references that instances may have.

```java
    A a = new A();
    admin.deleteObject(a);
```

There is no explicit operation for deleting individual properties but this can be achieved by
first deleting the property on the target instance and execute a 'set' operation. 

Deleting a property will reset the field back to its default value.

### Default values

Initial values in field declarations are treated as a default values. They are used as fallback values, 
merged with the instance if no configuration exist for those particular fields. 


```java
    @Config(name = "A")
    public class A {
      private Integer integer = 10;
      private Double decimal = 10.0;
      private List<String> stringList = Lists.newArrayList("a", "b", "c");
    }
```


```java
    A a = new A();
    a.setInteger(30);
    admin.createObject(a);
    
    a = admin.get(A.class);
    // returns the provisioned value 30
    a.getInteger();
    // returns the default value 10.0
    a.getDecimal();
    // returns the list a,b,c
    a.getStringList();
```

Default values cannot be declared on fields that contain references, only simple values are allowed.

### References

Any instance can have references to other instances as seen on the last three fields of class A. If the 
reference class does not have a @Config annotation (or qualify as a simple type) an excpetion will be thrown. 
References are allowed to be circular and can be single valued, a List or a Map with a string key (the id).

References are administrated in the same way as simple values. Add or remove references on the instance 
and then perform the operation using AdminContext.


```java
    A a = new A();
    B one = new B("1");
    B two = new B("2");
    B three = new B("3");
    
    a.setListReferences(one, two, three);
    // create instance and references in a single operation
    admin.createObjects(Arrays.asList(a, one, two, three));
    
    // merge 'a' list references to the single instance 'three'
    a.setListReferences(three);
    admin.mergeObject(a);
```

AdminContext will do referential checks to make sure that instances exist, or throw an exception otherwise.
The same is true when trying to delete instances already referenced by others.

```java
    a.setListReferences(four);
    // fails if 'four' have not been created earlier
    admin.createObject(a);

    // fails if 'a' reference 'two'
    admin.deleteObject(two);
```

### Validation

Bean validation 1.1 is enabled by adding an implementation to classpath, like 
[hibernate-validator](http://www.hibernate.org/subprojects/validator.html) for example.

```xml
    <dependency>
      <groupId>org.hibernate</groupId>
      <artifactId>hibernate-validator</artifactId>
      <version>5.0.0.Final</version>
      <scope>compile</scope>
    </dependency>
```

Creating, updating or deleting instances are now validated according to constraint annotations declared 
on configurable fields. Custom 
[ConstraintValidator](http://docs.jboss.org/hibernate/stable/beanvalidation/api/javax/validation/ConstraintValidator.html) 
may also be used.


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

Configurable fields are not limited to simple Java types, any object that can be represented as a string can
be made configurable.

Being able to define custom data types is a very important part configuration. Developers are encouraged to
create new types whenever they can in order to be very precise about their intents with the configuration. 
Instances are constructed whenever a value changes, which give the class a chance to reject the change and 
report errors if the value was invalid. This give data a very natural place for validation, business logic, 
documentation and at the same time giving users a type safe way of accessing it. 

Say you want to configure a ISO8601 DateTime, an IP address, a duration or whatever.

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

This class is accepted directly when declared on configurable fields because it has a default String constructor 
and a toString method that return a String representation that can be used to re-construct the DurationTime instance.
This is the reason why classes like URL and File can be used out of the box.

It is also possible to register custom string converters for types that does not have a String constructor. Here is an 
example of how Boolean values are converted internally (which is similar to how enum types are handled).


```java
    static final class StringToBooleanConverter implements Converter<String, Boolean> {
        private static final Set<String> trueValues = new HashSet<String>();
        private static final Set<String> falseValues = new HashSet<String>();

        static {
            trueValues.addAll(Arrays.asList("true", "on", "yes", "y", "1"));
            falseValues.addAll(Arrays.asList("false", "off", "no", "n", "0"));
        }

        @Override
        public Boolean convert(String source, Class<? extends Boolean> specificType) {
            final String value = source.trim();
            if (trueValues.contains(value)) {
                return Boolean.TRUE;
            } else if (falseValues.contains(value)) {
                return Boolean.FALSE;
            } else {
                throw new ConversionException("Invalid boolean value '" + source + "'");
            }
        }
    }

```

You can get __very creative__ with the fact that every type must be able to represent itself as a String. How about making
the following things configurable?

* Base64 encoding of images, music or other binary data.
* Serialize entire object graphs using serialization frameworks like [Kryo](http://code.google.com/p/kryo/) or [XStream](http://xstream.codehaus.org).
* Java class files with some classloader tricks and customizations, combined with configuration notifications
* Logback or log4j configurations that may propagate changes to the entire cluster
* Documents or wikipages

Some downsides with having proprietary formats as configurable values is that they cannot be queried and doesnt have
sensible default values. These issues can be solved, but it would require some compromises and extra effort.


### Default configuration file


Configuration can be bootstrapped using a [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) 
file (simplified, human optimized, JSON format). A file called application.conf will be loaded by default, if available on classpath
(it is possible to change the location of this file by setting a system property). This file is read-only and 
will not be modified by AdminContext. 

Instead, the file work as a fallback that is used only if no configuration is available for a particular class. 
Configuration provisioned by AdminContext takes precedence. Note also that file configuration override default values.

File configuration is validated according to class schema in the same way as other configuration. Validation is
performed when configuration is read the first time, which means that an application can break if configuration
is invalid.

This is an example of a HOCON application.conf, containing configuration of class A and B mentioned earlier.


    # class A, singleton no instances defined.
    A {
      # string
      value = someValue
      # double
      decimal = 1.243453
      # custom DurationTime
      customType = PT15H
      # list of URL
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
        # list of TimeUnit enum
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

Configuration is stored in-memory by default, which may not be desirable in deployments with availability 
and durability requirements. 

At the moment there are provider extensions that allow storing configuration on local disk as files, in a 
relational database using JPA or in HBase. Default storage is changed by adding a provider to classpath, which will automatically override the default
in-memory implementation. Using either of these storage mechanism, or switching between them, does not
impact client code what so ever.

As mentioned earlier, any configuration in persistence storage will override instances in the default 
[HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md)  configuration file.

Even if a persistence provider is used in production, the default in-memory storage can be very useful in
basic tests since it initalize and get provisioned/cleared very quickly without requiring complex and time
consuming setup of external dependencies.

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

Another option is [HBase](http://hbase.apache.org/), which is a distributed, scalable, big data store. 
It is ideal for storing configuration because of its schema-less nature, allowing instances to be stored in
a very compressed binary format.

HBase does not have foreign keys or joins, which normally is a problem when dealing with references. 
However, these features have been implemented to support referential integrity and eager fecthing of
configurable instances.

The hbase module implements a custom [filter](http://hbase.apache.org/apidocs/org/apache/hadoop/hbase/filter/Filter.html)
highly optimized for doing fast queries and pagination over huge amounts of configuration. This jar
must be deployed on every region server and the easiest way to do this is to put it in the lib directory,
or append it to HBASE_CLASSPATH.

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

Configuration may not be accessed quickly enough by the application when several thousands instances 
exist in storage but several mechanisms exist to reduce lookup latency. Caching is used to avoid costly disk
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

* Benchmark

To give an idea of how queries perform a simple benchmark consisting of 10000 instances 
measured the retrieval time (or latency) for the same query using iteration and query. 

With the iteration approach all instances are stored in a collection (not using configuration) and 
simply iterated over to find the correct instance. 

```java
   // list of 10000 instances not stored as configuration
   ArrayList<B> instances = .. // creation omitted for brevity
   
   List<B> found = new ArrayList();
   Stopwatch time = new Stopwatch().start();
   for (B b : instances) {
     if (b.getValue().equals("someValue")) {
       found.add(b);
     }
   }
   long elapsed = time.elapsed(TimeUnit.MICROSECONDS);

```

The same test using the configuration query approach.

```java
    // asssuming AdminContext have been provisioned with the same 10000 instances
    Stopwatch time = new Stopwatch().start();
    ConfigResultSet<B> result = config.newQuery(B.class).add(equal("value", "someValue")).retrieve();
    // force retrieval since the result set is lazy, which
    // includes deserializing instances from the off-heap cache
    List<B> found = Lists.newArrayList(result);
    long elapsed = time.elapsed(TimeUnit.MICROSECONDS);
```

The result from the benchmark show that iteration have a latency of ~2500 microseconds and the query 
~300 microseconds. If we increase the number of instances 3 times (30000), iteration is almost
exactly 3 times slower (8.2 ms) while the query finish in ~450 microseconds.

Why? Because the configurable field has an index. This means that we can pick a better algorithm to
find instances than simply iterating through the list, which can be hugely inefficient as number of
instances increase. But keep in mind that maintaining an index will make create, update and delete operations
slower.



### Administrative queries and pagination

Having several thousands or more configuration instances can quickly become difficult to administrate.
AdminContext have therefore been equipped with query and pagination capabilities to make configuration
management easier for the administrator. 

Forward pagination is supported, backward or seeking to an arbitary page is not. Clients are
responsible for implementing such features if needed (maybe using page caching or similar).

Example query using AdminContext.

```java
    BeanQuery query = admin.newQuery("Employee")
                             .add(lessThan("salary", 10000.0))
                             .add(contains("email", "gmail"))
                             .setMaxResults(50)
    // first page
    BeanQueryResult result = query.retrieve();
    
    // page forward by using same query and next first result of first page
    BeanQueryResult next = query.setFirstResult(result.nextFirstResult())
                             .setMaxResults(50)
                             .retrieve();


```

### Notifications

Some applications need to perform certain actions as configuration is created, updated or deleted. Maybe 
pushing notifications to a graphical UI, rebuilding internal state (like a cache or 
redirect a TCP connection) or similar. 

Notifications are sent only after changes have been successfully validated, committed to storage and cached. 
Observers receive notifications once and any exception (thrown by observer) will be ignored, no retries.
Notification failures will not affect delivery to other observers. Notifications convey
information on state before and after a change was made to support fine-grained comparisons.


Observers are created by implementing ConfigObserver and registering them with ConfigContext. 


```java
    config.registerObserver(new Observer());
```

Example of an observer.

```java
    public class Observer implements ConfigObserver {

      public void notify(ConfigChanges changes) {
        
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
### Remote administration using REST

Configuration can be administrated remotely through the JAX-RS 2.0 server endpoint using either 
the REST interface or the AdminContext client proxy. The proxy is just a simple convenience 
class that extend AdminContext and provide the same capabilities (as if it would be running locally)
without burdening users with HTTP or JAX-RS details.


```java
    // the client proxy implement AdminContext seamlessly
    AdminContext admin = AdminContextJaxrsProxy.get("localhost", 8080);
    
    admin.createObject(new A());
    admin.createObjects(Arrays.asList(new B("1"), new B("2")));
```

Configuration can also be administrated from a shell using curl and the REST interface.


```sh
# fetch instance 10 - 20 for schema 'A' in JSON format
$ curl -X GET "http://127.0.0.1:8080/confit-admin/query/A?first=10&max=20"
```
```sh
# create instance 1 of schema 'A' with property 'value' set to 'someValue'
$ curl -i -H "Content-Type: application/json" -X POST -d '{"schemaName":"A","id":"1","properties":{"value":"somevalue"}}' http://127.0.0.1:8080/confit-admin/createBean
```

The REST endpoint can be deployed in [Jetty](http://www.eclipse.org/jetty/) using [RestEasy](http://www.jboss.org/resteasy)
(for example). Have a look at the JAX-RS tests.


### Graphical administration using Angular.js

Conf-it comes with a lightweight [angular.js](http://angularjs.org/) [bootstrap](http://twitter.github.io/bootstrap/)
application that use the REST interface to visualize and administrate configuration graphically.

The graphical interface is generated dynamically at runtime by discovering class schemas from the server. 
Classes that are registered through the ConfigContext will appear immediatly in the GUI, at runtime.

Above all, this is a developer tool intended to give a quick overview of configuration in the system, used maybe
to do exploratory tests. The GUI have search and pagination capabilities.


### Sessions

Planned feature that will enable multiple isolated changes to be committed or rollback in the context of a session.

### ConfigContext vs AdminContext

ConfigContext and AdminContext may seem very similar, but there are important differences between them.


ConfigContext is used in an application context, where high volume traffic is processed from the outside 
world with sub-millisecond latency requirements. Applications load/fetch configuration when needed and 
never administrate configuration on their own. As far as they are concerned, configuration is read-only. 


AdminContext, on the other hand, is used by administrators (not literally) to manage configuration
published by applications. The administrator is probably a human, but not necessarily. Expectations on
traffic volume and latency are not as high in this context as for the application itself.

The important point is that configuration is read mostly, with a slow pace of change, not changing
every second. This is what enable heavy optimizations on read operations, but still able to handle
persistent changes, in runtime, without restarting applications.


### CDI integration and @Inject

Singleton class configuration can be injected into [CDI](http://www.cdi-spec.org/) beans using the @Inject annotation.
AdminContext and ConfigContext can also be injected. Configuration that have multiple instances cannot be injected at the
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

Conf-it support development of service providers that implement internal, but reasonably stable,
interfaces in order to customize certain well defined aspects like how instances are stored or validated. 
For such efforts there is a TCK that provide a set of tests to ensure that implementations behave
in the intended way, including in error conditions.

* LookupProvider

[ServiceLoader](http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html) 
and [CDI](http://www.cdi-spec.org/) are used to lookup manager implementations by default, but 
virtually any method can be used by registering additional LookupProviders (Spring, OSGi, Guice, JNDI, etc).
Lookup providers themselves are registered using the standard Java ServiceLoader mechanism.

* Converter

Handle serialization of objects, as mentioned earlier. A converter enable declaration and configuration of 
simple values that have specific types.

* SchemaManager

Manage schema discovery, conversion, storage and data type validation.

Default: Reflection based, storing schema in memory.

* BeanManager

Responsible for storing configuration and keeping it consistent.

Default: In memory storage.

* PropertyManager

Reads configuration from property files using an unspecified format, used 
for bootstrap and fallback for configuration that do not exist in the BeanManager.
Also used internally by other managers to configure themselves and force preferred lookup.

Default: [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) file format.

* ValidationManager

Maintain data integrity by enforcing validation constraints and reject operations that violate these rules.

Default: [JSR303](http://beanvalidation.org/1.0/spec/) validation.

* NotificationManager

Keeps track of observers and sends notifications to them when changes occur.

Default: Non-clustered, in-process notifications.

* SessionManager

Manage sessions and configuration changes before they are committed to the BeanManager.

* CacheManager

Cache configurable instances and perform application queries on behalf of applications.

Default: Off-heap storage with [javassist](http://www.jboss.org/javassist) proxies and 
heap based indexes based on [cqengine](https://code.google.com/p/cqengine/).

## Licensing

This distribution, as a whole, is licensed under the terms of the Apache License, Version 2.0 (see license.txt).



