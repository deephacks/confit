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

import com.google.common.collect.ImmutableList;
import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.test.ConfigTestData.*;

import java.util.Collection;

import static org.deephacks.confit.test.ConfigTestData.*;
import static org.deephacks.confit.test.ConversionUtils.toBeans;

public abstract class ConfigDefaultSetup {
    protected ConfigContext config = ConfigContext.get();
    protected AdminContext admin = AdminContext.get();
    protected Child c1;
    protected Child c2;
    protected Parent p1;
    protected Parent p2;
    protected Grandfather g1;
    protected Grandfather g2;
    protected SingletonParent sp1;
    protected Singleton s1;
    protected JSR303Validation jsr303;
    protected static Collection<Bean> defaultBeans;

    protected void setupDefaultConfigData() {

        sp1 = new SingletonParent();
        s1 = new Singleton();

        c1 = getChild("c1");
        c2 = getChild("c2");

        p1 = getParent("p1");
        p1.add(c2, c1);
        p1.set(c1);
        p1.put(c1);
        p1.put(c2);

        p2 = getParent("p2");
        p2.add(c1, c2);
        p2.set(c2);
        p2.put(c1);
        p2.put(c2);

        g1 = getGrandfather("g1");
        g1.add(p1, p2);

        g2 = getGrandfather("g2");
        g2.add(p1, p2);
        g2.put(p1);

        jsr303 = getJSR303Validation("jsr303");

        config.register(Grandfather.class, Parent.class, Child.class, Singleton.class,
                SingletonParent.class, JSR303Validation.class);
        if (defaultBeans == null) {
            // toBeans steals quite a bit of performance when having larger hierarchies.
            defaultBeans = ImmutableList.copyOf(toBeans(c1, c2, p1, p2, g1, g2));
        }
    }
}
