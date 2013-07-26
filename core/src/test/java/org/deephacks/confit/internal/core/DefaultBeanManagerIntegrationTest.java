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
package org.deephacks.confit.internal.core;

import org.deephacks.confit.internal.core.runtime.DefaultBeanManager;
import org.deephacks.confit.test.FeatureTests;
import org.deephacks.confit.test.FeatureTestsBuilder.TestRound;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.deephacks.confit.test.integration.IntegrationConfigTests;
import org.deephacks.confit.test.integration.IntegrationTestsBuilder;
import org.deephacks.confit.test.integration.IntegrationValidationTests;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(FeatureTestsRunner.class)
public class DefaultBeanManagerIntegrationTest implements FeatureTests {
    @Override
    public List<TestRound> build() {
        return IntegrationTestsBuilder.named(DefaultBeanManagerIntegrationTest.class.getSimpleName())
                .using(new DefaultBeanManager())
                // .addTest(BeanQueryTest.class)
                .addTest(IntegrationConfigTests.class)
                .addTest(IntegrationValidationTests.class)
                .build();
    }
}
