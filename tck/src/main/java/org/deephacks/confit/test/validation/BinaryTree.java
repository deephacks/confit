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

import org.deephacks.confit.Config;
import org.deephacks.confit.Id;

import javax.validation.constraints.NotNull;

@Config(name = "binarytree", desc = "A binary tree")
@BinaryTreeConstraint
public class BinaryTree {

    @Id(desc = "id of the current tree")
    private String id;

    @Config(desc = "value of the current tree")
    @NotNull
    private Integer value;

    @Config(desc = "parent tree")
    private BinaryTree parent;

    @Config(desc = "left tree")
    private BinaryTree left;

    @Config(desc = "right tree")
    private BinaryTree right;

    public int getId() {
        return new Integer(value).intValue();
    }

    public int getValue() {
        return value.intValue();
    }

    public BinaryTree getParent() {
        return parent;
    }

    public BinaryTree getLeft() {
        return left;
    }

    public BinaryTree getRight() {
        return right;
    }

    public String toString() {
        return id + "=" + value;
    }

}
