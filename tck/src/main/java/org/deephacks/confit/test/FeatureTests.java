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

import java.util.List;

import org.deephacks.confit.test.FeatureTestsBuilder.TestRound;
import org.junit.runner.RunWith;

/**
 * {@link FeatureTests} are implemented by classes that provide a set of 
 * junit tests testing certain features. These classes must also use the
 * annotation {@link RunWith} with value {@link FeatureTestsRunner}. 
 */
public interface FeatureTests {
    /**
     * Returns the set of tests to be run. 
     */
    public List<TestRound> build();

}
