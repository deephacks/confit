package org.deephacks.confit.internal.jaxrs;

import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.jaxrs.AdminContextJaxrsProxy;
import org.deephacks.confit.test.ConfigTestData.Child;
import org.deephacks.confit.test.ConfigTestData.Grandfather;
import org.deephacks.confit.test.ConfigTestData.Parent;

/**
 * Run and point browser to http://127.0.0.1:8080/index.html to do manual
 * test for the angular.js web application.
 */
public class AngularjsConfigTest {

    public static void main(String[] args) throws Exception {
        JettyServer.start();

        AdminContext admin = AdminContextJaxrsProxy.get("localhost", 8080);
        for (int i = 0; i < 500; i++) {
            Child c = new Child(String.format("%05d", i));
            admin.createObject(c);
        }
        for (int i = 0; i < 500; i++) {
            Parent p = new Parent(String.format("%05d", i));
            admin.createObject(p);
        }
        for (int i = 0; i < 500; i++) {
            Grandfather g = new Grandfather(String.format("%05d", i));
            admin.createObject(g);
        }
        Thread.sleep(Integer.MAX_VALUE);
    }
}
