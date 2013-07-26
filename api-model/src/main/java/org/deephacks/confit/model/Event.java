package org.deephacks.confit.model;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Events are any occurence in the system that may information to be reported to somewhere
 * somehow. Events can be reported to multiple users and communication channels. 
 * 
 * Example of events are: successful execution/acceptance of a request or asynchronous job, 
 * encountering any kind of faults or failures (both abortable and recoverable), informative 
 * log statement of state changes, transaction-logs etc. 
 *  
 * Events can be reported to different communication channels and users: A log file for 
 * administrators/developers, inside exceptions thrown to end-users or different sub-systems,
 * as return value etc.
 * 
 * It should be possible to use module and code to uniquely correlate the event within 
 * the system.
 * 
 * Events are considered part of the public interface of a modulem, thus it is ruled by 
 * interface compatibility guidelines. 
 * 
 * TODO: fix localization.
 * 
 * @author Kristoffer Sjogren
 */
public class Event implements Serializable {

    private static final long serialVersionUID = 7861119352068414808L;
    private String module;
    private int code;
    private String message;
    private Event origin;

    public Event(String module, int code, String message) {
        this.module = Preconditions.checkNotNull(module);
        this.code = Preconditions.checkNotNull(code);
        this.message = Preconditions.checkNotNull(message);
        Preconditions.checkArgument(!message.equals(""));
    }

    public Event(String module, int statusCode, String message, Event origin) {
        this.module = Preconditions.checkNotNull(module);
        this.code = Preconditions.checkNotNull(statusCode);
        this.message = Preconditions.checkNotNull(message);
        Preconditions.checkArgument(!message.equals(""));
        this.origin = Preconditions.checkNotNull(origin);
    }

    /**
     * The module that reported the event. Some modules may be implementation
     * details. Events from these may be appropriate to only report to logging 
     * facilities.
     * 
     * @return a non-null string that uniquely identifies the module.
     */
    public String getModule() {
        return module;
    }

    /**
     * A code uniquely identifies the event that occured within a particular 
     * module.  Same codes can be reused and reported by  modules (i.e. must not be 
     * globally unique across modules).   
     * 
     * It must be possible to correlate module and code to a documented event using 
     * {@link EventDoc}. Do not report events that contain error codes from third party 
     * libraries, for example SQL, HTTP or similar.
     *  
     * If this is a failure, the code should indicate the state of the module and
     * if it is a temporary or permanent condition.  
     *   
     * @return a positive status code.
     */
    public int getCode() {
        return code;
    }

    /**
     * An informative message that explain contextual information/reasons that caused the event. 
     * This message is intended to be displayed to some kind of user and should thus be informative 
     * and understandable. 
     * 
     * It is vital that identification of the collaborating entities exist in the message, 
     * when trying to reduce the time spent diagnosing, troubleshooting or trace why a event 
     * occured.   
     * 
     * Example of useful information could be: user id, address/port, account id, to- and from-values 
     * or similar.   
     * 
     * @return an non-null and non-empty informative message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * A originating event that caused the occurence of this event. Can be used as a way of 
     * constructing a call stack of events. 
     *  
     * @return the originating response, may be null.
     */
    public Event getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Event.class).add("module", module).add("code", code)
                .add("message", message).toString();
    }

}