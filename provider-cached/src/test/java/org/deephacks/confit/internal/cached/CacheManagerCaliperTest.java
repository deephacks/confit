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
package org.deephacks.confit.internal.cached;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.test.ConfigTestData.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.deephacks.confit.internal.core.schema.ConversionUtils.toBean;
import static org.deephacks.confit.test.ConfigTestData.*;

public class CacheManagerCaliperTest extends SimpleBenchmark {
    private CachedCacheManager manager = new CachedCacheManager();
    private List<BeanId> ids = new ArrayList<>();
    @Param
    int size;

    @Override
    protected void setUp() throws Exception {
        for (int i = 0; i < size; i++) {
            String gId = UUID.randomUUID().toString();
            BeanId g = BeanId.create(gId, GRANDFATHER_SCHEMA_NAME);
            ids.add(g);
            BeanId p = BeanId.create(UUID.randomUUID().toString(), PARENT_SCHEMA_NAME);
            BeanId c = BeanId.create(UUID.randomUUID().toString(), CHILD_SCHEMA_NAME);
            Grandfather grandfather = getGrandfather(g.getInstanceId());
            Parent parent = getParent(p.getInstanceId());
            Child child = getChild(c.getInstanceId());
            parent.put(child);
            grandfather.put(parent);
            manager.put(toBean(grandfather));
        }
    }

    public void timeGet(int reps) {
        for (int i = 0; i < reps; i++) {
            manager.get(ids.iterator().next());
        }
    }

    public void timeList(int reps) {
        for (int i = 0; i < reps; i++) {
            manager.get(GRANDFATHER_SCHEMA_NAME);
        }
    }

    public static void main(String[] args) throws Exception {
        Runner.main(CacheManagerCaliperTest.class, new String[]{"-Dsize=1000"});
    }


}
