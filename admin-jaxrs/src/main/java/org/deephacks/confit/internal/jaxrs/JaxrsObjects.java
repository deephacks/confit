package org.deephacks.confit.internal.jaxrs;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.VisibilityChecker;

import java.util.ArrayList;
import java.util.Collection;

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
