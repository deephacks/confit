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
package org.deephacks.confit.internal.core.yaml;

import com.google.common.io.Closeables;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureSetupTeardown;
import org.junit.Before;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

@FeatureSetupTeardown(BeanManager.class)
public class YamlBeanManagerSetup {

    @Before
    public void before() {
        File dir = YamlBeanManager.getStorageDir();
        clearStorageFile(dir, YamlBeanManager.YAML_EMPTY_FILE, YamlBeanManager.YAML_BEAN_FILE_NAME);
    }

    private static void clearStorageFile(File dir, String contents, String fileName) {
        FileWriter writer = null;
        try {
            // make sure dir exist
            if (!dir.exists()) {
                dir.mkdir();

            }
            File file = new File(dir, fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            writer = new FileWriter(new File(dir, fileName));
            writer.write(contents);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Closeables.closeQuietly(writer);
        }
    }

}
