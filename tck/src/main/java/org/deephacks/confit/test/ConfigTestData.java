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
package org.deephacks.confit.test;

import org.deephacks.confit.Config;
import org.deephacks.confit.Id;
import org.deephacks.confit.Index;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.test.validation.FirstUpper;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ConfigTestData {

    public static Grandfather getGrandfather(String id) {
        try {
            Grandfather gf = new Grandfather(id);
            gf.id = id;
            gf.prop1 = "value";
            gf.prop2 = new HashSet<String>(Arrays.asList("c", "b", "a"));
            gf.prop4 = new DateTime("2002-09-24-06:00");
            gf.prop5 = new DurationTime("PT15H");
            gf.prop8 = new Byte((byte) 1);
            gf.prop9 = new Long(1000000000000L);
            gf.prop10 = new Short((short) 123);
            gf.prop11 = new Float(12313.13);
            gf.prop12 = new Double(238.476238746834796);
            gf.prop13 = new Boolean(true);
            gf.prop14 = TimeUnit.NANOSECONDS;
            gf.prop15 = new URL("http://www.deephacks.org");
            gf.prop16 = new File(".").getAbsoluteFile();
            gf.prop17 = Arrays.asList(new File(".").getAbsoluteFile(),
                    new File(".").getAbsoluteFile());
            gf.prop18 = Arrays.asList(new URL("http://www.deephacks.org"), new URL(
                    "http://www.google.se"));
            gf.prop19 = Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS);
            gf.prop21 = 1;
            return gf;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Parent getParent(String parentId) {
        try {
            Parent gf = new Parent(parentId);
            gf.id = parentId;
            gf.prop1 = "value";
            gf.prop2 = new HashSet<String>(Arrays.asList("c", "b", "a"));
            gf.prop4 = new DateTime("2002-09-24-06:00");
            gf.prop5 = new DurationTime("PT15H");
            gf.prop8 = new Byte((byte) 1);
            gf.prop9 = new Long(1000000000000L);
            gf.prop10 = new Short((short) 123);
            gf.prop11 = new Float(12313.13);
            gf.prop12 = new Double(238.476238746834796);
            gf.prop13 = new Boolean(true);
            gf.prop15 = new URL("http://www.deephacks.org");
            gf.prop16 = new File(".").getAbsoluteFile();
            gf.prop17 = Arrays.asList(new File(".").getAbsoluteFile(),
                    new File(".").getAbsoluteFile());
            gf.prop18 = Arrays.asList(new URL("http://www.deephacks.org"), new URL(
                    "http://www.google.se"));
            gf.prop19 = Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS);
            return gf;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Child getChild(String childId) {
        try {
            Child gf = new Child(childId);
            gf.id = childId;
            gf.prop2 = new HashSet<String>(Arrays.asList("c", "b", "a"));
            gf.prop4 = new DateTime("2002-09-24-06:00");
            gf.prop5 = new DurationTime("PT15H");
            gf.prop8 = new Byte((byte) 1);
            gf.prop9 = new Long(1000000000000L);
            gf.prop10 = new Short((short) 123);
            gf.prop11 = new Float(12313.13);
            gf.prop12 = new Double(238.476238746834796);
            gf.prop13 = new Boolean(true);
            gf.prop15 = new URL("http://www.deephacks.org");
            gf.prop16 = new File(".").getAbsoluteFile();
            gf.prop17 = Arrays.asList(new File(".").getAbsoluteFile(),
                    new File(".").getAbsoluteFile());
            gf.prop18 = Arrays.asList(new URL("http://www.deephacks.org"), new URL(
                    "http://www.google.se"));
            gf.prop19 = Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS);
            return gf;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JSR303Validation getJSR303Validation(String id) {
        return new JSR303Validation(id);
    }

    public static Person getPerson(String id) {
        return new Person(id);
    }

    public static final String GRANDFATHER_SCHEMA_NAME = "GrandfatherSchemaName";

    @Config(name = GRANDFATHER_SCHEMA_NAME, desc = "a test class")
    public static class Grandfather {

        @Id(name = "id", desc = "desc")
        private String id;
        @Config(desc = "prop1Desc")
        @Index
        private String prop1 = "defaultValue";
        @Config(desc = "prop2Desc")
        private Set<String> prop2;
        @Config(desc = "prop3Desc")
        @Index
        private List<Integer> prop3 = Arrays.asList(1, 2, 3);
        @Config(desc = "prop4Desc")
        private DateTime prop4;
        @Config(desc = "prop5Desc")
        private DurationTime prop5;
        @Config(desc = "prop7Desc")
        @Index
        private List<Parent> prop7;
        @Config(desc = "prop8Desc")
        private byte prop8;
        @Config(desc = "prop9Desc")
        private long prop9;
        @Config(desc = "prop10Desc")
        private short prop10;
        @Config(desc = "prop11Desc")
        private float prop11;
        @Config(desc = "prop12Desc")
        @Index
        private double prop12;
        @Config(desc = "prop13Desc")
        private boolean prop13;
        @Config(desc = "prop14Desc")
        private TimeUnit prop14 = TimeUnit.MICROSECONDS;
        @Config(desc = "prop15Desc")
        private URL prop15;
        @Config(desc = "prop16Desc")
        private File prop16;
        @Config(desc = "prop17Desc")
        private List<File> prop17;
        @Config(desc = "prop18Desc")
        private List<URL> prop18;
        @Config(desc = "prop19Desc")
        private List<TimeUnit> prop19 = Arrays.asList(TimeUnit.HOURS, TimeUnit.SECONDS);
        @Config(desc = "prop20Desc")
        private Map<String, Parent> prop20;
        @Config(desc = "prop21Desc")
        private int prop21;

        public Grandfather() {
        }

        public Grandfather(String id) {
            this.id = id;
        }

        public BeanId getBeanId() {
            return BeanId.create(id, GRANDFATHER_SCHEMA_NAME);
        }

        public String getId() {
            return id;
        }

        public String getProp1() {
            return prop1;
        }

        public Set<String> getProp2() {
            return prop2;
        }

        public List<Integer> getProp3() {
            return prop3;
        }

        public DateTime getProp4() {
            return prop4;
        }

        public DurationTime getProp5() {
            return prop5;
        }

        public List<Parent> getProp7() {
            return prop7;
        }

        public byte getProp8() {
            return prop8;
        }

        public long getProp9() {
            return prop9;
        }

        public short getProp10() {
            return prop10;
        }

        public float getProp11() {
            return prop11;
        }

        public double getProp12() {
            return prop12;
        }

        public boolean getProp13() {
            return prop13;
        }

        public TimeUnit getProp14() {
            return prop14;
        }

        public URL getProp15() {
            return prop15;
        }

        public File getProp16() {
            return prop16;
        }

        public List<File> getProp17() {
            return prop17;
        }

        public List<URL> getProp18() {
            return prop18;
        }

        public List<TimeUnit> getProp19() {
            return prop19;
        }

        public Map<String, Parent> getProp20() {
            return prop20;
        }

        public int getProp21() {
            return prop21;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setProp1(String prop1) {
            this.prop1 = prop1;
        }

        public void setProp2(Set<String> prop2) {
            this.prop2 = prop2;
        }

        public void setProp3(List<Integer> prop3) {
            this.prop3 = prop3;
        }

        public void setProp4(DateTime prop4) {
            this.prop4 = prop4;
        }

        public void setProp5(DurationTime prop5) {
            this.prop5 = prop5;
        }

        public void setProp7(List<Parent> prop7) {
            this.prop7 = prop7;
        }

        public void setProp8(byte prop8) {
            this.prop8 = prop8;
        }

        public void setProp9(long prop9) {
            this.prop9 = prop9;
        }

        public void setProp10(short prop10) {
            this.prop10 = prop10;
        }

        public void setProp11(float prop11) {
            this.prop11 = prop11;
        }

        public void setProp12(double prop12) {
            this.prop12 = prop12;
        }

        public void setProp13(boolean prop13) {
            this.prop13 = prop13;
        }

        public void setProp14(TimeUnit prop14) {
            this.prop14 = prop14;
        }

        public void setProp15(URL prop15) {
            this.prop15 = prop15;
        }

        public void setProp16(File prop16) {
            this.prop16 = prop16;
        }

        public void setProp17(List<File> prop17) {
            this.prop17 = prop17;
        }

        public void setProp18(List<URL> prop18) {
            this.prop18 = prop18;
        }

        public void setProp19(List<TimeUnit> prop19) {
            this.prop19 = prop19;
        }

        public void setProp20(Map<String, Parent> prop20) {
            this.prop20 = prop20;
        }

        public void setProp21(int prop21) {
            this.prop21 = prop21;
        }

        public void add(Parent... p) {
            if (prop7 == null) {
                prop7 = new ArrayList<>();
            }
            prop7.addAll(Arrays.asList(p));
        }

        public List<Parent> getParents() {
            return prop7;
        }

        public void resetParents() {
            prop7.clear();
        }

        public void put(Parent p) {
            if (prop20 == null) {
                prop20 = new HashMap<>();
            }
            prop20.put(p.id, p);
        }

        public Bean toBean() {
            return ConversionUtils.toBean(this);
        }
    }

    public static final String PARENT_SCHEMA_NAME = "ParentSchemaName";

    @Config(name = PARENT_SCHEMA_NAME, desc = "a test class")
    public static class Parent {

        @Id(desc = "desc")
        private String id;
        private String prop1 = "defaultValue";
        private Set<String> prop2;
        private List<Integer> prop3 = Arrays.asList(1, 2, 3);
        private DateTime prop4;
        private DurationTime prop5;
        private Child prop6;
        private List<Child> prop7;
        private Byte prop8;
        private Long prop9;
        private Short prop10;
        private float prop11;
        private Double prop12;
        private Boolean prop13;
        private TimeUnit prop14;
        private URL prop15;
        private File prop16;
        private List<File> prop17;
        private List<URL> prop18;
        private List<TimeUnit> prop19;
        private Map<String, Child> prop20;

        public String getProp1() {
            return prop1;
        }

        public Set<String> getProp2() {
            return prop2;
        }

        public List<Integer> getProp3() {
            return prop3;
        }

        public DateTime getProp4() {
            return prop4;
        }

        public DurationTime getProp5() {
            return prop5;
        }

        public Child getProp6() {
            return prop6;
        }

        public List<Child> getProp7() {
            return prop7;
        }

        public Byte getProp8() {
            return prop8;
        }

        public Long getProp9() {
            return prop9;
        }

        public Short getProp10() {
            return prop10;
        }

        public float getProp11() {
            return prop11;
        }

        public Double getProp12() {
            return prop12;
        }

        public Boolean getProp13() {
            return prop13;
        }

        public TimeUnit getProp14() {
            return prop14;
        }

        public URL getProp15() {
            return prop15;
        }

        public File getProp16() {
            return prop16;
        }

        public List<File> getProp17() {
            return prop17;
        }

        public List<URL> getProp18() {
            return prop18;
        }

        public List<TimeUnit> getProp19() {
            return prop19;
        }

        public Map<String, Child> getProp20() {
            return prop20;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setProp1(String prop1) {
            this.prop1 = prop1;
        }

        public void setProp2(Set<String> prop2) {
            this.prop2 = prop2;
        }

        public void setProp3(List<Integer> prop3) {
            this.prop3 = prop3;
        }

        public void setProp4(DateTime prop4) {
            this.prop4 = prop4;
        }

        public void setProp5(DurationTime prop5) {
            this.prop5 = prop5;
        }

        public void setProp6(Child prop6) {
            this.prop6 = prop6;
        }

        public void setProp7(List<Child> prop7) {
            this.prop7 = prop7;
        }

        public void setProp8(Byte prop8) {
            this.prop8 = prop8;
        }

        public void setProp9(Long prop9) {
            this.prop9 = prop9;
        }

        public void setProp10(Short prop10) {
            this.prop10 = prop10;
        }

        public void setProp11(float prop11) {
            this.prop11 = prop11;
        }

        public void setProp12(Double prop12) {
            this.prop12 = prop12;
        }

        public void setProp13(Boolean prop13) {
            this.prop13 = prop13;
        }

        public void setProp14(TimeUnit prop14) {
            this.prop14 = prop14;
        }

        public void setProp15(URL prop15) {
            this.prop15 = prop15;
        }

        public void setProp16(File prop16) {
            this.prop16 = prop16;
        }

        public void setProp17(List<File> prop17) {
            this.prop17 = prop17;
        }

        public void setProp18(List<URL> prop18) {
            this.prop18 = prop18;
        }

        public void setProp19(List<TimeUnit> prop19) {
            this.prop19 = prop19;
        }

        public void setProp20(Map<String, Child> prop20) {
            this.prop20 = prop20;
        }

        public void add(Child... c) {
            if (prop7 == null) {
                prop7 = new ArrayList<Child>();
            }
            prop7.addAll(Arrays.asList(c));
        }

        public List<Child> getChilds() {
            return prop7;
        }

        public void resetChilds() {
            prop7.clear();
        }

        public BeanId getBeanId() {
            return BeanId.create(id, PARENT_SCHEMA_NAME);
        }

        public String getId() {
            return id;
        }

        public void set(Child c) {
            prop6 = c;
        }

        public void put(Child c) {
            if (prop20 == null) {
                prop20 = new HashMap<String, Child>();
            }
            prop20.put(c.id, c);
        }

        public Parent() {
        }

        public Parent(String id) {
            this.id = id;
        }

        public Bean toBean() {
            return ConversionUtils.toBean(this);
        }
    }

    public static final String CHILD_SCHEMA_NAME = "ChildSchemaName";

    @Config(name = CHILD_SCHEMA_NAME, desc = "a test class")
    public static class Child {

        @Id(desc = "desc")
        private String id;
        @Config(desc = "prop1Desc")
        private String prop1;
        @Config(desc = "prop2Desc")
        private Set<String> prop2;
        @Config(desc = "prop3Desc")
        private List<Integer> prop3 = Arrays.asList(1, 2, 3);
        @Config(desc = "prop4Desc")
        private DateTime prop4;
        @Config(desc = "prop5Desc")
        private DurationTime prop5;
        @Config(desc = "prop8Desc")
        private Byte prop8;
        @Min(1)
        @Config(desc = "prop9Desc")
        private long prop9 = 100000000;
        @Config(desc = "prop10Desc")
        private Short prop10;
        @Config(desc = "prop11Desc")
        private Float prop11;
        @Config(desc = "prop12Desc")
        private Double prop12;
        @Config(desc = "prop13Desc")
        private Boolean prop13;
        @Config(desc = "prop15Desc")
        private URL prop15;
        @Config(desc = "prop16Desc")
        private File prop16;
        @Config(desc = "prop17Desc")
        private List<File> prop17;
        @Config(desc = "prop18Desc")
        private List<URL> prop18;
        @Config(desc = "prop19Desc")
        private List<TimeUnit> prop19;
        public Child(String id) {
            this.id = id;
        }

        public BeanId getBeanId() {
            return BeanId.create(id, CHILD_SCHEMA_NAME);
        }

        public String getId() {
            return id;
        }

        public Child() {

        }

        public Set<String> getProp2() {
            return prop2;
        }

        public List<Integer> getProp3() {
            return prop3;
        }

        public DateTime getProp4() {
            return prop4;
        }

        public DurationTime getProp5() {
            return prop5;
        }

        public Byte getProp8() {
            return prop8;
        }

        public long getProp9() {
            return prop9;
        }

        public Short getProp10() {
            return prop10;
        }

        public Float getProp11() {
            return prop11;
        }

        public Double getProp12() {
            return prop12;
        }

        public Boolean getProp13() {
            return prop13;
        }

        public URL getProp15() {
            return prop15;
        }

        public File getProp16() {
            return prop16;
        }

        public List<File> getProp17() {
            return prop17;
        }

        public List<URL> getProp18() {
            return prop18;
        }

        public List<TimeUnit> getProp19() {
            return prop19;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setProp1(String prop1) {
            this.prop1 = prop1;
        }

        public void setProp2(Set<String> prop2) {
            this.prop2 = prop2;
        }

        public void setProp3(List<Integer> prop3) {
            this.prop3 = prop3;
        }

        public void setProp4(DateTime prop4) {
            this.prop4 = prop4;
        }

        public void setProp5(DurationTime prop5) {
            this.prop5 = prop5;
        }

        public void setProp8(Byte prop8) {
            this.prop8 = prop8;
        }

        public void setProp9(long prop9) {
            this.prop9 = prop9;
        }

        public void setProp10(Short prop10) {
            this.prop10 = prop10;
        }

        public void setProp11(Float prop11) {
            this.prop11 = prop11;
        }

        public void setProp12(Double prop12) {
            this.prop12 = prop12;
        }

        public void setProp13(Boolean prop13) {
            this.prop13 = prop13;
        }

        public void setProp15(URL prop15) {
            this.prop15 = prop15;
        }

        public void setProp16(File prop16) {
            this.prop16 = prop16;
        }

        public void setProp17(List<File> prop17) {
            this.prop17 = prop17;
        }

        public void setProp18(List<URL> prop18) {
            this.prop18 = prop18;
        }

        public void setProp19(List<TimeUnit> prop19) {
            this.prop19 = prop19;
        }

        public Bean toBean() {
            return ConversionUtils.toBean(this);
        }
    }

    public static final String SINGLETON_SCHEMA_NAME = "SingletonSchemaName";

    @Config(name = SINGLETON_SCHEMA_NAME, desc = "")
    public static class Singleton {
        @Config(desc = "")
        private String property;

        @Config(desc="")
        private Parent parent;

        public BeanId getBeanId() {
            return BeanId.createSingleton(SINGLETON_SCHEMA_NAME);
        }

        public String getProperty() {
            return property;
        }

        public Parent getParent() {
            return parent;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }

    public static final String SINGLETON_PARENT_SCHEMA_NAME = "SingletonParentSchemaName";

    @Config(name = SINGLETON_PARENT_SCHEMA_NAME, desc = "")
    public static class SingletonParent {
        @Config(desc = "")
        private Singleton singleton;

        @Config(desc = "")
        private String property;

        public SingletonParent() {

        }

        public String getProperty() {
            return property;
        }

        public Singleton getSingleton() {
            return singleton;
        }

        public void setSingleton(Singleton singleton) {
            this.singleton = singleton;
        }
    }

    public static final String VALIDATION_SCHEMA_NAME = "ValidationSchemaName";

    @Config(name = VALIDATION_SCHEMA_NAME, desc = "JSR303 validation assertion")
    @SuppressWarnings("unused")
    public static class JSR303Validation {

        @Id(desc = "validationCheckId")
        private String id;

        @Config(desc = "Assert that JSR303 works as expected.")
        @FirstUpper
        @Size(min = 2, max = 50)
        private String prop;

        @Config(desc = "Assert that JSR303 works as expected.")
        @NotNull
        private Integer height;

        @Config(desc = "Assert that JSR303 works as expected.")
        @NotNull
        private Integer width;

        @Max(20)
        private int getArea() {
            // check for null, height and weight may not have been set.
            if (height != null && width != null) {
                return height * width;
            }
            return 0;
        }

        private JSR303Validation(String id) {
            this.id = id;
        }

        public JSR303Validation() {

        }

        public BeanId getBeanId() {
            return BeanId.create(id, VALIDATION_SCHEMA_NAME);
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public String getId() {
            return id;
        }

        public String getProp() {
            return prop;
        }

        public Integer getHeight() {
            return height;
        }

        public Integer getWidth() {
            return width;
        }
    }

    @Config(name = "person", desc = "desc")
    public static class Person {
        @Id(desc = "")
        private String id;

        @Config(desc = "")
        private Person bestFriend;

        @Config(desc = "")
        private List<Person> closeFriends = new ArrayList<>();

        @Config(desc = "")
        private Map<String, Person> colleauges = new HashMap<>();

        public Person(String id) {
            this.id = id;
        }

        public Person() {

        }
    }

    @Config(name="A")
    public static class A {
        private String name;
        public A () {

        }
        public A (String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }


    @Config(name="B")
    public static class B {
        private String name;

        public B () {

        }

        public B (String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
