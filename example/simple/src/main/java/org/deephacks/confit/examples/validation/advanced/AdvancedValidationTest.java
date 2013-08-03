package org.deephacks.confit.examples.validation.advanced;

import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;

import static org.deephacks.confit.examples.validation.advanced.FamilyTestData.createFamily;

public class AdvancedValidationTest {

    public static void main(String[] args) {
        ConfigContext config = ConfigContext.lookup();
        config.register(Person.class, Marriage.class);
        Bean child1 = createFamily("1", "MALE");
        Bean child2 = createFamily("2", "FEMALE");
        Bean child3 = createFamily("3", "MALE");
        Bean child4 = createFamily("4", "FEMALE");

        Bean child5 = createFamily("1.1", child1, child2, "MALE");
        Bean child6 = createFamily("1.2", child3, child4, "FEMALE");

        Bean child7 = createFamily("1.1.1", child5, child6, "MALE");

        Bean b = Bean.create(BeanId.create("1.1", "Person"));
        System.out.println("done");
    }
}
