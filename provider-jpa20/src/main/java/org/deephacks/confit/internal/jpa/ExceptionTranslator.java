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

import static org.deephacks.confit.model.Events.CFG301_MISSING_RUNTIME_REF;
import static org.deephacks.confit.model.Events.CFG302_CANNOT_DELETE_BEAN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.deephacks.confit.model.Bean.BeanId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to decouple clients from specific JPA+database specific exceptions
 * into a canonical error model.
 *
 * To do so we must also remove list compile dependencies to specific to JPA and database
 * provider implementation specific exceptions.
 *
 * Semantics are mostly same accross implementations but specific classes are is quite different.
 *
 */
public class ExceptionTranslator {
    private static Logger LOG = LoggerFactory.getLogger(ExceptionTranslator.class);

    private static final String MYSQL = "com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException";
    private static final String GENERIC = "java.sql.SQLIntegrityConstraintViolationException";
    private static final String HIBERNATE = "org.hibernate.exception.ConstraintViolationException";
    private static final Set<String> CONSTRAINT_VIOLATION = new HashSet<String>();

    static {
        CONSTRAINT_VIOLATION.addAll(Arrays.asList(MYSQL, GENERIC, HIBERNATE));
    }

    public static void translateDelete(Collection<BeanId> ids, Throwable e) {
        LOG.debug("", e);
        Throwable cause = e;

        while ((cause = cause.getCause()) != null) {
            System.out.println(cause);
            if (CONSTRAINT_VIOLATION.contains(cause.getClass().getName())) {
                throw CFG302_CANNOT_DELETE_BEAN(ids);
            }
        }
        throw new RuntimeException(e);
    }

    public static void translateDelete(Collection<String> ids, String schemaName, Throwable e) {
        LOG.debug("", e);
        Throwable cause = e;
        Collection<BeanId> beanIds = new ArrayList<BeanId>();
        for (String instanceId : ids) {
            beanIds.add(BeanId.create(instanceId, schemaName));
        }
        while ((cause = cause.getCause()) != null) {
            if (CONSTRAINT_VIOLATION.contains(cause.getClass().getName())) {
                throw CFG302_CANNOT_DELETE_BEAN(beanIds);
            }

        }
        throw new RuntimeException(e);
    }

    public static void translateMerge(BeanId id, Throwable e) {
        LOG.debug("", e);
        Throwable cause = e;

        while ((cause = cause.getCause()) != null) {
            if (CONSTRAINT_VIOLATION.contains(cause.getClass().getName())) {
                throw CFG301_MISSING_RUNTIME_REF(id);
            }
        }
        throw new RuntimeException(e);
    }

    public static void translateMerge(Throwable e) {
        LOG.debug("", e);
        Throwable cause = e;

        while ((cause = cause.getCause()) != null) {
            if (CONSTRAINT_VIOLATION.contains(cause.getClass().getName())) {
                throw CFG301_MISSING_RUNTIME_REF();
            }
        }
        throw new RuntimeException(e);
    }
}
