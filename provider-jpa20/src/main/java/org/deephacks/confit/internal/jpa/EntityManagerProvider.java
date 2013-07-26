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

import com.google.common.io.Closeables;
import org.deephacks.confit.model.ThreadLocalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class EntityManagerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(EntityManagerProvider.class);
    public static final String UNIT_NAME = "confit-jpa-unit";
    private EntityManagerFactory factory;

    public void closeEntityManager() {
        EntityManager manager = ThreadLocalManager.pop(EntityManager.class);
        if (manager == null) {
            LOG.warn("Cannot close, no EntityManager was found in thread local.");
            return;
        }
        if (!manager.isOpen()) {
            LOG.warn("Cannot close, EntityManager has already been closed.");
            return;
        }
        manager.close();
    }

    public EntityManagerFactory createEntityManagerFactory(URL prop) {
        return createEntityManagerFactory(UNIT_NAME, prop);
    }

    public EntityManagerFactory createEntityManagerFactory(File prop) {
        try {
            return createEntityManagerFactory(UNIT_NAME, prop.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public EntityManagerFactory createEntityManagerFactory(String unitName, File prop) {
        try {
            return createEntityManagerFactory(UNIT_NAME, prop.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public EntityManagerFactory createEntityManagerFactory(String unitName, URL prop) {
        if (factory != null && factory.isOpen()) {
            return factory;
        }
        FileInputStream in = null;
        Properties p = new Properties();
        try {
            p.load(prop.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Closeables.closeQuietly(in);
        }
        factory = Persistence.createEntityManagerFactory(unitName, p);
        return factory;
    }

    public void closeEntityManagerFactory() {
        if (factory == null) {
            return;
        }
        if (factory.isOpen()) {
            factory.close();
        }
        factory = null;
    }

}
