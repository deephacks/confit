/**
* This package contains service provider interfaces (SPI) that enable applications to 
* replace, extend and customize different configuration aspects such as storage, caching behaviour, 
* runtime environment and so forth. 
* <p>
* Each SPI deals with a very specific concern within the architecture and service providers are
* free to implement any of them. A key design goal for each SPI is to be decoupled from other SPI to 
* enable providers to create modular implementations that are orthogonal and cohesive with 
* respect to other SPI. This enables interaction between unforseen combinations of service providers.  
* <p>
* Provider services are registered and automatically bound to core functionality when 
* the application is started/loaded. It is the responsibility of the core to glue/mediate communication 
* between different providers. The mechanism used is the standard {@link java.util.ServiceLoader} 
* at the moment, but other lookup/registrations (OSGi, CDI SPI extensions, JNDI etc) are envisioned. 
* <p>
* A Test Compability Kit (TCK) is available for providers to verify that their 
* implementation behaves correctly.
* </p>
* <p> 
* Each SPI have a default implementation that will be used if no other is provided. 
* </p>
* 
* <p>
* The following SPIs are available at the moment:
* <ul>
* <li> {@link org.deephacks.confit.spi.BeanManager}</li>
* <li> {@link org.deephacks.confit.spi.SchemaManager}</li>
* <li> {@link org.deephacks.confit.spi.ValidationManager}</li>
* <li> {@link org.deephacks.tools4j.support.conversion.Converter}</li>
* This interface takes care of converting configuration property types to-and-from String 
* (which is the format for storing property values).
* </ul> 
* </p>
* 
* <p>
* The following SPIs are envisioned in the future: 
* <ul>
* <li> SessionManager</li>
* Doing configuration changes in the context of a session that can commit and validate (or rollback) atomically. 
* <li> CachingManager</li>
* Implements a cache policy of runtime instances, such as eviction, memory size, storage, passivation etc.
* <li> NotificationManager</li>
* Provide applications with notification guarantees, making them aware of changes made to their configuration 
* in case an adjusting actions needs to be taken within their runtime environment.
* </ul>
* </p>   
* <h2>Usage</h2>
* 
* TODO:
*/
package org.deephacks.confit.spi;

;