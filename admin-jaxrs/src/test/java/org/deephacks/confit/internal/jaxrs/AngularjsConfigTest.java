package org.deephacks.confit.internal.jaxrs;

/**
 * Run and point browser to http://127.0.0.1:8080/index.html to do manual
 * test for the angular.js web application.
 */
public class AngularjsConfigTest {

    public static void main(String[] args) throws Exception {
        JettyServer.start();
        Thread.sleep(Integer.MAX_VALUE);
    }
}
