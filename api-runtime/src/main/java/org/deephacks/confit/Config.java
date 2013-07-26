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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * This annotation is used to mark a classes and fields as configurable and serves as a specification
 * of to express the structure, constraints and conditions under which instances can exist to
 * fulfill their intended purpose. Instances of configurable classes must always be unique
 * with respect to {@link Id}.
 * </p>
 * <p>
 * Changing configuration should never cause system failure or malfunctioning. Configurables should
 * therefore make sure that their properties have proper validation rules so that administrators
 * does not accidentally misconfigure the system.
 * </p>
 * <p>
 * <ul>
 * <li>Fields can be single-valued or multi-valued using any subclass of {@link java.util.Collection} type.</li>
 * <li>Fields can be any subclass of {@link java.util.Map} type, but this is only allowed for
 * referencing other {@link Config}, where key is the {@link Id} parameterized as {@link java.lang.String}.</li>
 * <li>Fields can be <b>final</b> in which case it is considered immutable.</li>
 * <li>Fields are not allowed to be <b>transient</b>.</li>
 * <li>Fields are not allowed to be non-<b>final</b> <b>static</b>.</li>
 * <li>Fields can reference other {@link Config} classes, single or multiple using any subclass of
 * {@link java.util.Collection} type.</li>
 * <li>Fields can have default values. These are used if no values have been set.</li>
 * </p>
 * @author Kristoffer Sjogren
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
@Inherited
public @interface Config {
    /**
     * <p>
     * An informative name that clearly identifies the configuration in the system.
     * Good names are those which describe domain specific aspects already established
     * in the system architecture.
     * </p>
     * <p>
     * Names will be displayed to administrative users and must be unique within the
     * system.
     * </p>
     * If no name is choosen the field or class name will picked instead.
     *
     * @return Name of the configurable.
     */
    String name() default "";

    /**
     * An informative description that justify the existence of the configuration,
     * putting it into context for how it relates to high-level system concepts and
     * ouline what it is used for and how changes affect the behaviour of
     * the system.
     * <p>
     * Descriptions will be displayed to administrative users.
     * </p>
     * @return A description.
     */
    String desc() default "";

}
