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

import javax.validation.constraints.NotNull;

import org.deephacks.confit.Config;
import org.deephacks.confit.Id;

@Config(desc = "")
public class AbstractPerson {
    @Id(desc = "id")
    @NotNull
    protected String id;

    @Config(desc = "First name of a person")
    @NotNull
    protected String firstName;

    @Config(desc = "Last name of a person")
    @NotNull
    protected String lastName;

    @Config(desc = "Gender")
    @NotNull
    protected Gender gender;

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String toString() {
        return firstName + " " + lastName;
    }
}
