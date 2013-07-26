package org.deephacks.confit;

import com.google.common.base.Stopwatch;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.reporting.ConsoleReporter;
import org.deephacks.confit.internal.jpa.Jpa20BeanManager;
import org.deephacks.confit.internal.jpa.Jpa20BeanManagerIntegrationSetup.ProviderCombination;

public class Jpa20BeanManagerTest extends BeanManagerTest {
    protected static final ProviderCombination provider = new ProviderCombination("mysql", "hibernate");
    static {
        provider.createEntityManagerFactory();
    }
    @Override
    public void cleanUpState() {
        provider.createDatabase();
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("typesafe.query.enabled", "true");
        Jpa20BeanManagerTest test = new Jpa20BeanManagerTest();
        int reps = 100;
        int items = 100;
        test.setupTest(new Jpa20BeanManager(), items, reps);
        // test.warmUpCreate(5);
        System.out.println("Warmup done.");
        Stopwatch w = new Stopwatch().start();
        Histogram histogram = test.executeCreate(10);
        System.out.println("executeCreate took " + w.elapsedMillis() / reps + " ms");
        report(histogram);
        /*
        w = new Stopwatch().start();
        histogram = test.executeAll();
        System.out.println("executeAll took " + w.elapsedMillis() / reps + " ms");
        report(histogram);
        */
        System.out.println("now");
        Thread.sleep(5000);
        w = new Stopwatch().start();
        histogram = test.executeQuery();
        System.out.println("executeQuery took " + w.elapsedMillis() / reps + " ms");
        report(histogram);
        Thread.sleep(2000000);

    }

    public static void report(Histogram histogram) {
        ConsoleReporter reporter = new ConsoleReporter(System.out);
        MetricName name = new MetricName(Jpa20BeanManagerTest.class, "test");
        reporter.processHistogram(name, histogram, System.out);
    }


}
