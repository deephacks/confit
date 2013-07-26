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
package org.deephacks.confit.examples.family;

import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.internal.jpa.Database;
import org.deephacks.confit.internal.jpa.EntityManagerProvider;
import org.deephacks.confit.internal.jpa.Jpa20BeanManager;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.LookupProxy;

import java.io.File;
import java.net.URL;

import static org.deephacks.confit.examples.family.FamilyTestData.createFamily;
import static org.deephacks.confit.internal.jpa.Database.MYSQL;
import static org.deephacks.confit.test.JUnitUtils.getMavenProjectChildFile;

/**
 * FamilyTest is dependent on mysql to work correctly.
 */
@SuppressWarnings("unused")
public class FamilyTest {
    // intentially removed from test suite. Uncomment for demo purposes in eclipse.
    // @Test
    public void passing_test() {
        File scriptDir = getMavenProjectChildFile(Jpa20BeanManager.class,
                "src/main/resources/META-INF/");

        Database.create(MYSQL, scriptDir).initalize();
        LookupProxy.register(BeanManager.class, new Jpa20BeanManager());
        AdminContext admin = AdminContext.get();
        ConfigContext config = ConfigContext.get();
        EntityManagerProvider provider = new EntityManagerProvider();
        URL url = Thread.currentThread().getContextClassLoader()
                .getResource("META-INF/jpa.properties");
        provider.createEntityManagerFactory(url);
        config.register(Person.class, Marriage.class);
        Bean child1 = createFamily("1", "MALE");
        Bean child2 = createFamily("2", "FEMALE");
        Bean child3 = createFamily("3", "MALE");
        Bean child4 = createFamily("4", "FEMALE");

        Bean child5 = createFamily("1.1", child1, child2, "MALE");
        Bean child6 = createFamily("1.2", child3, child4, "FEMALE");

        Bean child7 = createFamily("1.1.1", child5, child6, "MALE");

        Bean b = Bean.create(BeanId.create("1.1", "Person"));

        provider.closeEntityManager();

    }
}
