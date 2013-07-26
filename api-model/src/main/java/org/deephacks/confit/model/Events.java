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

import org.deephacks.confit.model.Bean.BeanId;

import java.io.File;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Collection;

/**
 * Defines all the anticipated events that may occur within the typesafe module and its
 * service providers in a canonical format.
 *
 * @author Kristoffer Sjogren
 */
public final class Events {
    /**
     * Unique identifier of this module: {@value}
     */
    public static final String MODULE_NAME = "tools4j.config";
    /**
     * {@value} - Success.
     */
    public static final int CFG001 = 1;
    static final String CFG001_MSG = "Success.";

    @EventDoc(module = MODULE_NAME, code = CFG001, desc = CFG001_MSG)
    static Event CFG001_SUCCESS() {
        return new Event(MODULE_NAME, CFG001, CFG001_MSG);
    }

    /**
     * {@value} - There was a modification conflict.
     */
    public static final int CFG088 = 88;
    private static final String CFG088_MSG = "Invalid data.";

    @EventDoc(module = MODULE_NAME, code = CFG089, desc = CFG089_MSG)
    public static AbortRuntimeException CFG088_INVALID_DATA() {
        Event event = new Event(MODULE_NAME, CFG088, CFG088_MSG);
        return new AbortRuntimeException(event);
    }

    public static AbortRuntimeException CFG088_INVALID_DATA(String msg) {
        Event event = new Event(MODULE_NAME, CFG088, msg);
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - There was a modification conflict.
     */
    public static final int CFG089 = 89;
    private static final String CFG089_MSG = "There was a modification conflict.";

    @EventDoc(module = MODULE_NAME, code = CFG089, desc = CFG089_MSG)
    public static AbortRuntimeException CFG089_MODIFICATION_CONFLICT() {
        Event event = new Event(MODULE_NAME, CFG089, CFG089_MSG);
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Schema or id does not exist.
     */
    public static final int CFG090 = 90;
    private static final String CFG090_MSG = "Schema or id was not found.";

    @EventDoc(module = MODULE_NAME, code = CFG090, desc = CFG090_MSG)
    public static AbortRuntimeException CFG90_SCHEMA_OR_ID_NOT_EXIST() {
        Event event = new Event(MODULE_NAME, CFG090, CFG090_MSG);
        return new AbortRuntimeException(event);
    }


    /**
     * {@value} - Config schema does not exist.
     */
    public static final int CFG101 = 101;
    private static final String CFG101_MSG = "Config schema does not exist: {0}";

    @EventDoc(module = MODULE_NAME, code = CFG101, desc = "Config schema does not exist.")
    public static AbortRuntimeException CFG101_SCHEMA_NOT_EXIST(String schemaName) {
        Event event = new Event(MODULE_NAME, CFG101, MessageFormat.format(CFG101_MSG, schemaName));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Class is not configurable.
     */
    public static final int CFG102 = 102;
    private static final String CFG102_MSG = "Class is not configurable: {0}";

    @EventDoc(module = MODULE_NAME, code = CFG102, desc = "Class is not configurable.")
    public static AbortRuntimeException CFG102_NOT_CONFIGURABLE(Class<?> clazz) {
        Event event = new Event(MODULE_NAME, CFG102, MessageFormat.format(CFG102_MSG,
                clazz.getName()));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Configurable class lacks a Id annotation.
     */
    public static final int CFG103 = 103;
    private static final String CFG103_MSG = "Configurable class lacks a Id annotation: {0}";

    @EventDoc(module = MODULE_NAME, code = CFG103,
            desc = "Configurable class lacks a Id annotation.")
    public static AbortRuntimeException CFG103_NO_ID(Class<?> clazz) {
        Event event = new Event(MODULE_NAME, CFG103, MessageFormat.format(CFG103_MSG,
                clazz.getName()));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Configurable class have a property of unsupported type.
     */
    public static final int CFG104 = 104;
    private static final String CFG104_MSG = "Configurable class {0} have a property {1} of unsupported type: {2}";

    @EventDoc(module = MODULE_NAME, code = CFG104,
            desc = "Configurable class have a property of unsupported type.")
    public static AbortRuntimeException CFG104_UNSUPPORTED_PROPERTY(Class<?> clazz,
            String property, Class<?> type) {
        Event event = new Event(MODULE_NAME, CFG104, MessageFormat.format(CFG104_MSG,
                clazz.getName(), property, type.getName()));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean have property value that does not match schema.
     */
    public static final int CFG105 = 105;
    private static final String CFG105_MSG = "Bean {0} have a property {1} with value {2} not matching its type.";

    @EventDoc(module = MODULE_NAME, code = CFG105,
            desc = "Bean have property value that does not match schema.")
    public static AbortRuntimeException CFG105_WRONG_PROPERTY_TYPE(BeanId id, String propertyName,
            String type, String value) {
        Event event = new Event(MODULE_NAME, CFG105, MessageFormat.format(CFG105_MSG, id, type
                + "@" + propertyName, value));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean have a property value of invalid multiplicity type according to schema.
     */
    public static final int CFG106 = 106;
    private static final String CFG106_MSG = "Bean {0} have a property {1} with invalid multiplicity type value.";

    @EventDoc(module = MODULE_NAME, code = CFG106,
            desc = "Bean have a property value of invalid multiplicity type according to schema.")
    public static AbortRuntimeException CFG106_WRONG_MULTIPLICITY_TYPE(BeanId id,
            String propertyName) {
        Event event = new Event(MODULE_NAME, CFG106, MessageFormat.format(CFG106_MSG, id,
                propertyName));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean is lacking identification.
     */
    public static final int CFG107 = 107;
    private static final String CFG107_MSG = "Bean lacks identification.";

    @EventDoc(module = MODULE_NAME, code = CFG107, desc = CFG107_MSG)
    public static AbortRuntimeException CFG107_MISSING_ID() {
        Event event = new Event(MODULE_NAME, CFG106, CFG107_MSG);
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean property have illegal modifiers.
     */
    public static final int CFG108 = 108;
    private static final String CFG108_MSG = "Bean property {0} have illegal modifiers.";

    @EventDoc(module = MODULE_NAME, code = CFG108, desc = CFG108_MSG)
    public static AbortRuntimeException CFG108_ILLEGAL_MODIFIERS(Field field) {
        Event event = new Event(MODULE_NAME, CFG108, MessageFormat.format(CFG108_MSG, field.toString()));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean property have illegal map parameterization.
     */
    public static final int CFG109 = 109;
    private static final String CFG109_MSG = "Bean property {0} have illegal map parameterization.";

    @EventDoc(module = MODULE_NAME, code = CFG109,
            desc = "Bean property have illegal map parameterization.")
    public static AbortRuntimeException CFG109_ILLEGAL_MAP(String propertyName) {
        Event event = new Event(MODULE_NAME, CFG109, MessageFormat.format(CFG109_MSG, propertyName));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean have property that does not exist in schema.
     */
    public static final int CFG110 = 110;
    private static final String CFG110_MSG = "Bean property [{0}] does not exist in schema.";

    @EventDoc(module = MODULE_NAME, code = CFG110,
            desc = "Bean have property that does not exist in schema.")
    public static AbortRuntimeException CFG110_PROP_NOT_EXIST_IN_SCHEMA(String propertyName) {
        Event event = new Event(MODULE_NAME, CFG110, MessageFormat.format(CFG110_MSG, propertyName));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean have reference that does not exist in schema.
     */
    public static final int CFG111 = 111;
    private static final String CFG111_MSG = "Bean reference [{0}] does not exist in schema.";

    @EventDoc(module = MODULE_NAME, code = CFG111,
            desc = "Bean have reference that does not exist in schema.")
    public static AbortRuntimeException CFG111_REF_NOT_EXIST_IN_SCHEMA(String referenceName) {
        Event event = new Event(MODULE_NAME, CFG111,
                MessageFormat.format(CFG111_MSG, referenceName));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - XML schema storage file does not exist.
     */
    public static final int CFG202 = 202;
    static final String CFG202_MSG = "XML schema storage file does not exist: {0}";

    @EventDoc(module = MODULE_NAME, code = CFG202, desc = "XML schema storage file does not exist.")
    public static AbortRuntimeException CFG202_XML_SCHEMA_FILE_MISSING(File dir) {
        Event event = new Event(MODULE_NAME, CFG202, MessageFormat.format(CFG202_MSG,
                dir.getAbsolutePath()));
        throw new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean have a missing runtime references.
     */
    public static final int CFG301 = 301;
    private static final String CFG301_MSG_1 = "Bean {0} have a missing runtime references: {1}";
    private static final String CFG301_MSG_2 = "Bean {0} have a missing runtime references.";
    private static final String CFG301_MSG_3 = "A bean have a missing runtime references.";

    @EventDoc(module = MODULE_NAME, code = CFG301, desc = "Bean have a missing references.")
    public static AbortRuntimeException CFG301_MISSING_RUNTIME_REF(BeanId id, BeanId ref) {
        Event event = new Event(MODULE_NAME, CFG301, MessageFormat.format(CFG301_MSG_1, id, ref));
        return new AbortRuntimeException(event);
    }

    public static AbortRuntimeException CFG301_MISSING_RUNTIME_REF(BeanId id, Collection<BeanId> ref) {
        Event event = new Event(MODULE_NAME, CFG301, MessageFormat.format(CFG301_MSG_1, id, ref));
        return new AbortRuntimeException(event);
    }

    public static AbortRuntimeException CFG301_MISSING_RUNTIME_REF(BeanId id) {
        Event event = new Event(MODULE_NAME, CFG301, MessageFormat.format(CFG301_MSG_2, id));
        return new AbortRuntimeException(event);
    }

    public static AbortRuntimeException CFG301_MISSING_RUNTIME_REF() {
        Event event = new Event(MODULE_NAME, CFG301, CFG301_MSG_3);
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - One or more beans cannot be deleted beacuse of existing references from other beans.
     */
    public static final int CFG302 = 302;
    private static final String CFG302_MSG = "One or more beans {0} cannot be deleted because of existing references from other beans.";

    @EventDoc(
            module = MODULE_NAME,
            code = CFG302,
            desc = "One or more beans cannot be deleted beacuse of existing references from other beans. ")
    public static AbortRuntimeException CFG302_CANNOT_DELETE_BEAN(Collection<BeanId> id) {
        Event event = new Event(MODULE_NAME, CFG302, MessageFormat.format(CFG302_MSG, id));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean with same identification already exist.
     */
    public static final int CFG303 = 303;
    private static final String CFG303_MSG = "Bean with id {0} already exist.";

    @EventDoc(module = MODULE_NAME, code = CFG303,
            desc = "Bean with same identification already exist.")
    public static AbortRuntimeException CFG303_BEAN_ALREADY_EXIST(BeanId id) {
        Event event = new Event(MODULE_NAME, CFG303, MessageFormat.format(CFG303_MSG,
                id.getInstanceId()));
        return new AbortRuntimeException(event);
    }

    public static AbortRuntimeException CFG303_BEAN_ALREADY_EXIST() {
        Event event = new Event(MODULE_NAME, CFG303, "Bean with same identification already exist.");
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean does not exist.
     */
    public static final int CFG304 = 304;
    private static final String CFG304_MSG = "Bean with id {0} does not exist.";

    @EventDoc(module = MODULE_NAME, code = CFG304, desc = "Bean does not exist.")
    public static AbortRuntimeException CFG304_BEAN_DOESNT_EXIST(BeanId id) {
        Event event = new Event(MODULE_NAME, CFG304, MessageFormat.format(CFG304_MSG, id));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Property does not exist.
     */
    public static final int CFG305 = 305;
    private static final String CFG305_MSG = "Property {0} does not exist.";

    @EventDoc(module = MODULE_NAME, code = CFG305, desc = "Property does not exist.")
    public static AbortRuntimeException CFG305_PROPERTY_DOESNT_EXIST(String propertyName) {
        Event event = new Event(MODULE_NAME, CFG305, MessageFormat.format(CFG305_MSG, propertyName));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Property is not mutable.
     */
    public static final int CFG306 = 306;
    private static final String CFG306_MSG = "Property {0} for bean with id {1} is not mutable.";

    @EventDoc(module = MODULE_NAME, code = CFG306, desc = "Property is not mutable.")
    public static AbortRuntimeException CFG306_PROPERTY_IMMUTABLE(BeanId id, String propertyName) {
        Event event = new Event(MODULE_NAME, CFG306, MessageFormat.format(CFG306_MSG, id,
                propertyName));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Singleton beans cannot be removed.
     */
    public static final int CFG307 = 307;
    private static final String CFG307_MSG = "Singleton bean {0} cannot be removed.";

    @EventDoc(module = MODULE_NAME, code = CFG307, desc = "Singleton beans cannot be removed.")
    public static AbortRuntimeException CFG307_SINGELTON_REMOVAL(BeanId id) {
        Event event = new Event(MODULE_NAME, CFG307, MessageFormat.format(CFG307_MSG, id));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Bean application validation failed.
     */
    public static final int CFG309 = 309;
    private static final String CFG309_MSG = "Bean application validation failed: {0}";

    @EventDoc(module = MODULE_NAME, code = CFG309, desc = "Bean application validation failed.")
    public static AbortRuntimeException CFG309_VALIDATION_ERROR(String msg) {
        Event event = new Event(MODULE_NAME, CFG309, MessageFormat.format(CFG309_MSG, msg));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Beans of same type have circular references.
     */
    public static final int CFG310 = 310;
    private static final String CFG310_MSG = "Beans of same type have circular references: {0} {1}";

    @EventDoc(module = MODULE_NAME, code = CFG310,
            desc = "Beans of same type have circular references.")
    public static AbortRuntimeException CFG310_CIRCULAR_REF(BeanId a, BeanId b) {
        Event event = new Event(MODULE_NAME, CFG310, MessageFormat.format(CFG310_MSG, a, b));
        return new AbortRuntimeException(event);
    }

    /**
     * {@value} - Default beans cannot be removed.
     */
    public static final int CFG311 = 311;
    private static final String CFG311_MSG = "Default bean {0} cannot be removed.";

    @EventDoc(module = MODULE_NAME, code = CFG311, desc = "Default beans cannot be removed.")
    public static AbortRuntimeException CFG311_DEFAULT_REMOVAL(BeanId id) {
        Event event = new Event(MODULE_NAME, CFG311, MessageFormat.format(CFG311_MSG, id));
        return new AbortRuntimeException(event);
    }
}
