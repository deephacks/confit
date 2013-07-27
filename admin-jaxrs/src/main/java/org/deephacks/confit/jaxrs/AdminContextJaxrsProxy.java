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
package org.deephacks.confit.jaxrs;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.admin.query.BeanQuery;
import org.deephacks.confit.admin.query.BeanQueryBuilder.BeanRestriction;
import org.deephacks.confit.internal.jaxrs.JaxrsBeans;
import org.deephacks.confit.internal.jaxrs.JaxrsBeans.JaxrsBean;
import org.deephacks.confit.internal.jaxrs.JaxrsConfigEndpoint;
import org.deephacks.confit.internal.jaxrs.JaxrsObjects;
import org.deephacks.confit.internal.jaxrs.JaxrsObjects.JaxrsObject;
import org.deephacks.confit.internal.jaxrs.JaxrsQuery;
import org.deephacks.confit.internal.jaxrs.JaxrsSchemas;
import org.deephacks.confit.internal.jaxrs.JaxrsSchemas.JaxrsSchema;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

/**
 * JAX-RS version of AdminContext that allow accessing configuration in a type-safe way
 * without explicitly using HTTP or JAX-RS APIs.
 */
public class AdminContextJaxrsProxy extends AdminContext {
    private final Client client;
    private final String host;
    private final int port;
    private final String prefixUri;

    private final AdminContext admin = AdminContext.get();

    private AdminContextJaxrsProxy(String host, int port, String prefixUri) {
        client = ClientBuilder.newBuilder().build();
        this.host = host;
        this.port = port;
        this.prefixUri = prefixUri;
    }

    public static AdminContextJaxrsProxy get(String host, int port) {
        return new AdminContextJaxrsProxy(host, port, "");
    }

    public static AdminContextJaxrsProxy get(String host, int port, String uriPrefix) {
        return new AdminContextJaxrsProxy(host, port, uriPrefix);
    }

    @Override
    public Optional<Bean> get(BeanId beanId) throws AbortRuntimeException {
        UriBuilder builder = getUri("getBean");
        URI uri = builder.build(beanId.getSchemaName(), beanId.getInstanceId());
        Response response = get(uri);
        try {
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                return Optional.absent();
            }
            handleReadResponse(response);
            JaxrsBean jaxrsBean = response.readEntity(JaxrsBean.class);
            Optional<Schema> schema = admin.getSchema(beanId.getSchemaName());
            if (!schema.isPresent()) {
                throw Events.CFG101_SCHEMA_NOT_EXIST(beanId.getSchemaName());
            }
            return Optional.of(jaxrsBean.toBean(schema.get()));
        } finally {
            response.close();
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> configurable) throws AbortRuntimeException {
        UriBuilder builder = getUri("getSingleton");
        URI uri = builder.build(configurable.getName());
        Response response = get(uri);
        try {
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                return Optional.absent();
            }
            handleReadResponse(response);
            JaxrsObject jaxrsObject = response.readEntity(JaxrsObject.class);
            return Optional.of((T) jaxrsObject.toObject());
        } finally {
            response.close();
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> configurable, String instanceId) throws AbortRuntimeException {
        UriBuilder builder = getUri("getObject");
        URI uri = builder.build(configurable.getName(), instanceId);
        Response response = get(uri);
        try {
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                return Optional.absent();
            }
            handleReadResponse(response);
            JaxrsObject jaxrsObject = response.readEntity(JaxrsObject.class);
            return Optional.of((T) jaxrsObject.toObject());
        } finally {
            response.close();
        }
    }

    @Override
    public List<Bean> list(String schemaName) throws AbortRuntimeException {
        UriBuilder builder = getUri("listBean");
        URI uri = builder.build(schemaName);
        Response response = get(uri);
        try {
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw Events.CFG101_SCHEMA_NOT_EXIST(schemaName);
            }
            handleReadResponse(response);
            JaxrsBeans jaxrsBeans = response.readEntity(JaxrsBeans.class);
            Map<String, Schema> schemas = admin.getSchemas();
            return jaxrsBeans.toBeans(schemas);
        } finally {
            response.close();
        }
    }

    @Override
    public <T> Collection<T> list(Class<T> configurable) throws AbortRuntimeException {
        UriBuilder builder = getUri("listObjects");
        URI uri = builder.build(configurable.getName());
        Response response = get(uri);
        try {
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw Events.CFG101_SCHEMA_NOT_EXIST(configurable.getName());
            }
            handleReadResponse(response);
            JaxrsObjects objects = response.readEntity(JaxrsObjects.class);
            return (Collection<T>) objects.toObjects();
        } finally {
            response.close();
        }
    }

    @Override
    public List<Bean> list(String schemaName, Collection<String> instanceIds) throws AbortRuntimeException {
        UriBuilder builder = getUri("listBeans");
        URI uri = builder.queryParam("id", instanceIds.toArray(new String[instanceIds.size()])).build(schemaName);
        Response response = get(uri);
        try {
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw Events.CFG90_SCHEMA_OR_ID_NOT_EXIST();
            }
            handleReadResponse(response);
            JaxrsBeans beans = response.readEntity(JaxrsBeans.class);
            Map<String, Schema> schemas = admin.getSchemas();
            return beans.toBeans(schemas);
        } finally {
            response.close();
        }
    }

    @Override
    public void create(Bean bean) throws AbortRuntimeException {
        URI uri = getUri("createBean").build();
        Entity entity = Entity.entity(new JaxrsBean(bean), APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void createObject(Object object) throws AbortRuntimeException {
        URI uri = getUri("createObject").build();
        Entity entity = Entity.entity(new JaxrsObject(object), APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void create(Collection<Bean> beans) throws AbortRuntimeException {
        URI uri = getUri("createBeans").build();
        JaxrsBeans jaxrsBeans = new JaxrsBeans();
        jaxrsBeans.addAll(beans);
        Entity entity = Entity.entity(jaxrsBeans, APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void createObjects(Collection<?> objects) throws AbortRuntimeException {
        URI uri = getUri("createObjects").build();
        JaxrsObjects jaxrsObjects = new JaxrsObjects();
        jaxrsObjects.addAll(objects);
        Entity entity = Entity.entity(jaxrsObjects, APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void set(Bean bean) throws AbortRuntimeException {
        URI uri = getUri("setBean").build();
        Entity entity = Entity.entity(new JaxrsBean(bean), APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void setObject(Object object) throws AbortRuntimeException {
        URI uri = getUri("setObject").build();
        Entity entity = Entity.entity(new JaxrsObject(object), APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void set(Collection<Bean> beans) throws AbortRuntimeException {
        URI uri = getUri("setBeans").build();
        JaxrsBeans jaxrsBeans = new JaxrsBeans();
        jaxrsBeans.addAll(beans);
        Entity entity = Entity.entity(jaxrsBeans, APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void setObjects(Collection<?> objects) throws AbortRuntimeException {
        URI uri = getUri("setObjects").build();
        JaxrsObjects jaxrsObjects = new JaxrsObjects();
        jaxrsObjects.addAll(objects);
        Entity entity = Entity.entity(jaxrsObjects, APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void merge(Bean bean) throws AbortRuntimeException {
        URI uri = getUri("mergeBean").build();
        Entity entity = Entity.entity(new JaxrsBean(bean), APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void mergeObject(Object object) throws AbortRuntimeException {
        URI uri = getUri("mergeObject").build();
        Entity entity = Entity.entity(new JaxrsObject(object), APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void merge(Collection<Bean> beans) throws AbortRuntimeException {
        URI uri = getUri("mergeBeans").build();
        JaxrsBeans jaxrsBeans = new JaxrsBeans();
        jaxrsBeans.addAll(beans);
        Entity entity = Entity.entity(jaxrsBeans, APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void mergeObjects(Collection<?> objects) throws AbortRuntimeException {
        URI uri = getUri("mergeObjects").build();
        JaxrsObjects jaxrsObjects = new JaxrsObjects();
        jaxrsObjects.addAll(objects);
        Entity entity = Entity.entity(jaxrsObjects, APPLICATION_JSON_TYPE);
        Response response = post(uri, entity);
        handleResponse(response);
    }

    @Override
    public void delete(BeanId bean) throws AbortRuntimeException {
        UriBuilder builder = getUri("deleteBean");
        URI uri = builder.build(bean.getSchemaName(), bean.getInstanceId());
        Response response = delete(uri);
        handleResponse(response);
    }

    @Override
    public void delete(String schemaName, Collection<String> instanceIds) throws AbortRuntimeException {
        UriBuilder builder = getUri("deleteBeans");
        URI uri = builder.queryParam("id", instanceIds.toArray(new String[instanceIds.size()])).build(schemaName);
        Response response = delete(uri);
        handleResponse(response);
    }

    @Override
    public Map<String, Schema> getSchemas() {
        URI uri = getUri("getSchemas").build();
        Response response = get(uri);
        try {
            handleReadResponse(response);
            JaxrsSchemas schemas = response.readEntity(JaxrsSchemas.class);
            return schemas.toSchema();
        } finally {
            response.close();
        }
    }

    @Override
    public Optional<Schema> getSchema(String schemaName) {
        URI uri = getUri("getSchema").build(schemaName);
        Response response = get(uri);
        try {
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                return Optional.absent();
            }
            handleReadResponse(response);
            JaxrsSchema jaxrsSchema = response.readEntity(JaxrsSchema.class);
            return Optional.of(jaxrsSchema.toSchema());
        } finally {
            response.close();
        }
    }

    @Override
    public BeanQuery newQuery(final String schemaName) {

        return new BeanQuery() {
            private int first = 0;
            private int max = 0;
            @Override
            public BeanQuery add(BeanRestriction restriction) {
                return null;
            }

            @Override
            public BeanQuery setFirstResult(int firstResult) {
                first = firstResult;
                return this;
            }

            @Override
            public BeanQuery setMaxResults(int maxResults) {
                max = maxResults;
                return this;
            }

            @Override
            public List<Bean> retrieve() {
                URI uri = getUri("query")
                        .queryParam("q", "df=120&asdfd=1212")
                        .queryParam("first", first)
                        .queryParam("max", max)
                        .build(schemaName);
                JaxrsQuery query = new JaxrsQuery();
                Response response = get(uri);
                return new ArrayList<>();
            }
        };
    }

    private UriBuilder getUri(String method) {
        if (Strings.isNullOrEmpty(prefixUri)) {
            return UriBuilder.fromResource(JaxrsConfigEndpoint.class)
                    .scheme("http")
                    .path(JaxrsConfigEndpoint.class, method)
                    .host(host)
                    .port(port);
        } else {
            return UriBuilder.fromPath(prefixUri).path(JaxrsConfigEndpoint.class)
                    .scheme("http")
                    .path(JaxrsConfigEndpoint.class, method)
                    .host(host)
                    .port(port);
        }
    }

    private Response get(URI uri) {
        return client.target(uri).request().buildGet().invoke();
    }

    private Response post(URI uri, Entity<?> entity) {
        return client.target(uri).request().buildPost(entity).invoke();
    }

    private Response delete(URI uri) {
        return client.target(uri).request().buildDelete().invoke();
    }

    private void handleResponse(Response response) {
        try {
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw Events.CFG90_SCHEMA_OR_ID_NOT_EXIST();
            } else if (response.getStatus() == Status.CONFLICT.getStatusCode()) {
                throw Events.CFG303_BEAN_ALREADY_EXIST();
            } else if (response.getStatus() == Status.FORBIDDEN.getStatusCode()) {
                throw Events.CFG089_MODIFICATION_CONFLICT();
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw Events.CFG088_INVALID_DATA();
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw Events.CFG088_INVALID_DATA();
            }
        } finally {
            response.close();
        }
    }
    private void handleReadResponse(Response response) {
        if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            response.close();
            throw Events.CFG90_SCHEMA_OR_ID_NOT_EXIST();
        } else if (response.getStatus() == Status.CONFLICT.getStatusCode()) {
            response.close();
            throw Events.CFG303_BEAN_ALREADY_EXIST();
        } else if (response.getStatus() == Status.FORBIDDEN.getStatusCode()) {
            response.close();
            throw Events.CFG089_MODIFICATION_CONFLICT();
        } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            response.close();
            throw Events.CFG088_INVALID_DATA();
        } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            response.close();
            throw Events.CFG088_INVALID_DATA();
        }
    }
}
