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
package org.deephacks.confit;

import com.google.common.base.Optional;
import org.deephacks.confit.query.ConfigQuery;
import org.deephacks.confit.query.ConfigQueryBuilder;

import java.util.List;

/**
 * <p>
 * Central interface for providing configuration to applications.
 * <p>
 * Applications use this interface for registering {@link Config} classes with this runtime context
 * in order to make them visible and available for provisioning in an administrative context.
 * Configuration instances should not be cached, unless applications have very specific caching needs.
 * </p>
 *
 * @author Kristoffer Sjogren
 */
public abstract class ConfigContext {
    private static final String CORE_IMPL = "org.deephacks.confit.internal.core.config.ConfigCoreContext";

    private static ConfigContext ctx;

    protected ConfigContext() {
        // only core should implement this class
        if (!getClass().getName().equals(CORE_IMPL)) {
            throw new IllegalArgumentException("Only RuntimeCoreContext is allowed to"
                    + "implement this interface.");
        }
    }

    /**
     * Get the context for fetching configurable application instances.
     *
     * @return the context.
     */
    public static synchronized ConfigContext lookup() {
        if (ctx != null) {
            return ctx;
        }
        try {
            ctx = (ConfigContext) Class.forName(CORE_IMPL).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ctx;
    }

    /**
     * Register a configurable class and make it visible and available for provisioning in
     * an administrative context.
     *
     * <p>
     * Registering classes are optional, and if not, remember that classes will not be visible
     * for administrative provisioning until they have been touched from this context.
     * </p>
     *
     * <p>
     * If the same class is registered multiple times, the former class is replaced or upgraded.
     * <p>
     * Be cautious of registering a new version of a class that is not compatible with
     * earlier versions of data of the same class.
     * </p>
     *
     * @param configurable {@link Config} classes.
     */
    public abstract void register(final Class<?>... configurable);

    /**
     * Register default configurable instances.
     *
     * Each default instance must have a unique id.
     *
     * Schema must registered before any default instances can be registered.
     *
     * @param instances {@link Config} instances.
     */
    public abstract void registerDefault(final Object... instances);

    /**
     * Remove a configurable class. This will make the schema unavailable for provisioning
     * in an administrative context.
     * <p>
     * Data will never be removed when unregistering a configurable class.
     * </p>
     *
     * @param configurable {@link Config} classes.
     */
    public abstract void unregister(final Class<?>... configurable);

    /**
     * Get a singleton instance.
     *
     * <p>
     * Trying to read instances that are not singletons will result in an error.
     * </p>
     *
     * @param configurable {@link Config} class.
     * @return The lookup instance of {@link Config} T class.
     */
    public abstract <T> T get(final Class<T> configurable);

    /**
     * Get a specific instance with respect to its {@link Id}. All references
     * and properties will be traversed and fetched eagerly.
     *
     * @param id of the instance as specified by {@link Id}.
     *
     * @param configurable {@link Config} class.
     * @return an instance of {@link Config} T class.
     */
    public abstract <T> Optional<T> get(final String id, final Class<T> configurable);

    /**
     * List instances of the the provided type. All references and properties
     * will be traversed and fetched eagerly.
     *
     * @param configurable {@link Config} class.
     * @return list instances of {@link Config} T class.
     */
    public abstract <T> List<T> list(final Class<T> configurable);

    /**
     * Create a new query used for retrieving configurable instances using
     * flexible composition of <tt>Criterion</tt> objects.
     *
     * In order to use queries, a cache manager must be available and each field that
     * is queried must be {@link Index} annotated.
     *
     * This feature is currently experimental and must be turned on by setting
     * 'confit.query.enabled' to true.
     *
     * @see {@link ConfigQueryBuilder}
     *
     * @param configurable {@link Config} class.
     * @param <T> Configurable type
     * @return Used for composing a query in conjuction with {@link ConfigQueryBuilder}.
     */
    public abstract <T> ConfigQuery<T> newQuery(final Class<T> configurable);

    /**
     * Register an observer.
     *
     * @param observer class of the observer.
     */
    public abstract void registerObserver(Object observer);

}
