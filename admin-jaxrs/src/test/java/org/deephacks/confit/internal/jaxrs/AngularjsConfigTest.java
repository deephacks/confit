package org.deephacks.confit.internal.jaxrs;

import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.jaxrs.AdminContextJaxrsProxy;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.Bean.BeanId;

/**
 * Run and point browser to http://127.0.0.1:8080/index.html to do manual
 * test for the angular.js web application.
 */
public class AngularjsConfigTest {

    public static void main(String[] args) throws Exception {
        // JettyServer.start();

        AdminContext admin = AdminContextJaxrsProxy.get("localhost", 8080, "jaxrs");
        for (int i = 0; i < 500; i++) {
            Bean bean = Bean.create(BeanId.create(String.format("%05d", i), "servers"));
            admin.create(bean);

        }

    }
}
