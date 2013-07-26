package org.deephacks.confit.internal.jaxrs;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.module.SimpleModule;
import org.deephacks.confit.internal.core.Lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.deephacks.confit.internal.core.Reflections.getParameterizedType;

public class JaxrsObjects {
    private Collection<JaxrsObject> objects = new ArrayList<>();
    private long totalCount;

    public JaxrsObjects() {

    }

    public JaxrsObjects(Collection<?> objects) {
        addAll(objects);
    }

    public void add(JaxrsObject object) {
        this.objects.add(object);
    }

    public void addAll(Collection<?> objects) {
        for (Object object : objects) {
            add(new JaxrsObject(object));
        }
    }

    public void setTotalCount(long total) {
        this.totalCount = total;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public Collection<JaxrsObject> getObjects() {
        return objects;
    }

    public Collection<Object> toObjects() {
        ArrayList<Object> objects = new ArrayList<>();
        for (JaxrsObject jaxrsObject : this.objects) {
            objects.add(jaxrsObject.toObject());
        }
        return objects;
    }

    public static class JaxrsObject {
        private static final ObjectMapper mapper = new ObjectMapper();
        static {
            mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance()
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
            // register jackson serializers/deserilzers for custom simple data types
            // using standard java service provider lookup.
            SimpleModule module = new SimpleModule("confit", new Version(1, 0, 0, null));
            Collection<JsonDeserializer> deserializers = Lookup.get().lookupAll(JsonDeserializer.class);
            for (JsonDeserializer deserializer : deserializers) {
                List<Class<?>> cls = getParameterizedType(deserializer.getClass(), deserializer.getClass());
                module.addDeserializer(cls.get(0), deserializer);
            }
            Collection<JsonSerializer> serializers = Lookup.get().lookupAll(JsonSerializer.class);
            for (JsonSerializer serializer : serializers) {
                List<Class<?>> cls = getParameterizedType(serializer.getClass(), serializer.getClass());
                module.addSerializer(cls.get(0), serializer);
            }

            mapper.registerModule(module);
        }
        private String className;
        private Object object;

        public JaxrsObject() {

        }

        public JaxrsObject(String className, Object object) {
            this.className = className;
            this.object = object;

        }

        public JaxrsObject(Object o) {
            this.className = o.getClass().getName();
            this.object = o;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }

        public Object getObject(){
            return object;
        }

        public Object toObject() {
            try {
                Class<?> clazz = Class.forName(className);
                Object value = mapper.convertValue(object, clazz);
                if (value == null) {
                    return clazz.newInstance();
                }
                return value;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
