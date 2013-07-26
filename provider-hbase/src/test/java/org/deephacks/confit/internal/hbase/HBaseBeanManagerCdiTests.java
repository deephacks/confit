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
package org.deephacks.confit.internal.hbase;

import org.deephacks.confit.test.FeatureTests;
import org.deephacks.confit.test.FeatureTestsBuilder.TestRound;
import org.deephacks.confit.test.cdi.CdiFeatureTestBuilder;
import org.deephacks.confit.test.cdi.CdiFeatureTestsRunner;
import org.junit.runner.RunWith;

import javax.inject.Singleton;
import java.util.List;

@Singleton
@RunWith(CdiFeatureTestsRunner.class)
public class HBaseBeanManagerCdiTests implements FeatureTests {

    @Override
    public List<TestRound> build() {
        HBaseUtil.getLocalTestManager();
        return CdiFeatureTestBuilder.named(HBaseBeanManagerCdiTests.class.getSimpleName()).build();
    }
}
