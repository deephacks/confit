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
package org.deephacks.confit.examples.validation.advanced;

import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;

public class FamilyTestData {
    static AdminContext admin = AdminContext.get();

    public static Bean createFamily(String prefix, String childGender) {
        int counter = 0;
        String lastName = "lastName";
        Bean male = getMale(id(prefix, counter++), lastName);
        Bean female = getFemale(id(prefix, counter++), lastName);
        Bean child = getMale(id(prefix, counter++), lastName);
        child.setProperty("gender", childGender);
        Bean marriage = getMarriage(male.getId().getInstanceId(), female.getId().getInstanceId());
        male.addReference("children", child.getId());
        female.addReference("children", child.getId());
        marriage.addReference("children", child.getId());

        admin.create(child);
        admin.create(male);
        admin.create(female);
        admin.create(marriage);

        return child;
    }

    public static Bean createFamily(String prefix, Bean child1, Bean child2, String childGender) {
        int counter = 0;
        String lastName = "lastName";

        Bean male = child1;
        Bean FEMALE = child2;
        Bean child = getMale(id(prefix, counter++), lastName);
        child.setProperty("gender", childGender);
        Bean marriage = getMarriage(male.getId().getInstanceId(), FEMALE.getId().getInstanceId());
        male.addReference("children", child.getId());
        FEMALE.addReference("children", child.getId());

        marriage.addReference("children", child.getId());
        admin.create(child);
        admin.merge(male);
        admin.merge(FEMALE);
        admin.create(marriage);
        return child;
    }

    private static String id(String prefix, int counter) {
        return prefix + "." + ++counter;
    }

    public static Bean getMale(String name, String lastName) {
        Bean b = getPerson(name, lastName);
        b.addProperty("gender", "MALE");
        return b;
    }

    public static Bean getFemale(String name, String lastName) {
        Bean b = getPerson(name, lastName);
        b.addProperty("gender", "FEMALE");
        return b;
    }

    public static Bean getPerson(String name, String lastName) {
        Bean b = Bean.create(BeanId.create(name, "Person"));
        b.addProperty("firstName", name);
        b.addProperty("lastName", lastName);
        return b;
    }

    public static Bean getMarriage(String male, String FEMALE) {
        Bean b = Bean.create(BeanId.create(male + "+" + FEMALE, "Marriage"));
        b.addReference("couple", BeanId.create(male, "Person"));
        b.addReference("couple", BeanId.create(FEMALE, "Person"));
        return b;
    }
}
