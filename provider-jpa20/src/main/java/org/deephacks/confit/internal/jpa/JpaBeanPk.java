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
public class JpaBeanPk implements Serializable {
    private static final long serialVersionUID = 6120090280743915215L;

    @Column(name = "BEAN_ID", nullable = false)
    protected String id;

    @Column(name = "BEAN_SCHEMA_NAME", nullable = false)
    protected String schemaName;

    /**
     * Empty constructor is a JPA requirement.
     */
    public JpaBeanPk() {

    }

    public JpaBeanPk(BeanId id) {
        this.id = id.getInstanceId();
        this.schemaName = id.getSchemaName();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JpaBeanPk)) {
            return false;
        }
        JpaBeanPk o = (JpaBeanPk) obj;
        return equal(id, o.id) && equal(schemaName, o.schemaName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, schemaName);
    }

    @Override
    public String toString() {
        return toStringHelper(JpaBeanPk.class).add("id", id).add("schemaName", schemaName)
                .toString();
    }

}
