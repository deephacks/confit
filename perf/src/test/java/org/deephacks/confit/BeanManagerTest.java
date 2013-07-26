package org.deephacks.confit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.MetricsRegistry;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.internal.core.runtime.ClassToSchemaConverter;
import org.deephacks.confit.internal.core.runtime.FieldToSchemaPropertyConverter;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.internal.core.Lookup;
import org.deephacks.confit.query.ConfigResultSet;
import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.spi.Conversion;
import org.deephacks.confit.test.ConfigTestData.*;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.deephacks.confit.test.ConfigTestData.*;

public abstract class BeanManagerTest {
    static {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ERROR);
    }
    public static Conversion conversion = Conversion.get();
    static {
        conversion.register(new ClassToSchemaConverter());
        conversion.register(new FieldToSchemaPropertyConverter());
    }
    public ConfigContext config = ConfigContext.get();
    public AdminContext admin = AdminContext.get();

    public static final List<List<Bean>> beans = new ArrayList<>();
    private MetricsRegistry registry = new MetricsRegistry();

    private int reps;
    private int items;

    public <T extends BeanManager> void setupTest(T beanManager, int items, int reps) throws Exception {
        this.reps = reps;
        this.items = items;
        Lookup.get().register(BeanManager.class, beanManager);
        for (int i = 0; i < items; i++) {
            String id = Integer.toString(i);
            Grandfather g = getGrandfather(id);
            Parent p = getParent(id);
            Child c = getChild(id);
            g.add(p);
            p.add(c);
            ArrayList<Bean> unit = new ArrayList<>();
            unit.add(conversion.convert(g, Bean.class));
            unit.add(conversion.convert(p, Bean.class));
            unit.add(conversion.convert(c, Bean.class));
            beans.add(unit);
        }
        config.register(Grandfather.class, Parent.class, Child.class);
        cleanUpState();
    }

    public abstract void cleanUpState();

    public void warmUpCreate(int num) {
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < items; j++) {
                admin.create(beans.get(j));
            }
            cleanUpState();
        }
    }

    public Histogram executeCreate(int reps) {
        Histogram responseTime = registry.newHistogram(BeanManagerTest.class, "executeCreate");
        for (int i = 0; i < reps; i++) {
            for (int j = 0; j < items; j++) {
                Stopwatch w = new Stopwatch().start();
                admin.create(beans.get(j));
                responseTime.update(w.elapsedMillis());
            }
            if((i + 1) < reps) {
                cleanUpState();
            }
        }
        return responseTime;
    }

    public Histogram executeQuery() {
        Histogram responseTime = registry.newHistogram(BeanManagerTest.class, "executeQuery");
        for (int i = 0; i < reps; i++) {
            Stopwatch w = new Stopwatch().start();
            ConfigResultSet<Grandfather> resultSet = config.newQuery(Grandfather.class).retrieve();
            List<Grandfather> grandfathers = Lists.newArrayList(resultSet);
            responseTime.update(w.elapsedMillis());
        }
        return responseTime;
    }

    public Histogram executeAll() {
        Histogram responseTime = registry.newHistogram(BeanManagerTest.class, "executeAll");
        for (int i = 0; i < reps; i++) {
            Stopwatch w = new Stopwatch().start();
            List<Grandfather> resultSet = config.list(Grandfather.class);
            responseTime.update(w.elapsedMillis());
        }
        return responseTime;
    }
  }
