package org.deephacks.confit.internal.jpa;

import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Event;
import org.deephacks.confit.model.EventDoc;

import java.text.MessageFormat;

public class JpaEvents {
    /**
     * Unique identifier of this module: {@value}
     */
    public static final String MODULE_NAME = "confit.support-web";
    /**
     * {@value} - Success.
     */
    public static final int JPA001 = 1;
    static final String JPA001_MSG = "Success.";

    @EventDoc(module = MODULE_NAME, code = JPA001, desc = JPA001_MSG)
    static Event JPA001_SUCCESS() {
        return new Event(MODULE_NAME, JPA001, JPA001_MSG);
    }

    /**
     * {@value} - JPA property filepath system variable not set.
     */
    public static final int JPA201 = 201;
    private static final String JPA201_MSG = "JPA property filepath system variable [{0}] not set.";

    @EventDoc(module = MODULE_NAME, code = JPA201, desc = JPA201_MSG)
    public static AbortRuntimeException JPA201_PROP_FILEPATH_MISSING(String prop) {
        Event event = new Event(MODULE_NAME, JPA201, MessageFormat.format(JPA201_MSG, prop));
        throw new AbortRuntimeException(event);
    }

    /**
     * {@value} - An Entity Manager was not found with the current thread local.
     */
    public static final int JPA202 = 202;
    private static final String JPA202_MSG = "An Entity Manager was not found with the current thread local.";

    @EventDoc(module = MODULE_NAME, code = JPA202, desc = JPA202_MSG)
    public static AbortRuntimeException JPA202_MISSING_THREAD_EM() {
        Event event = new Event(MODULE_NAME, JPA202, JPA202_MSG);
        throw new AbortRuntimeException(event);
    }

    /**
     * {@value} - Entity Manager Factory was not found at lookup.
     */
    public static final int JPA203 = 203;
    private static final String JPA203_MSG = "Entity Manager Factory was not found at lookup.";

    @EventDoc(module = MODULE_NAME, code = JPA203, desc = JPA203_MSG)
    public static AbortRuntimeException JPA203_MISSING_THREAD_EMF() {
        Event event = new Event(MODULE_NAME, JPA203, JPA203_MSG);
        throw new AbortRuntimeException(event);
    }

}