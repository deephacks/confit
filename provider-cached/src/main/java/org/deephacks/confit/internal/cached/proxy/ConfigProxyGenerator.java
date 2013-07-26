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
package org.deephacks.confit.internal.cached.proxy;

import com.google.common.collect.Sets;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.model.Schema.SchemaPropertyRef;
import org.deephacks.confit.model.Schema.SchemaPropertyRefList;
import org.deephacks.confit.model.Schema.SchemaPropertyRefMap;
import org.deephacks.confit.spi.Conversion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.deephacks.confit.internal.core.Reflections.findField;
import static org.deephacks.confit.internal.core.Reflections.newInstance;

/**
 * Responsible for generating proxy classes of real configurable classes and creating
 * proxy objects that are handed off to clients.
 *
 *
 * In order for proxies to work properly a configurable class must conform to the following
 * requirements:
 *
 * - Declared as non-final (proxies extend configurable classes)
 * - Fields that reference other configurable classes must be accessed using
 *   accessor/getter methods (i.e. public fields will not work).
 *
 */
public class ConfigProxyGenerator {
    /** classname suffix of each generated proxy class */
    public static final String PROXY_CLASS_SUFFIX = "__javassist_config_proxy";

    /** name of the field that store the ConfigReferenceHolder */
    public static final String PROXY_FIELD_NAME = "__reference_holder";

    /** schemaName -> Schema, schema awareness is needed to fetch references */
    private static final HashMap<String, Schema> schemas = new HashMap<>();

    /** a cache for already generated proxy classes */
    private static final ConcurrentHashMap<String, Class<?>> proxyClassCache = new ConcurrentHashMap<>();

    /** javassist class pool */
    private static final ClassPool pool = ClassPool.getDefault();

    /** convert bean properties in string form to real objects */
    private static final Conversion converter = Conversion.get();

    /**
     * The proxy generator must be aware the schema of configurable classes,
     * including their references, to ba able to create proxies from them.
     */
    public void put(Schema schema) {
        schemas.put(schema.getName(), schema);
    }

    public Object generateConfigProxy(Bean bean)  {
        try {
            Schema schema = bean.getSchema();

            Class<?> proxyClass = getProxyClass(schema);
            Object proxyObject  = newInstance(proxyClass);
            // set @Id property on proxy object
            setId(proxyObject, bean);
            // set regular @Config properties on the proxy object
            for(SchemaProperty property : schema.get(SchemaProperty.class)) {
                setProperty(proxyObject, bean, property);
            }
            // set collection-like @Config properties on the proxy object
            for(SchemaPropertyList property : schema.get(SchemaPropertyList.class)) {
                setProperty(proxyObject, bean, property);
            }
            // attach the reference holder on the proxy object
            // every reference is a String, the instance id.
            setReferenceHolder(proxyObject, bean);

            return proxyObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setId(Object proxyObject, Bean bean) throws Exception {
        String fieldName = bean.getSchema().getId().getName();
        Field f = findField(proxyObject.getClass(), fieldName);
        f.set(proxyObject, bean.getId().getInstanceId());
    }

    private void setReferenceHolder(Object proxyObject, Bean bean) throws Exception {
        ConfigReferenceHolder proxy = new ConfigReferenceHolder(bean);
        Field f = findField(proxyObject.getClass(), PROXY_FIELD_NAME);
        f.set(proxyObject, proxy);
    }

    private void setProperty(Object proxyObject, Bean bean, SchemaPropertyList property) throws Exception {
        String fieldName = property.getFieldName();
        List<String> values = bean.getValues(fieldName);
        if(values == null || values.size() == 0) {
            return;
        }
        setValues(proxyObject, property, values);
    }

    private void setValues(Object proxyObject, SchemaPropertyList schema, List<String> stringValues) throws Exception {
        String fieldName = schema.getFieldName();
        Field f = findField(proxyObject.getClass(), fieldName);
        f.setAccessible(true);
        Class<?> collectionType = schema.getClassCollectionType();
        if(Set.class.isAssignableFrom(collectionType)) {
            Set values = (Set) converter.convert(Sets.newHashSet(stringValues), schema.getClassType());
            f.set(proxyObject, values);
        } else if (List.class.isAssignableFrom(collectionType)) {
            List values = (List) converter.convert(stringValues, schema.getClassType());
            f.set(proxyObject, values);
        } else {
            throw new UnsupportedOperationException("Collection type is not supported " + collectionType);
        }
    }

    private void setProperty(Object proxyObject, Bean bean, SchemaProperty property) throws Exception {
        String fieldName = property.getFieldName();
        List<String> values = bean.getValues(fieldName);
        if(values == null || values.size() == 0) {
            return;
        }
        setValue(proxyObject, fieldName, property.getClassType(), values.get(0));
    }

    private void setValue(Object proxyObject, String fieldName, Class<?> type, String stringValue) throws Exception {
        Field f = findField(proxyObject.getClass(), fieldName);
        f.setAccessible(true);
        Object value = converter.convert(stringValue, type);
        f.set(proxyObject, value);
    }

    private synchronized Class<?> getProxyClass(Schema schema) throws Exception {
        Class<?> proxyClass = proxyClassCache.get(schema.getName());
        if(proxyClass != null) {
            return proxyClass;
        }
        CtClass proxy = createCtClassProxy(schema);
        CtClass referenceHolder = pool.get(ConfigReferenceHolder.class.getName());
        CtField f = new CtField(referenceHolder, PROXY_FIELD_NAME, proxy);
        f.setModifiers(Modifier.PUBLIC);
        proxy.addField(f);

        for (SchemaPropertyRef ref : schema.get(SchemaPropertyRef.class)) {
            instrument(proxy, ref);
        }
        for (SchemaPropertyRefList ref : schema.get(SchemaPropertyRefList.class)) {
            instrument(proxy, ref);
        }
        for (SchemaPropertyRefMap ref : schema.get(SchemaPropertyRefMap.class)) {
            instrument(proxy, ref);
        }
        return createProxyClass(schema, proxy);
    }

    /**
     * Instrument a single reference field, using the ConfigReferenceHolder to fetch the real
     * reference from the cache and replace it with a real object.
     */
    private void instrument(CtClass proxy, final SchemaPropertyRef ref) throws Exception {
        final Schema schema = schemas.get(ref.getSchemaName());
        checkNotNull(schema, "Schema not found for SchemaPropertyRef ["+ref+"]");
        final String fieldName = ref.getFieldName();
        // for help on javassist syntax, see chapter around javassist.expr.FieldAccess at
        // http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/tutorial/tutorial2.html#before
        proxy.instrument(new ExprEditor() {
            public void edit(FieldAccess f) throws CannotCompileException {
                if (f.getFieldName().equals(ref.getFieldName())) {
                    StringBuilder code = new StringBuilder();
                    code.append("{");
                    code.append("$_=("+schema.getType()+") this."+PROXY_FIELD_NAME+".getObjectReference(\""+fieldName+"\", \""+schema.getName()+"\");");
                    code.append("}");
                    f.replace(code.toString());
                }
            }
        });
    }

    /**
     * Instrument a Collection field holding references, using the ConfigReferenceHolder
     * to fetch the real reference from the cache and replace it with a real object.
     */
    private void instrument(CtClass proxy, final SchemaPropertyRefList ref) throws Exception {
        final Schema schema = schemas.get(ref.getSchemaName());
        checkNotNull(schema, "Schema not found for SchemaPropertyRefList ["+ref+"]");
        final String fieldName = ref.getFieldName();
        // for help on javassist syntax, see chapter around javassist.expr.FieldAccess at
        // http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/tutorial/tutorial2.html#before
        proxy.instrument(new ExprEditor() {
            public void edit(FieldAccess f) throws CannotCompileException {
                if (f.getFieldName().equals(ref.getFieldName())) {
                    StringBuilder code = new StringBuilder();
                    code.append("{");
                    code.append("$_=(java.util.List) this."+PROXY_FIELD_NAME+".getObjectReferenceList(\""+fieldName+"\", \""+schema.getName()+"\");");
                    code.append("}");
                    f.replace(code.toString());
                }
            }
        });
    }

    /**
     * Instrument a Map field holding references, using the ConfigReferenceHolder
     * to fetch the real reference from the cache and replace it with a real object.
     */
    private void instrument(CtClass proxy, final SchemaPropertyRefMap ref) throws Exception {
        final Schema schema = schemas.get(ref.getSchemaName());
        checkNotNull(schema, "Schema not found for SchemaPropertyRefMap ["+ref+"]");
        final String fieldName = ref.getFieldName();
        // for help on javassist syntax, see chapter around javassist.expr.FieldAccess at
        // http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/tutorial/tutorial2.html#before
        proxy.instrument(new ExprEditor() {
            public void edit(FieldAccess f) throws CannotCompileException {
                if (f.getFieldName().equals(ref.getFieldName())) {
                    StringBuilder code = new StringBuilder();
                    code.append("{");
                    code.append("$_=(java.util.Map) this."+PROXY_FIELD_NAME+".getObjectReferenceMap(\""+fieldName+"\", \""+schema.getName()+"\");");
                    code.append("}");
                    f.replace(code.toString());
                }
            }
        });
    }

    /**
     * Create a copy of the configurable class, a proxy class, and extend the
     * configurable class with this proxy class. The idea is to let the proxy class
     * override list methods of the configurable superclass [JLS 8.4.8.1].
     *
     * So when the real configurable class access a @Config field through a method
     * it will access the field of the proxy instead of the real object.
     */
    private CtClass createCtClassProxy(Schema schema) throws Exception {

        CtClass proxyClass = pool.getAndRename(schema.getType(), schema.getType() + PROXY_CLASS_SUFFIX);
        CtClass org = getCtClass(schema);
        proxyClass.setSuperclass(org);
        return proxyClass;
    }

    private CtClass getCtClass(Schema schema) {
        String type = schema.getType();
        try {
            return pool.get(type);
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Class<?> createProxyClass(Schema schema, CtClass proxy) throws IOException, CannotCompileException {
        Class<?> proxyClass;
        byte[] enhanced = proxy.toBytecode();
        ClassLoader cl = new ClassLoader() { };
        ClassPool cp = new ClassPool( false );
        cp.appendClassPath( new LoaderClassPath( cl ) );
        CtClass enhancedCtClass = cp.makeClass( new ByteArrayInputStream( enhanced ) );
        proxyClass = enhancedCtClass.toClass( cl, ConfigProxyGenerator.class.getProtectionDomain() );
        proxyClassCache.put(schema.getName(), proxyClass);
        return proxyClass;
    }

}
