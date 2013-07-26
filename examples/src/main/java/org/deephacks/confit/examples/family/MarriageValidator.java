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

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class MarriageValidator implements ConstraintValidator<MarriageConstraint, Marriage> {

    @Override
    public void initialize(MarriageConstraint constraint) {
    }

    @Override
    public boolean isValid(Marriage marriage, ConstraintValidatorContext context) {
        /**
         * marriage must consist of a couple
         */
        if (marriage.getCouple() == null || marriage.getCouple().size() != 2) {
            context.buildConstraintViolationWithTemplate("marriage must be between two pepole")
                    .addConstraintViolation();
            return false;
        }
        /**
         * one male required in marriage
         */
        Person male = marriage.getMale();
        if (male == null) {
            context.buildConstraintViolationWithTemplate("no male in marriage")
                    .addConstraintViolation();
            return false;
        }
        /**
         * one female required in marriage
         */
        Person female = marriage.getFemale();
        if (female == null) {
            context.buildConstraintViolationWithTemplate("no female in marriage")
                    .addConstraintViolation();
            return false;
        }
        /**
         * female must have same lastname as male
         */
        if (!male.getLastName().equals(female.getLastName())) {
            context.buildConstraintViolationWithTemplate(
                    "female [" + female + "] lastname is not same as man [" + male + "]")
                    .addConstraintViolation();
            return false;
        }

        /**
         * children must have same lastname as father
         */
        for (Person child : marriage.getChildren()) {
            if (!male.lastName.equals(child.lastName)) {
                context.buildConstraintViolationWithTemplate(
                        "child [" + child + "] have lastname not matching dad ["
                                + marriage.getMale() + "]").addConstraintViolation();
                return false;
            }
        }
        /**
         * children in marriage must also belong to male
         */
        if (!consistentChildren(male, marriage.getChildren())) {
            context.buildConstraintViolationWithTemplate(
                    "Male [" + male + "] does not have same children as marriage.")
                    .addConstraintViolation();
            return false;
        }
        /**
         * children in marriage must also belong to female
         */
        if (!consistentChildren(female, marriage.getChildren())) {
            context.buildConstraintViolationWithTemplate(
                    "Female [" + female + "] does not have same children as marriage.")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    public boolean consistentChildren(Person p, List<Person> marriageChildren) {
        if (!p.getChildren().containsAll(marriageChildren)) {
            return false;
        }
        return true;
    }
}
