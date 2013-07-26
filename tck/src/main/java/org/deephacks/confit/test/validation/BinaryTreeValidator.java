/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.confit.test.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator that make sure that properties of a binary tree holds:
 * 
 * - The left subtree of a node contains only nodes with keys less than the node's key.
 * - The right subtree of a node contains only nodes with keys greater than the node's key.
 * - Both the left and right subtrees must also be binary search trees.
 * 
 */
public class BinaryTreeValidator implements ConstraintValidator<BinaryTreeConstraint, BinaryTree> {

    @Override
    public void initialize(BinaryTreeConstraint constraintAnnotation) {

    }

    @Override
    public boolean isValid(BinaryTree current, ConstraintValidatorContext context) {
        /**
         * Assert that 'parent' children are correct
         */
        if (current.getParent() != null) {

            BinaryTree parent = current.getParent();
            if (current.getValue() > parent.getValue()) {
                if (parent.getRight() != null && current.getValue() != parent.getRight().getValue()) {
                    context.buildConstraintViolationWithTemplate(
                            "RULE1: " + current + " must be to right of " + parent)
                            .addConstraintViolation();
                    return false;
                }
            }

            if (current.getValue() < parent.getValue()) {
                if (parent.getLeft() != null && current.getValue() != parent.getLeft().getValue()) {
                    context.buildConstraintViolationWithTemplate(
                            "RULE2: " + current + " must be to left of " + parent)
                            .addConstraintViolation();
                    return false;
                }
            }
        }
        /**
         * Assert 'current' children are correct
         */
        if (current.getLeft() != null) {
            if (current.getValue() < current.getLeft().getValue()) {
                context.buildConstraintViolationWithTemplate(
                        "RULE3: " + current.getLeft() + " must be to right of " + current)
                        .addConstraintViolation();
                return false;
            }

        }
        if (current.getRight() != null) {
            if (current.getValue() > current.getRight().getValue()) {
                context.buildConstraintViolationWithTemplate(
                        "RULE4: " + current.getRight() + " must be to left of " + current)
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
