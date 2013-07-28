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

import com.google.common.base.Optional;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.spi.CacheManager;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;

import static org.deephacks.confit.model.Events.CFG101_SCHEMA_NOT_EXIST;

/**
 * ConfigCore implements functionality that is common to both admin and config context.
 */
@Singleton
public final class ConfigCore {
    private SystemProperties properties = SystemProperties.instance();
    private Lookup lookup = Lookup.get();
    private static final String CONFIG_QUERY_FEATURE_ENABLED_PROPERTY = "confit.query.enabled";
    private static Optional<CacheManager> cacheManager;
    private boolean configQueryFeatureEnabled = false;

    public void setSchema(Map<String, Schema> schemas, Collection<Bean> beans) {
        for (Bean bean : beans) {
            setSchema(bean, schemas);
        }
    }

    public void setSchema(Bean b, Map<String, Schema> schemas) {
        Schema s = schemas.get(b.getId().getSchemaName());
        if (s == null) {
            throw CFG101_SCHEMA_NOT_EXIST(b.getId().getSchemaName());
        }
        b.set(s);
        for (BeanId id : b.getReferences()) {
            Bean ref = id.getBean();
            if (ref != null && ref.getSchema() == null) {
                setSchema(ref, schemas);
            }
        }
    }

    public void setSchema(Map<String, Schema> schemas, Map<BeanId, Bean> beans) {
        for (Bean b : beans.values()) {
            setSchema(b, schemas);
        }
    }

    public Optional<CacheManager> lookupCacheManager() {
        Optional<String> optional = properties.get(CONFIG_QUERY_FEATURE_ENABLED_PROPERTY);
        if(optional.isPresent()){
            configQueryFeatureEnabled = true;
        }
        if(!configQueryFeatureEnabled) {
            return Optional.absent();
        }
        CacheManager manager = lookup.lookup(CacheManager.class);
        if (manager != null) {
            cacheManager = Optional.of(manager);
        } else {
            cacheManager = Optional.absent();
        }
        return cacheManager;
    }

}
