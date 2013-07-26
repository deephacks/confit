package org.deephacks.confit.test.integration;

import org.deephacks.confit.ConfigContext;
import org.deephacks.confit.admin.AdminContext;
import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Bean;
import org.deephacks.confit.test.ConfigTestData.JSR303Validation;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.deephacks.confit.test.validation.BinaryTree;
import org.deephacks.confit.test.validation.BinaryTreeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Set;

import static org.deephacks.confit.model.Events.CFG309;
import static org.deephacks.confit.test.ConfigTestData.getJSR303Validation;
import static org.deephacks.confit.test.ConversionUtils.toBean;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(FeatureTestsRunner.class)
public class IntegrationValidationTests {
    protected JSR303Validation jsr303;
    private ConfigContext config = ConfigContext.get();
    private AdminContext admin = AdminContext.get();

    @Before
    public void before() {
        jsr303 = getJSR303Validation("jsr303");
    }

    @Test
    public void test_JSR303_validation_success() {
        jsr303.setProp("Valid upper value for @FirstUpperValidator");
        jsr303.setWidth(2);
        jsr303.setHeight(2);
        Bean jsr303Bean = toBean(jsr303);
        admin.create(jsr303Bean);
    }

    @Test
    public void test_JSR303_validation_failures() {
        jsr303.setProp("Valid upper value for @FirstUpperValidator");
        jsr303.setWidth(20);
        jsr303.setHeight(20);
        Bean jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Area exceeds constraint");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }

        jsr303.setProp("test");
        jsr303.setWidth(1);
        jsr303.setHeight(1);
        jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Prop does not have first upper case.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }
        jsr303.setProp("T");
        jsr303.setWidth(1);
        jsr303.setHeight(1);
        jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Prop must be longer than one char");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }

        jsr303.setProp("Valid upper value for @FirstUpperValidator");
        jsr303.setWidth(null);
        jsr303.setHeight(null);
        jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Width and height may not be null.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }
    }

    /**
     * Test that JSR303 validation can operate on references in order
     * to validate that a binary tree is correct after modification.
     *
     * This is what the test-tree looks like.
     *
     *          _______5______
     *         /              \
     *     ___4            ___8__
     *    /               /      \
     *   _2               6      10
     *  /  \               \    /
     *  1   3               7   9
     */
    @Test
    public void test_JSR303_reference_validation() {
        config.register(BinaryTree.class);

        // generate the tree seen in the javadoc
        Set<Bean> beans = BinaryTreeUtils.getTree(5, Arrays.asList(8, 4, 10, 2, 5, 1, 9, 6, 3, 7));
        admin.create(beans);
        BinaryTree root = config.get("5", BinaryTree.class).get();
        BinaryTreeUtils.printPretty(root);
        /**
         * TEST1: a correct delete of 8 where 9 takes its place
         */
        Bean eight = BinaryTreeUtils.getBean(8, beans);
        Bean ten = BinaryTreeUtils.getBean(10, beans);
        // 8: set value 9
        eight.setProperty("value", "9");
        // 10: remove reference to 9
        ten.remove("left");
        admin.set(Arrays.asList(eight, ten));

        root = config.get("5", BinaryTree.class).get();
        // take a look at the tree after delete
        BinaryTreeUtils.printPretty(root);
        /**
         * TEST2: set 4 to 7 should fail
         */
        try {
            Bean four = BinaryTreeUtils.getBean(4, beans);
            four.setProperty("value", "7");
            admin.merge(four);
            fail("setting 4 to 7 should not be possible.");
        } catch (AbortRuntimeException e) {
            // BinaryTreeValidator should notice that 7 satisfies left 2
            // but not parent 5 (7 should be to right of 5)
            assertThat(e.getEvent().getCode(), is(CFG309));
            // display error message to prove that validator found the error.
            System.out.println(e.getEvent().getMessage());
        }
    }

}
