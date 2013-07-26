package org.deephacks.confit.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * EventDoc is used as a explicit, structured and common way for consolidating, deprecating/introducing and 
 * documenting all the event that may occur in the system. 
 * 
 * There are multiple possible uses for this annotation: it can be used to generate product 
 * documentation (pdf, html or similar), dynamically generate a GUI that display all possible 
 * events in the system or as a mechanism for communicating event compatibility between upgrades.
 * 
 * This makes it easier for users that interact with the system to get a high level overview 
 * and understanding of the events that occur in the system.
 *  
 * @author Kristoffer Sjogren
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.METHOD })
@Inherited
public @interface EventDoc {

    /**
     * A unique identification of the module that reports the event.
     * 
     * @return string that uniquely identifies the module.
     */
    String module();

    /**
     * A code that uniquely identifies (within the module) the event that occured.
     *  
     * @return a positive status code.
     */
    int code();

    /**
     * An informative message that explain the event, reasons why may have occured and
     * maybe possible states that the module may be in.
     * 
     * If the event is an exceptional one, corrective and/or preventive measures may
     * also be described.
     * 
     * This message will be visible in documentation in different formats. Hence it will be 
     * displayable to an administrator or end user. Avoid too many implementation details 
     * and assumptions that may not make sense. Advice to analyse log files if anything else.
     * 
     * @return informative message of the status of the module.
     */
    String desc();

}