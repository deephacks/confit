package org.deephacks.confit.examples.basic;

import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.examples.basic.BasicExample.A;
import org.deephacks.confit.examples.basic.BasicExample.B;

import java.io.File;
import java.net.URL;

public class ConfExample {
    static {
        // by default conf-it uses example.conf located in the current
        // class loader. We cant use that here so redirect to the file located
        // in this directory
        File file = getConfFile();
        if (!file.exists()) {
            throw new IllegalStateException("conf file ["+file.getAbsolutePath()+"] does not exist");
        }
        System.setProperty("application.conf", file.getAbsolutePath());
    }
    private static final ConfigContext config = ConfigContext.get();
    private static final AdminContext admin = AdminContext.get();
    static {
        config.register(A.class, B.class);
    }


    public static void main(String[] args) {
        simpleConfFileOnly();
        referenceConfFileOnly();
        confFileFallback();
    }

    private static void simpleConfFileOnly() {
        A singleton = config.get(A.class);
        System.out.println(singleton.getValue());

    }

    private static void referenceConfFileOnly() {

    }

    private static void confFileFallback() {
    }

    public static File getConfFile() {
        return new File(getRoot(), "./src/main/resources/example.conf");
    }
    public static File getRoot() {
        final String clsUri = ConfExample.class.getName().replace('.', '/') + ".class";
        final URL url = ConfExample.class.getClassLoader().getResource(clsUri);
        final String clsPath = url.getPath();
        final File target_test_classes = new File(clsPath.substring(0,
                clsPath.length() - clsUri.length()));
        return target_test_classes.getParentFile().getParentFile();
    }
}
