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

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.deephacks.confit.model.Bean.BeanId;

import com.google.common.base.Objects;

@Embeddable
public class JpaPropertyPk implements Serializable {

    private static final long serialVersionUID = 403712155247347862L;

    @Column(name = "FK_BEAN_ID", nullable = false)
    private String id;

    @Column(name = "FK_BEAN_SCHEMA_NAME", nullable = false)
    private String schemaName;

    @Column(name = "PROP_NAME", nullable = false)
    private String propName;

    /**
     * Empty constructor is a JPA requirement.
     */
    public JpaPropertyPk() {

    }

    public JpaPropertyPk(BeanId id, String propName) {
        this.propName = propName;
        this.id = id.getInstanceId();
        this.schemaName = id.getSchemaName();

    }

    public BeanId getBeanId() {
        return BeanId.create(id, schemaName);
    }

    public String getPropName() {
        return propName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JpaPropertyPk)) {
            return false;
        }
        JpaPropertyPk o = (JpaPropertyPk) obj;
        return equal(id, o.id) && equal(propName, o.propName) && equal(schemaName, o.schemaName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, schemaName, propName);
    }

    @Override
    public String toString() {
        return toStringHelper(JpaPropertyPk.class).add("id", id).add("schemaName", schemaName)
                .add("propName", propName).toString();
    }
}
