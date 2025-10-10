package org.datastructure;

/**
 * 普通二叉树实现
 */
public class BinaryTreeDS implements DataStructure {

    // ================== 节点类 ==================
    static class Node {
        int value;
        Node left;
        Node right;

        Node(int value) {
            this.value = value;
        }
    }

    private Node root; // 根节点

    public BinaryTreeDS() {
        root = null;
    }

    // ================== DataStructure接口实现 ==================

    @Override
    public void insert(int value) {
        root = insertRecursive(root, value);
    }

    @Override
    public void delete(int value) {
        root = deleteRecursive(root, value);
    }

    @Override
    public boolean search(int value) {
        return searchRecursive(root, value);
    }

    // ================== 递归方法 ==================

    private Node insertRecursive(Node current, int value) {
        if (current == null) {
            return new Node(value);
        }
        // ⚠️ 普通二叉树这里随便插：比如先放左，再放右
        if (current.left == null) {
            current.left = insertRecursive(current.left, value);
        } else {
            current.right = insertRecursive(current.right, value);
        }
        return current;
    }

    private Node deleteRecursive(Node current, int value) {
        if (current == null) return null;

        if (current.value == value) {
            // 情况1：叶子节点
            if (current.left == null && current.right == null) {
                return null;
            }
            // 情况2：只有一个子节点
            if (current.left == null) return current.right;
            if (current.right == null) return current.left;

            // 情况3：有两个子节点（这里随便选一个替代，简单实现）
            int smallestValue = findSmallestValue(current.right);
            current.value = smallestValue;
            current.right = deleteRecursive(current.right, smallestValue);
            return current;
        }

        // 向左右子树递归
        current.left = deleteRecursive(current.left, value);
        current.right = deleteRecursive(current.right, value);
        return current;
    }

    private boolean searchRecursive(Node current, int value) {
        if (current == null) return false;
        if (current.value == value) return true;
        return searchRecursive(current.left, value) || searchRecursive(current.right, value);
    }

    private int findSmallestValue(Node root) {
        return root.left == null ? root.value : findSmallestValue(root.left);
    }

    // ================== 其他方法 ==================

    /** 前序遍历 */
    public void traversePreOrder() {
        traversePreOrder(root);
        System.out.println();
    }

    private void traversePreOrder(Node node) {
        if (node != null) {
            System.out.print(node.value + " ");
            traversePreOrder(node.left);
            traversePreOrder(node.right);
        }
    }

    /** 获取根节点（方便可视化调用） */
    public Node getRoot() {
        return root;
    }
}

