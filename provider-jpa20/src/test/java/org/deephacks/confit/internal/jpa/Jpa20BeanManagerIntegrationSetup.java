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
package org.deephacks.confit.internal.jpa;

import org.deephacks.confit.internal.jpa.JpaUtils.Jpaprovider;
import org.deephacks.confit.internal.core.Lookup;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureSetupTeardown;
import org.deephacks.confit.test.JUnitUtils;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;

import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.deephacks.confit.internal.jpa.Database.*;
import static org.deephacks.confit.internal.jpa.JpaUtils.ECLIPSELINK;
import static org.deephacks.confit.internal.jpa.JpaUtils.HIBERNATE;

@FeatureSetupTeardown(BeanManager.class)
public class Jpa20BeanManagerIntegrationSetup {
    /** Possible values: MYSQL, POSTGRESQL, DERBY */
    public static List<String> ALL_DB_PROVIDERS = Arrays.asList(MYSQL, DERBY, POSTGRESQL);
    /** DERBY default to avoid failures when mysql or postgres not already installed */
    public static List<String> RUN_TEST_WITH_DB_PROVIDERS = Arrays.asList(DERBY);
    /** entity manager provider */
    private static EntityManagerProvider provider;
    /** test hibernate and eclipselink */
    public static List<String> TEST_JPA_PROVIDERS = Arrays.asList(HIBERNATE, ECLIPSELINK);
    /** Current database and jpa provider combination */
    private static ProviderCombination CURRENT_COMBO;
    /** Unique jpa/database provider combination for a specifci test execution. */
    private ProviderCombination parameter;

    public Jpa20BeanManagerIntegrationSetup(ProviderCombination parameter) {
        if (!parameter.equals(CURRENT_COMBO)) {
            if (provider != null) {
                provider.closeEntityManagerFactory();
                provider = null;
            }
        }
        CURRENT_COMBO = parameter;
        this.parameter = parameter;

    }

    @Before
    public void setup() {
        File targetDir = JUnitUtils.getMavenProjectChildFile(Jpa20BeanManager.class, "target");
        File jpaProperties = new File(targetDir, "jpa.properties");
        parameter.jpaProvider.write(jpaProperties);
        parameter.database.initalize();

        if (provider == null) {
            provider = new EntityManagerProvider();
            EntityManagerFactory emf = provider.createEntityManagerFactory(jpaProperties);
            Lookup.get().register(EntityManagerFactory.class, emf);
        }
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> parameters = new ArrayList<>();
        List<List<String>> list = new ArrayList<>();
        list.add(RUN_TEST_WITH_DB_PROVIDERS);
        list.add(TEST_JPA_PROVIDERS);

        for (List<String> combination : getCombinations(list)) {
            parameters.add(new Object[] { new ProviderCombination(combination.get(0), combination
                    .get(1)) });
        }
        return parameters;
    }

    private static <T> List<List<T>> getCombinations(List<List<T>> listOfLists) {
        List<List<T>> returned = new ArrayList<>();
        if (listOfLists.size() == 1) {
            for (T item : listOfLists.get(0)) {
                List<T> list = new ArrayList<>();
                list.add(item);
                returned.add(list);
            }
            return returned;
        }
        List<T> itemList = listOfLists.get(0);
        for (List<T> possibleList : getCombinations(listOfLists.subList(1, listOfLists.size()))) {
            for (T item : itemList) {
                List<T> addedList = new ArrayList<>();
                addedList.add(item);
                addedList.addAll(possibleList);
                returned.add(addedList);
            }
        }
        return returned;
    }

    public static class ProviderCombination {
        private Database database;
        private Jpaprovider jpaProvider;

        public ProviderCombination(String dbProvider, String jpaProvider) {
            File scriptDir = JUnitUtils.getMavenProjectChildFile(Jpa20BeanManager.class,
                    "src/main/resources/META-INF/");
            this.database = Database.create(dbProvider, scriptDir);
            this.jpaProvider = Jpaprovider.create(jpaProvider, database);
        }

        public void createDatabase() {
            database.initalize();
        }

        public void createEntityManagerFactory() {
            File targetDir = JUnitUtils.getMavenProjectChildFile(Jpa20BeanManager.class, "target");
            File jpaProperties = new File(targetDir, "jpa.properties");
            jpaProvider.write(jpaProperties);

            provider = new EntityManagerProvider();
            EntityManagerFactory emf = provider.createEntityManagerFactory(jpaProperties);
            Lookup.get().register(EntityManagerFactory.class, emf);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ProviderCombination other = (ProviderCombination) obj;
            if (database == null) {
                if (other.database != null)
                    return false;
            } else if (!database.equals(other.database))
                return false;
            if (jpaProvider == null) {
                if (other.jpaProvider != null)
                    return false;
            } else if (!jpaProvider.equals(other.jpaProvider))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return database.getDatabaseProvider() + "+" + jpaProvider.getClass().getSimpleName();
        }

    }

}
