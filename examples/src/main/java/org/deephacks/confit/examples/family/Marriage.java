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

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.deephacks.confit.Config;
import org.deephacks.confit.Id;

/**
 * A recursive example of a family tree. 
 * 
 * See the parent-child.png for an illustration of the potential realtionship 
 * between marriage and persons.
 */
@Config(desc = "A marriage between two people.", name = "Marriage")
@MarriageConstraint
@SuppressWarnings("unused")
public class Marriage {
    @Id(desc = "a marriage")
    private String id;

    @Config(desc = "Two married people.")
    @Size(min = 2, max = 2)
    @NotNull
    private List<Person> couple = new ArrayList<Person>();

    @Config(desc = "Children from this marriage.")
    private List<Person> children = new ArrayList<Person>();

    public List<Person> getCouple() {
        return couple;
    }

    public List<Person> getChildren() {
        return children;
    }

    public Person getMale() {
        for (Person p : couple) {
            if (p.gender == Gender.MALE) {
                return p;
            }
        }
        return null;
    }

    public Person getFemale() {
        for (Person p : couple) {
            if (p.gender == Gender.FEMALE) {
                return p;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        if (couple == null) {
            couple = new ArrayList<Person>();
        }
        if (children == null) {
            children = new ArrayList<Person>();
        }
        return "Marriage between " + couple.get(0) + " and " + couple.get(1) + " with children "
                + children;
    }
}
