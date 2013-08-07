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
package org.deephacks.confit.internal.jaxrs;

import com.google.common.base.Optional;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.admin.query.BeanQueryBuilder.BeanRestriction;
import org.deephacks.confit.admin.query.BeanQueryResult;
import org.deephacks.confit.internal.jaxrs.JaxrsBeans.JaxrsBean;
import org.deephacks.confit.internal.jaxrs.JaxrsObjects.JaxrsObject;
import org.deephacks.confit.internal.jaxrs.JaxrsSchemas.JaxrsSchema;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * JAX-RS endpoint for provisioning configuration in JSON format.
 */
@Path(JaxrsConfigEndpoint.PATH)
@Consumes({ APPLICATION_JSON })
@Produces({ APPLICATION_JSON })
public class JaxrsConfigEndpoint {

    /** address also used by angular.js web application */
    public static final String PATH = "/confit-admin";

    private AdminContext admin;

    private static final Map<String, Schema> schemas = new HashMap<>();

    @Inject
    public JaxrsConfigEndpoint(AdminContext admin) {
        this.admin = admin;
        for (Schema s : admin.getSchemas().values()) {
            schemas.put(s.getName(), s);
        }
    }

    @GET
    @Produces({ APPLICATION_JSON })
    @Path("getBean/{schemaName}/{id}")
    public JaxrsBean getBean(@PathParam("schemaName") String schemaName,
                        @PathParam("id") String id) throws AbortRuntimeException {
        Optional<Bean> optional = admin.get(BeanId.create(id, schemaName));
        if (optional.isPresent()) {
            return new JaxrsBean(optional.get());
        }
        throw new WebApplicationException(Status.NOT_FOUND);
    }
    @GET
    @Produces({ APPLICATION_JSON })
    @Path("getSingleton/{className}")
    public JaxrsObject getSingleton(@PathParam("className") String className) throws AbortRuntimeException {
        try {
            Optional<?> optional = admin.get(Class.forName(className));
            if (optional.isPresent()) {
                return new JaxrsObject(optional.get());
            }
            throw new WebApplicationException(Status.NOT_FOUND);
        } catch (ClassNotFoundException e) {
            throw Events.CFG101_SCHEMA_NOT_EXIST(className);
        }
    }

    @GET
    @Produces({ APPLICATION_JSON })
    @Path("getObject/{className}/{id}")
    public JaxrsObject getObject(@PathParam("className") String className,
                            @PathParam("id") String id) throws AbortRuntimeException {
        try {
            Optional<?> optional = admin.get(Class.forName(className), id);
            if (optional.isPresent()) {
                return new JaxrsObject(optional.get());
            }
            throw new WebApplicationException(Status.NOT_FOUND);
        } catch (ClassNotFoundException e) {
            throw Events.CFG101_SCHEMA_NOT_EXIST(className);
        }
    }

    @GET
    @Produces({ APPLICATION_JSON })
    @Path("listBean/{schemaName}")
    public JaxrsBeans listBean(@PathParam("schemaName") String schemaName) throws AbortRuntimeException {
        List<Bean> beans = admin.list(schemaName);
        return new JaxrsBeans(beans);
    }

    @GET
    @Produces({ APPLICATION_JSON })
    @Path("listObjects/{className}")
    public JaxrsObjects listObjects(@PathParam("className") String className) throws AbortRuntimeException {
        try {
            Collection<?> objects = admin.list(Class.forName(className));
            return new JaxrsObjects(objects);
        } catch (ClassNotFoundException e) {
            throw Events.CFG101_SCHEMA_NOT_EXIST(className);
        }

    }

    @GET
    @Produces({ APPLICATION_JSON })
    @Path("listBeans/{schemaName}")
    public JaxrsBeans listBeans(@PathParam("schemaName") String schemaName,
                                @QueryParam("id") final List<String> ids) throws AbortRuntimeException {
        Collection<Bean> beans = admin.list(schemaName, ids);
        return new JaxrsBeans(beans);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("createBean")
    public void createBean(JaxrsBean jaxrsBean) throws AbortRuntimeException {
        Bean bean = toBean(jaxrsBean);
        admin.create(bean);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("createObject")
    public void createObject(JaxrsObject jaxrsObject) throws AbortRuntimeException {
        Object object = jaxrsObject.toObject();
        admin.createObject(object);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("createBeans")
    public void createBeans(JaxrsBeans jaxrsBeans) throws AbortRuntimeException {
        Collection<Bean> beans = toBeans(jaxrsBeans);
        admin.create(beans);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("createObjects")
    public void createObjects(JaxrsObjects jaxrsObjects) throws AbortRuntimeException {
        Collection<?> objects = jaxrsObjects.toObjects();
        admin.createObjects(objects);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("setBean")
    public void setBean(JaxrsBean jaxrsBean) throws AbortRuntimeException {
        Bean bean = toBean(jaxrsBean);
        admin.set(bean);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("setObject")
    public void setObject(JaxrsObject jaxrsObject) throws AbortRuntimeException {
        Object object = jaxrsObject.toObject();
        admin.setObject(object);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("setBeans")
    public void setBeans(JaxrsBeans jaxrsBeans) throws AbortRuntimeException {
        Collection<Bean> beans = toBeans(jaxrsBeans);
        admin.set(beans);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("setObjects")
    public void setObjects(JaxrsObjects jaxrsObjects) throws AbortRuntimeException {
        Collection<Object> objects = jaxrsObjects.toObjects();
        admin.setObjects(objects);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("mergeBean")
    public void mergeBean(JaxrsBean jaxrsBean) throws AbortRuntimeException {
        Bean bean = toBean(jaxrsBean);
        admin.merge(bean);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("mergeObject")
    public void mergeObject(JaxrsObject jaxrsObject) throws AbortRuntimeException {
        Object object = jaxrsObject.toObject();
        admin.mergeObject(object);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("mergeBeans")
    public void mergeBeans(JaxrsBeans jaxrsBeans) throws AbortRuntimeException {
        Collection<Bean> beans = toBeans(jaxrsBeans);
        admin.merge(beans);
    }

    @POST
    @Consumes({ APPLICATION_JSON })
    @Path("mergeObjects")
    public void mergeObjects(JaxrsObjects jaxrsObjects) throws AbortRuntimeException {
        Collection<Object> objects = jaxrsObjects.toObjects();
        admin.mergeObjects(objects);
    }

    @DELETE
    @Consumes({ "*/*" })
    @Path("deleteBean/{schemaName}/{id}")
    public void deleteBean(@PathParam("schemaName") String schemaName,
                           @PathParam("id") String id) throws AbortRuntimeException {
        admin.delete(BeanId.create(id, schemaName));
    }

    @DELETE
    @Consumes({ "*/*" })
    @Path("deleteBeans/{schemaName}")
    public void deleteBeans(@PathParam("schemaName") String schemaName,
                            @QueryParam("id") final List<String> ids) throws AbortRuntimeException {
        admin.delete(schemaName, ids);
    }

    @GET
    @Produces({ APPLICATION_JSON })
    @Path("getSchemas")
    public JaxrsSchemas getSchemas() {
        JaxrsSchemas jaxrsSchemas = new JaxrsSchemas();
        for (Schema schema : admin.getSchemas().values()) {
            jaxrsSchemas.add(new JaxrsSchema(schema));
        }
        return jaxrsSchemas;
    }

    @GET
    @Produces({ APPLICATION_JSON })
    @Path("getSchema/{schemaName}")
    public JaxrsSchema getSchema(@PathParam("schemaName") String schemaName) {
        Optional<Schema> schema = admin.getSchema(schemaName);
        if (!schema.isPresent()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return new JaxrsSchema(schema.get());
    }

    @GET
    @Produces({ APPLICATION_JSON })
    @Path("query/{schemaName}")
    public JaxrsBeans query(@PathParam("schemaName") String schemaName, @QueryParam("q") String query,
                            @QueryParam("first") String first, @QueryParam("max") int max) {
        max = max == 0 ? Integer.MAX_VALUE : max;
        JaxrsQuery jaxrsQuery = new JaxrsQuery(query);
        BeanQuery beanQuery = admin.newQuery(schemaName)
                .setFirstResult(first)
                .setMaxResults(max);
        for (BeanRestriction restriction : jaxrsQuery.getRestrictions()) {
            beanQuery.add(restriction);
        }
        BeanQueryResult result = beanQuery.retrieve();
        JaxrsBeans jaxrsBeans = new JaxrsBeans(result.get());
        return jaxrsBeans;
    }

    private Bean toBean(JaxrsBean jaxrsBean) {
        Schema schema = schemas.get(jaxrsBean.getSchemaName());
        if (schema == null) {
            throw Events.CFG101_SCHEMA_NOT_EXIST(jaxrsBean.getSchemaName());
        }
        return jaxrsBean.toBean(schema);
    }

    private Collection<Bean> toBeans(JaxrsBeans jaxrsBeans) {
        ArrayList<Bean> beans = new ArrayList<>();
        for (JaxrsBean jaxrsBean : jaxrsBeans.getBeans()) {
            Schema schema = schemas.get(jaxrsBean.getSchemaName());
            if (schema == null) {
                throw Events.CFG101_SCHEMA_NOT_EXIST(jaxrsBean.getSchemaName());
            }
            beans.add(jaxrsBean.toBean(schema));
        }
        return beans;
    }
}
