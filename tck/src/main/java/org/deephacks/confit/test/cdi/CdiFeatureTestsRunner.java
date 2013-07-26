package org.deephacks.confit.test.cdi;

import org.deephacks.confit.test.FeatureTestsBuilder.TestRound;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class CdiFeatureTestsRunner extends FeatureTestsRunner {
    private Weld weld;
    private WeldContainer container;

    public CdiFeatureTestsRunner(final Class<?> cls) throws Throwable {
        super(cls);
    }

    @Override
    public Object getFeatureTest(Class<?> cls) {
        return createCdiInstance(cls);
    }

    public Object createCdiInstance(Class<?> cls) {
        try {
            if (container == null) {
                this.weld = new Weld();
                try {
                    this.container = weld.initialize();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new InitializationError(e);
                }
            }
            return container.instance().select(cls).get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public BlockJUnit4ClassRunner getRunnerForParameters(TestRound round)
            throws InitializationError {
        return new CdiTestClassRunnerForParameters(round);
    }

    public class CdiTestClassRunnerForParameters extends TestClassRunnerForParameters {

        public CdiTestClassRunnerForParameters(TestRound round) throws InitializationError {
            super(round);
        }

        @Override
        public Object createTest() throws Exception {
            return createCdiInstance(getTestClass().getJavaClass());
        }

    }
}
