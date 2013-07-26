package org.deephacks.confit.jaxrs;

import org.codehaus.jackson.map.DeserializationConfig;
import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.internal.jaxrs.JaxrsConfigEndpoint;
import org.deephacks.confit.internal.jaxrs.JaxrsConfigExceptionHandler;
import org.deephacks.confit.test.ConfigTestData.Child;
import org.deephacks.confit.test.ConfigTestData.Grandfather;
import org.deephacks.confit.test.ConfigTestData.Parent;
import org.deephacks.confit.test.ConfigTestData.Singleton;
import org.deephacks.confit.test.ConfigTestData.SingletonParent;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJacksonProvider;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class JettyServer {
    private static Server server;
    private static AtomicBoolean RUNNING = new AtomicBoolean(false);
    static  {
        ConfigContext config = ConfigContext.get();
        config.register(Child.class, Parent.class, Grandfather.class, Singleton.class, SingletonParent.class);
    }
    public static void start() {
        if (RUNNING.get()) {
            return;
        }
        server = new Server(8080);
        ResteasyJacksonProvider provider = new ResteasyJacksonProvider();
        provider.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplication(new JaxrsApplication());

        ResteasyProviderFactory resteasyFactory = ResteasyProviderFactory.getInstance();
        resteasyFactory.registerProviderInstance(provider);

        deployment.setProviderFactory(resteasyFactory);

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setAttribute(ResteasyDeployment.class.getName(), deployment);
        servletContextHandler.setContextPath("/");
        ServletHolder h = new ServletHolder(new HttpServletDispatcher());
        h.setInitParameter("javax.ws.rs.Application", JaxrsApplication.class.getName());
        servletContextHandler.addServlet(h, JaxrsConfigEndpoint.PATH + "/*");

        HandlerList handlers = new HandlerList();

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        File root = new File(computeMavenProjectRoot(JaxrsConfigEndpoint.class), "src/main/resources/html");
        resourceHandler.setResourceBase(root.getAbsolutePath());
        handlers.addHandler(resourceHandler);
        handlers.addHandler(servletContextHandler);
        server.setHandler(handlers);
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        RUNNING.set(true);
        ShutdownHook.install(new Thread() {

            @Override
            public void run() {
                try {
                    server.stop();
                    RUNNING.set(false);
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            }
        });
    }

    private static class JaxrsApplication extends Application {
        private static final Set<Object> singletons = new HashSet<>();
        static {
            singletons.add(new JaxrsConfigEndpoint(AdminContext.get()));
        }

        @Override
        public Set<Class<?>> getClasses() {
            HashSet<Class<?>> providers = new HashSet<>();
            providers.add(JaxrsConfigExceptionHandler.class);
            return providers;
        }

        @Override
        public Set<Object> getSingletons() {
            return singletons;
        }
    }

    static class ShutdownHook {
        private static final Logger log = LoggerFactory.getLogger(ShutdownHook.class);

        static void install(final Thread threadToJoin) {
            Thread thread = new ShutdownHookThread(threadToJoin);
            Runtime.getRuntime().addShutdownHook(thread);
            log.debug("Create shutdownhook: " + thread.getName());
        }

        private static class ShutdownHookThread extends Thread {
            private final Thread threadToJoin;

            private ShutdownHookThread(final Thread threadToJoin) {
                super("ShutdownHook: " + threadToJoin.getName());
                this.threadToJoin = threadToJoin;
            }

            @Override
            public void run() {
                log.debug("Starting " + getName());
                shutdown(threadToJoin, 30000);
                log.debug("Finished " + getName());
            }
        }

        public static void shutdown(final Thread t, final long joinwait) {
            if (t == null)
                return;
            t.start();
            while (t.isAlive()) {
                try {
                    t.join(joinwait);
                } catch (InterruptedException e) {
                    log.warn(t.getName() + "; joinwait=" + joinwait, e);
                }
            }
        }
    }
    public static File computeMavenProjectRoot(Class<?> anyTestClass) {
        final String clsUri = anyTestClass.getName().replace('.', '/') + ".class";
        final URL url = anyTestClass.getClassLoader().getResource(clsUri);
        final String clsPath = url.getPath();
        // located in ./target/test-classes or ./eclipse-out/target
        final File target_test_classes = new File(clsPath.substring(0,
                clsPath.length() - clsUri.length()));
        // get parent's parent
        return target_test_classes.getParentFile().getParentFile();
    }
}
