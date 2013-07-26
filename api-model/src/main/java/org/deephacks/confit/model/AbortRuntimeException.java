/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.confit.model;

import com.google.common.base.Preconditions;

/**
 * AbortRuntimeException is thrown when the system itself cannot recover from a certain event and
 * must therefore abort execution. Users may be able to do corrective measures. The intention is 
 * to let this exception bubble up through the call stack. 
 * 
 * This exception should rarley be logged or caught by other than high level, bootstrap or similar 
 * code - unless the exception needs to propagate and mediate across different abstraction layers.  
 * 
 * Errors reported should be readable, informative and understandable by a human end user
 * or administrator/ops.
 * 
 * @author Kristoffer Sjogren
 */
public class AbortRuntimeException extends RuntimeException {
    private static final long serialVersionUID = -7994691832123397253L;
    private Event event;

    /**
     * @param event this exception was identified to be caused by this event.
     */
    public AbortRuntimeException(Event event) {
        this.event = Preconditions.checkNotNull(event);
    }

    /**
     * @param event this exception was identified to be caused by this event.
     * @param e The exception that caused the exception.
     */
    public AbortRuntimeException(Event event, Exception e) {
        super(e);
        this.event = Preconditions.checkNotNull(event);
    }

    /**
     * Return the event that clearly identifies the failure that occured.
     * 
     * @return a status code.
     */
    public Event getEvent() {
        return event;
    }

    /**
     * A human readable and informative error message.
     */
    @Override
    public String getMessage() {
        return event.toString();
    }
}