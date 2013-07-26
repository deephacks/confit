package org.deephacks.confit.test;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class JUnitUtils {
    /**
     * Compute the root directory of this maven project. This will result in the
     * same directory no matter if executed from Eclipse, this maven project root or
     * any parent maven pom directory.
     *
     * @param anyTestClass Any test class *local* to the maven project, i.e that
     * only exist in this maven project.
     *
     * @param anyTestClass The file that should be
     * @return The root directory of this maven project.
     */
    public static File computeMavenProjectRoot(Class<?> anyTestClass) {
        final String clsUri = anyTestClass.getName().replace('.', '/') + ".class";
        final URL url = anyTestClass.getClassLoader().getResource(clsUri);
        final String clsPath = url.getPath();
        // located in ./target/test-classes or ./eclipse-out/target
        final File target_test_classes = new File(clsPath.substring(0,
                clsPath.length() - clsUri.length()));
        // get parent's parent
        return target_test_classes.getParentFile().getParentFile();
    }

    /**
     * Normalizes the root for reading a file to the maven project root directory.
     *
     * @param anyTestClass Any test class *local* to the maven project, i.e that
     * only exist in this maven project.
     *
     * @param child A child path.
     *
     * @return A file relative to the maven root.
     */
    public static File getMavenProjectChildFile(Class<?> anyTestClass, String child) {
        return new File(computeMavenProjectRoot(anyTestClass), child);
    }

    public static File getMetaInfDir(Class<?> context) {
        URL url = context.getResource("/META-INF/");
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> readMetaInfResource(Class<?> context, String filepath) {
        InputStream in = context.getResourceAsStream("/META-INF/" + filepath);
        ArrayList<String> list = new ArrayList<String>();
        try {
            String content = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));

            list.addAll(Arrays.asList(content.split("\n")));
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Mini-languge for generating classes and bean instances.
     *
     * Ex: A=1000, B=200$10, C=3$300; B=2000, C=200$10; C=300
     *
     *   - creates 1000 A, 2000 B and 300 C instances.
     *   - Assign 200 random A instances to 10 random B references
     *   - Assign 3 random A instances to 300 random C references
     *   - Assign 200 random B instances to 10 random C references
     */
    public static Set<ConfigClass> generate(String minilang, int numprops, int numvalues) {

        String[] cfgtypes = minilang.split(";");
        HashMap<String, ConfigClass> configs = new HashMap<>();
        // declared typesafe types
        for (String cfgtype : cfgtypes) {
            String[] stmts = cfgtype.split(",");
            String stmt = stmts[0].trim();
            String[] assign = stmt.split("=");
            ConfigClass clazz = new ConfigClass(assign[0].trim(), ConfigClass.randomFieldName());
            clazz.addBeans(Integer.parseInt(assign[1].trim()));
            String[] refstmts = new String[cfgtype.split(",").length - 1];
            System.arraycopy(stmts, 1, refstmts, 0, refstmts.length);
            clazz.stmts = refstmts;
            configs.put(assign[0].trim(), clazz);

        }
        // reference assignment for each typesafe type
        for (String parentname : configs.keySet()) {
            ConfigClass parent = configs.get(parentname);
            for (int i = 0; i < numprops; i++) {
                parent.addField(numvalues);
            }
            for (String stmt : parent.stmts) {
                String childname = stmt.split("=")[0].trim();
                String refs = stmt.split("=")[1].trim();
                ConfigClass child = configs.get(childname);
                int parentnum = Integer.parseInt(refs.split("\\$")[0].trim());
                int childnum = Integer.parseInt(refs.split("\\$")[1].trim());
                parent.addReferences(parentnum, childnum, child);
            }
        }
        return new HashSet<>(configs.values());
    }

    public static List<Bean> generateBeans(int numBeans, int numProps) {
        ArrayList<Bean> beans = new ArrayList<>();
        for (int i = 0; i < numBeans; i++) {
            String id = "beanId" + i;
            String type = "beanType" + i;
            Bean bean = Bean.create(BeanId.create(id, type));
            for (int j = 0; j < numProps; j++) {
                String _name = "propName" + j;
                String _value = "propFieldName" + j;
                bean.addProperty(_name, _value);
                List<String> d = Arrays.asList("1", "2", "3");
                bean.addProperty(_name, d);
            }
            beans.add(bean);
        }
        return beans;
    }

    /**
     * Compile a generated typesafe class to given directory.
     *
     * Classes will be loaded into current thread classloader.
     */
    public static Set<Class<?>> compile(ConfigClass config, File dir) {
        return compile(Arrays.asList(config), dir);
    }

    /**
     * Compile generated typesafe classes to given directory.
     *
     * Classes will be loaded into current thread classloader.
     */
    public static Set<Class<?>> compile(Collection<ConfigClass> configs, File dir) {
        HashMap<String, String> sources = new HashMap<>();
        for (ConfigClass config : configs) {
            sources.put(config.classname, config.toString());
        }
        return CompilerUtils.compile(sources, ConfigClass.class.getPackage().getName(), dir);
    }

    /**
     * A dynamically generated typesafe class.
     */
    public static class ConfigClass {
        private final static Class<?>[] types = new Class[] { Long.class, String.class };

        static final String CLASSNAME = "%classname%";

        static final String ID = "%idfield%";
        static final String ID_NAME = "%idname%";
        static final String ID_TYPE = "%idtype%";
        static final String ID_TEMPLATE = " @Id(desc=\"" + ID_NAME + "\") private " + ID_TYPE + " "
                + ID_NAME + ";";

        static final String FIELDS = "%fields%";
        static final String FIELD_NAME = "%fieldname%";
        static final String FIELD_TYPE = "%fieldtype%";
        static final String FIELD_TEMPLATE = " @Config(desc=\"" + FIELD_NAME + "\") private "
                + FIELD_TYPE + " " + FIELD_NAME + ";";

        static final String REFERENCES = "%references%";
        static final String REFERENCE_NAME = "%referencename%";
        static final String REFERENCE_TYPE = "%referencetype%";
        static final String REFERENCE_TEMPLATE = " @Config(desc=\"" + REFERENCE_NAME
                + "\") private " + REFERENCE_TYPE + " " + REFERENCE_NAME + ";";

        static final String CLASS_TEMPLATE = "package " + ConfigClass.class.getPackage().getName()
                + ";\n" + "import org.deephacks.tools4j.typesafe.*;\nimport java.util.*;\n"
                + "@Config(name=\"" + CLASSNAME + "\", desc=\"" + CLASSNAME + "\")\n"
                + "public class %classname% {\n" + ID + "\n" + FIELDS + "\n" + REFERENCES + "\n}";

        private Map<String, String> fields = new HashMap<>();
        private Map<String, String> references = new HashMap<>();

        public String classname;
        public String idName;
        public String idType;
        public ArrayList<Bean> beans = new ArrayList<>();
        String[] stmts = new String[0];

        public ConfigClass(String classname, String idName) {
            this.classname = classname;
            this.idName = idName;
            this.idType = "String";
        }

        public static Set<ConfigClass> generate(int numTypes, int minProps, int maxProps) {
            numTypes = numTypes == 0 ? 1 : numTypes;
            Set<ConfigClass> classes = new HashSet<>();
            for (int i = 0; i < numTypes; i++) {
                int numProps = randomInt(minProps, maxProps);
                ConfigClass config = new ConfigClass(randomClassname(), randomFieldName());
                for (int j = 0; j < numProps; j++) {
                    config.addField(10);
                }
                classes.add(config);
            }
            return classes;
        }

        public void addBeans(int num) {
            for (int i = 0; i < num; i++) {
                Bean b = Bean.create(BeanId.create(randomAlphanumeric(20), classname));
                beans.add(b);
            }
        }

        public List<Bean> getBeans() {
            return beans;
        }

        public void addField(int numvalues) {
            Class<?> clazz = types[new Random().nextInt(types.length)];
            boolean isList = new Random().nextBoolean();
            String name = randomFieldName();
            if (!isList) {
                fields.put(name, clazz.getName());
                for (Bean bean : beans) {
                    bean.addProperty(name, generateRandomValue(clazz));
                }
            } else {
                fields.put(name, "List<" + clazz.getName() + ">");
                for (Bean bean : beans) {
                    for (int i = 0; i < numvalues; i++) {
                        bean.addProperty(name, generateRandomValue(clazz));
                    }
                }
            }
        }

        public void addReferenceRandom(String type) {
            boolean isList = new Random().nextBoolean();
            if (!isList) {
                addReference(type);
            } else {
                addReferenceList(randomFieldName(), type);
            }
        }

        public void addReferences(int parentnum, int childnum, ConfigClass child) {
            addReferenceList(child.classname, child.classname);
            Bean[] b = beans.toArray(new Bean[0]);
            for (int i = 0; i < parentnum; i++) {
                List<Integer> numbers = randomUniqueInt(0, beans.size() - 1);
                while (parentnum-- > 0) {
                    if (parentnum > b.length) {
                        throw new IllegalArgumentException(
                                "MINILANG: too few instances available [" + b.length
                                        + "] to reference [" + parentnum + "] instances.");
                    }
                    Bean parent = b[numbers.get(parentnum)];
                    Set<Bean> children = child.getInstances(childnum);
                    for (Bean bean : children) {
                        // circular references to self is not allowed.
                        if (!bean.equals(parent)) {
                            parent.addReference(bean.getId().getSchemaName(), bean.getId());
                        }
                    }
                }
            }
        }

        private Set<Bean> getInstances(int num) {
            Bean[] b = beans.toArray(new Bean[0]);
            Set<Bean> result = new HashSet<>();
            List<Integer> numbers = randomUniqueInt(0, num - 1);
            while (num-- > 0) {
                result.add(b[numbers.get(num)]);
            }
            return result;
        }

        public void addReference(String type) {
            references.put(randomFieldName(), type);
        }

        public void addReferenceList(String name, String type) {
            references.put(name, "List<" + type + ">");
        }

        public Collection<String> getFields() {
            return fields.keySet();
        }

        public Collection<String> getReferences() {
            return references.keySet();
        }

        /**
         * Class names are assumed to have between 8 - 40 alphabetic characters.
         */
        private static String randomClassname() {
            return randomAlphabeth(8, 20);
        }

        /**
         * Field names are assumed to have between 5 - 15 alphabetic characters.
         */
        private static String randomFieldName() {
            return randomAlphabeth(5, 15);
        }

        /**
         * generate a random number between min and max
         */
        private static int randomInt(int min, int max) {
            return min + (new Random()).nextInt(max - min);
        }

        private static List<Integer> randomUniqueInt(int min, int max) {
            List<Integer> numbers = new ArrayList<>();
            for (int i = min; i <= max; i++) {
                numbers.add(i);
            }
            Collections.shuffle(numbers);
            return numbers;
        }

        /**
         * generate a random number of alphabetic characters between min and max
         */
        private static String randomAlphabeth(int min, int max) {
            return randomAlphabetic(randomInt(min, max));
        }

        /**
         * generate a random number of alphanumeric characters between
         * min and max
         */
        private static String randomAlphanum(int min, int max) {
            return randomAlphanumeric(randomInt(min, max));
        }

        private final static List<String> longValueCache = new ArrayList<>();
        private static int numLongs = 0;
        private final static List<String> doubleValueCache = new ArrayList<>();
        private static int numDoubles = 0;
        private final static List<String> stringValueCache = new ArrayList<>();
        private static int numStrings = 0;

        /**
         * This method will generate at most 1 million random values for each type
         * and then serve the same values again to save memory and time.
         */
        private static final String generateRandomValue(Class<?> clazz) {
            String value = null;
            if (clazz.isAssignableFrom(Long.class)) {
                if (numLongs > 1000000) {
                    return longValueCache.get(numLongs++ % 1000000);
                }
                numLongs++;
                value = "" + new Random().nextLong();
                longValueCache.add(value);
            } else if (clazz.isAssignableFrom(Double.class)) {
                if (numDoubles > 1000000) {
                    return doubleValueCache.get(numDoubles++ % 1000000);
                }
                numDoubles++;
                value = "" + new Random().nextDouble();
                doubleValueCache.add(value);
            } else if (clazz.isAssignableFrom(String.class)) {
                if (numStrings > 1000000) {
                    return stringValueCache.get(numStrings++ % 1000000);
                }
                numStrings++;
                value = randomAlphanum(10, 40);
                stringValueCache.add(value);
            }
            return value;

        }

        /**
         * Add references between configurables with given probability.
         * 2 = 50%, 3 = 33%, 4 = 25 % and so on.
         */
        @SuppressWarnings("unused")
        private static void addReferences(Set<ConfigClass> all, int probability) {
            for (ConfigClass target : all) {
                for (ConfigClass source : all) {
                    if (new Random().nextInt(probability) == 0) {
                        target.addReference(source.classname);
                    }
                }
            }
        }

        @Override
        public String toString() {
            String clazz = CLASS_TEMPLATE.replaceAll(CLASSNAME, classname);

            String fieldStr = replace(fields, FIELD_TEMPLATE, FIELD_TYPE, FIELD_NAME);
            String referenceStr = replace(references, REFERENCE_TEMPLATE, REFERENCE_TYPE,
                    REFERENCE_NAME);

            String id = ID_TEMPLATE.replaceAll(ID_NAME, idName);
            id = id.replaceAll(ID_TYPE, idType);

            clazz = clazz.replaceAll(ID, id);
            clazz = clazz.replaceAll(FIELDS, fieldStr);
            clazz = clazz.replaceAll(REFERENCES, referenceStr);

            return clazz;
        }

        private String replace(Map<String, String> content, String template, String type,
                               String name) {
            StringBuffer referenceStrs = new StringBuffer();
            for (String key : content.keySet()) {
                String reference = template.replaceAll(name, key);
                reference = reference.replaceAll(type, content.get(key));
                referenceStrs.append(reference).append("\n");
            }
            return referenceStrs.toString();
        }
    }

}
