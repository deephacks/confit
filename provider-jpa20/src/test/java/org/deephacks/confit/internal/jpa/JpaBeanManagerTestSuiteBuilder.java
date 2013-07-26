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

import org.deephacks.confit.test.FeatureTestsBuilder;
import org.deephacks.confit.test.bean.BeanManagerGetTests;

import java.util.ArrayList;
import java.util.List;

public class JpaBeanManagerTestSuiteBuilder extends FeatureTestsBuilder {

    public static JpaBeanManagerTestSuiteBuilder named(String name) {
        JpaBeanManagerTestSuiteBuilder builder = new JpaBeanManagerTestSuiteBuilder();
        builder.name = name;
        return builder;
    }

    @Override
    public List<TestRound> build() {
        // TODO Auto-generated method stub
        return super.build();
    }

    @Override
    protected List<Class<?>> getTests() {
        List<Class<?>> tests = new ArrayList<>();
        // TODO: foxme
        // tests.add(BeanManagerDeleteTests.class);
        // tests.add(BeanManagerCreateTests.class);
        tests.add(BeanManagerGetTests.class);
        return tests;
    }

}
