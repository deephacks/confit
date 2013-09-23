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

import org.deephacks.confit.model.Bean;
import org.deephacks.confit.model.BeanId;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BinaryTreeUtils {

    public static Set<Bean> getTree(Integer root, List<Integer> children) {
        Node rootNode = new Node(root);
        for (int i : children) {
            rootNode.insert(i);
        }
        Set<Bean> beans = new HashSet<>();
        rootNode.traverse(beans, null);
        return beans;
    }

    public static Bean getBean(int i, Set<Bean> beans) {
        for (Bean bean : beans) {
            if (new Integer(bean.getId().getInstanceId()).intValue() == i) {
                return bean;
            }
        }
        return null;
    }

    public static class Node {
        int value = 0;
        public Node left;
        public Node right;

        Node(int value) {
            this.value = value;
        }

        public Bean traverse(Set<Bean> beans, Bean parent) {
            Bean current = Bean.create(BeanId.create(value + "", "binarytree"));
            current.setProperty("value", value + "");
            if (parent != null) {
                current.setReference("parent", parent.getId());
            }
            if (left != null) {
                Bean leftBean = left.traverse(beans, current);
                current.setReference("left", leftBean.getId());
            }
            if (right != null) {
                Bean rightBean = right.traverse(beans, current);
                current.setReference("right", rightBean.getId());
            }
            beans.add(current);
            return current;
        }

        public void insert(int insert) {
            if (value == insert) {
                return;
            }
            if (value > insert) {
                if (left != null) {
                    left.insert(insert);
                } else {
                    left = new Node(insert);
                }
            } else {
                if (right != null) {
                    right.insert(insert);
                } else {
                    right = new Node(insert);
                }
            }
        }

        public Node delete(int delete) {
            if (value == delete) {
                if (left != null && right != null) {

                    Node x = right;
                    while (x.left != null) {
                        x = x.left;
                    }

                    value = x.value;
                    x.value = delete;
                    right = right.delete(delete);

                    return this;
                } else if (left != null) {
                    return left;
                } else if (right != null) {
                    return right;
                } else {
                    return null;
                }
            } else if (value > delete) {
                left = (left == null) ? null : left.delete(delete);
                return this;
            } else {
                right = (right == null) ? null : right.delete(delete);
                return this;
            }
        }

        public String toString() {
            return value + "";
        }
    }

    public static void printPretty(BinaryTree root) {
        TreePrinter.printPretty(root, 1, 0, new TreePrinter.PaddedWriter(System.out));

    }

    private static class TreePrinter {

        // Search for the deepest part of the tree
        private static <T> int maxHeight(BinaryTree t) {
            if (t == null)
                return 0;
            int leftHeight = maxHeight(t.getLeft());
            int rightHeight = maxHeight(t.getRight());
            return (leftHeight > rightHeight) ? leftHeight + 1 : rightHeight + 1;
        }

        // Pretty formatting of a binary tree to the output stream
        public static <T> void printPretty(BinaryTree tree, int level, int indentSpace,
                PaddedWriter out) {
            int h = maxHeight(tree);
            int BinaryTreesInThisLevel = 1;
            int branchLen = 2 * ((int) Math.pow(2.0, h) - 1) - (3 - level)
                    * (int) Math.pow(2.0, h - 1);
            int BinaryTreeSpaceLen = 2 + (level + 1) * (int) Math.pow(2.0, h);
            int startLen = branchLen + (3 - level) + indentSpace;

            Deque<BinaryTree> BinaryTreesQueue = new LinkedList<BinaryTree>();
            BinaryTreesQueue.offerLast(tree);
            for (int r = 1; r < h; r++) {
                printBranches(branchLen, BinaryTreeSpaceLen, startLen, BinaryTreesInThisLevel,
                        BinaryTreesQueue, out);
                branchLen = branchLen / 2 - 1;
                BinaryTreeSpaceLen = BinaryTreeSpaceLen / 2 + 1;
                startLen = branchLen + (3 - level) + indentSpace;
                printBinaryTrees(branchLen, BinaryTreeSpaceLen, startLen, BinaryTreesInThisLevel,
                        BinaryTreesQueue, out);

                for (int i = 0; i < BinaryTreesInThisLevel; i++) {
                    BinaryTree currBinaryTree = BinaryTreesQueue.pollFirst();
                    if (currBinaryTree != null) {
                        BinaryTreesQueue.offerLast(currBinaryTree.getLeft());
                        BinaryTreesQueue.offerLast(currBinaryTree.getRight());
                    } else {
                        BinaryTreesQueue.offerLast(null);
                        BinaryTreesQueue.offerLast(null);
                    }
                }
                BinaryTreesInThisLevel *= 2;
            }
            printBranches(branchLen, BinaryTreeSpaceLen, startLen, BinaryTreesInThisLevel,
                    BinaryTreesQueue, out);
            printLeaves(indentSpace, level, BinaryTreesInThisLevel, BinaryTreesQueue, out);
        }

        private static <T> void printBranches(int branchLen, int BinaryTreeSpaceLen, int startLen,
                int BinaryTreesInThisLevel, Deque<BinaryTree> BinaryTreesQueue, PaddedWriter out) {
            Iterator<BinaryTree> iterator = BinaryTreesQueue.iterator();
            for (int i = 0; i < BinaryTreesInThisLevel / 2; i++) {
                if (i == 0) {
                    out.setw(startLen - 1);
                } else {
                    out.setw(BinaryTreeSpaceLen - 2);
                }
                out.write();
                BinaryTree next = iterator.next();
                if (next != null) {
                    out.write("/");
                } else {
                    out.write(" ");
                }
                out.setw(2 * branchLen + 2);
                out.write();
                next = iterator.next();
                if (next != null) {
                    out.write("\\");
                } else {
                    out.write(" ");
                }
            }
            out.endl();
        }

        // Print the branches and BinaryTree (eg, ___10___ )
        private static <T> void printBinaryTrees(int branchLen, int BinaryTreeSpaceLen,
                int startLen, int BinaryTreesInThisLevel, Deque<BinaryTree> BinaryTreesQueue,
                PaddedWriter out) {
            Iterator<BinaryTree> iterator = BinaryTreesQueue.iterator();
            BinaryTree currentBinaryTree;
            for (int i = 0; i < BinaryTreesInThisLevel; i++) {
                currentBinaryTree = iterator.next();
                if (i == 0) {
                    out.setw(startLen);
                } else {
                    out.setw(BinaryTreeSpaceLen);
                }
                out.write();
                if (currentBinaryTree != null && currentBinaryTree.getLeft() != null) {
                    out.setfill('_');
                } else {
                    out.setfill(' ');
                }
                out.setw(branchLen + 2);
                if (currentBinaryTree != null) {
                    out.write(currentBinaryTree.toString());
                } else {
                    out.write();
                }
                if (currentBinaryTree != null && currentBinaryTree.getRight() != null) {
                    out.setfill('_');
                } else {
                    out.setfill(' ');
                }
                out.setw(branchLen);
                out.write();
                out.setfill(' ');
            }
            out.endl();
        }

        // Print the leaves only (just for the bottom row)
        private static <T> void printLeaves(int indentSpace, int level, int BinaryTreesInThisLevel,
                Deque<BinaryTree> BinaryTreesQueue, PaddedWriter out) {
            Iterator<BinaryTree> iterator = BinaryTreesQueue.iterator();
            BinaryTree currentBinaryTree;
            for (int i = 0; i < BinaryTreesInThisLevel; i++) {
                currentBinaryTree = iterator.next();
                if (i == 0) {
                    out.setw(indentSpace + 2);
                } else {
                    out.setw(2 * level + 2);
                }
                if (currentBinaryTree != null) {
                    out.write(currentBinaryTree.toString());
                } else {
                    out.write();
                }
            }
            out.endl();
        }

        public static class PaddedWriter {
            private int width = 0;
            private char fillChar = ' ';
            private final PrintStream writer;

            public PaddedWriter(PrintStream writer) {
                this.writer = writer;
            }

            void setw(int i) {
                width = i;
            }

            void setfill(char c) {
                fillChar = c;
            }

            void write(String str) {
                write(str.toCharArray());
            }

            void write(char[] buf) {
                if (buf.length < width) {
                    char[] pad = new char[width - buf.length];
                    Arrays.fill(pad, fillChar);
                    writer.print(pad);
                }
                writer.print(buf);
                setw(0);
            }

            void write() {
                char[] pad = new char[width];
                Arrays.fill(pad, fillChar);
                writer.print(pad);
                setw(0);
            }

            void endl() {
                writer.println();
                setw(0);
            }
        }
    }

}
