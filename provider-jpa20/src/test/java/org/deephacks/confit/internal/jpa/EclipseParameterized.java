package org.deephacks.confit.internal.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;

/**
 * The original parameterized runner does not display, nor link paremeterized tests to
 * Eclipse IDE correctly. This is a hazzle when writing and debugging tests since is hard
 * to track and re-execute exact parameters to the right test.
 */
public class EclipseParameterized extends Parameterized {

    private Description description;

    public EclipseParameterized(Class<?> cl) throws Throwable {
        super(cl);
        List<String> parameterLabels = getParameterGroup();
        setDescription(parameterLabels);
    }

    /**
     * Get a toString label for each @Parameters.
     */
    private List<String> getParameterGroup() throws Throwable {
        Collection<Object[]> parameterArrays = getParameters();
        List<String> labels = new ArrayList<String>();
        for (Object[] parameterArray : parameterArrays) {
            String label = parameterArray[0].toString();
            labels.add(label);
        }
        return labels;
    }

    /**
     * Returns the parameters of the method annotated with @Parameters.
     */
    @SuppressWarnings("unchecked")
    private Collection<Object[]> getParameters() throws Throwable {
        List<FrameworkMethod> m = getTestClass().getAnnotatedMethods(Parameters.class);
        FrameworkMethod method = m.get(0);
        return (Collection<Object[]>) method.invokeExplosively(this, (Object[]) null);
    }

    /**
     * Group test methods results to a specific set of parameters for a specific
     * test class execution.
     */
    private void setDescription(List<String> parameterLabels) throws Exception {
        Description orgTestDesc = super.getDescription();
        description = Description.createSuiteDescription(orgTestDesc.getDisplayName());
        // paramDesc is the description which label and group a test class
        // execution with a set of parameters.
        ArrayList<Description> orgParamDescs = orgTestDesc.getChildren();
        int paramCount = orgParamDescs.size();
        if (paramCount != parameterLabels.size())
            throw new Exception("Number of labels and parameters must match.");
        Iterator<String> paramLabel = parameterLabels.iterator();
        for (Description orgParamDesc : orgParamDescs) {
            Description paramDesc = Description.createSuiteDescription(paramLabel.next());
            ArrayList<Description> testMethodDescs = orgParamDesc.getChildren();
            for (Description testMethodDesc : testMethodDescs)
                paramDesc.addChild(testMethodDesc);
            description.addChild(paramDesc);
        }
    }

    @Override
    public Description getDescription() {
        return description;
    }

}