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
package org.deephacks.confit.test.bean;

import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureTestsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a JUnit test suite that test a bean manager implementation.
 */
public class BeanManagerTestSuiteBuilder extends FeatureTestsBuilder {

    public static BeanManagerTestSuiteBuilder named(String name) {
        BeanManagerTestSuiteBuilder builder = new BeanManagerTestSuiteBuilder();
        builder.name = name;
        return builder;
    }

    public BeanManagerTestSuiteBuilder using(BeanManager manager) {
        using(BeanManager.class, manager);
        return this;
    }

    @Override
    protected List<Class<?>> getTests() {
        List<Class<?>> tests = new ArrayList<>();
        tests.add(BeanManagerDeleteTests.class);
        tests.add(BeanManagerCreateTests.class);
        tests.add(BeanManagerGetTests.class);
        return tests;
    }

}
