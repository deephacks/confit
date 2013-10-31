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
package org.deephacks.confit.internal.cached;

import org.deephacks.confit.model.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.test.ConfigTestData.*;
import org.junit.Before;
import org.junit.Test;

import static org.deephacks.confit.internal.core.schema.ConversionUtils.toBean;
import static org.deephacks.confit.test.ConfigTestData.*;
import static org.junit.Assert.*;

public class CachedCacheManagerTest {
    private static final CachedCacheManager manager = new CachedCacheManager();

    private BeanId g1 = BeanId.create("g1", GRANDFATHER_SCHEMA_NAME);
    private BeanId p1 = BeanId.create("p1", PARENT_SCHEMA_NAME);
    private BeanId c1 = BeanId.create("c1", CHILD_SCHEMA_NAME);
    private Grandfather grandfather = getGrandfather(g1.getInstanceId());
    private Parent parent = getParent(p1.getInstanceId());
    private Child child = getChild(c1.getInstanceId());
    static SchemaManager schemaManager = SchemaManager.lookup();

    static {
        schemaManager.register(Grandfather.class, Parent.class, Child.class);
    }

    static Schema gSchema = schemaManager.getSchema(Grandfather.class);
    static Schema pSchema = schemaManager.getSchema(Parent.class);
    static Schema cSchema = schemaManager.getSchema(Child.class);

    static {

    }
    static  {
        manager.registerSchema(gSchema);
        manager.registerSchema(pSchema);
        manager.registerSchema(cSchema);
    }

    @Before
    public void setup() {
        manager.clear();
    }

    @Test
    public void test_get_put() throws Exception {
        parent.put(child);
        grandfather.put(parent);
        manager.put(toBean(grandfather));
        manager.put(toBean(parent));
        manager.put(toBean(child));

        Grandfather gf = (Grandfather) manager.get(g1);
        Parent p = (Parent) manager.get(p1);
        Child c = (Child) manager.get(c1);

        // TODO: need better assertions to make sure that
        // cache return correct configurable proxies
        assertEquals(gf.getBeanId(), grandfather.getBeanId());
        assertEquals(p.getBeanId(), parent.getBeanId());
        assertEquals(c.getBeanId(), child.getBeanId());


    }



}
