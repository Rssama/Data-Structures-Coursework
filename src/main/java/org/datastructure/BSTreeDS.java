package org.datastructure;


/**
 * 二叉搜索树 (BST) 实现
 */
public class BSTreeDS implements DataStructure {

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

    public BSTreeDS() {
        root = null;
    }

    // ================== DataStructure 接口实现 ==================

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

    /** 插入：小的放左，大的放右 */
    private Node insertRecursive(Node current, int value) {
        if (current == null) {
            return new Node(value);
        }

        if (value < current.value) {
            current.left = insertRecursive(current.left, value);
        } else if (value > current.value) {
            current.right = insertRecursive(current.right, value);
        }
        // 相等时不插入（避免重复）
        return current;
    }

    /** 删除节点 */
    private Node deleteRecursive(Node current, int value) {
        if (current == null) return null;

        if (value < current.value) {
            current.left = deleteRecursive(current.left, value);
        } else if (value > current.value) {
            current.right = deleteRecursive(current.right, value);
        } else {
            // 找到要删除的节点
            if (current.left == null && current.right == null) {
                return null; // 叶子节点
            } else if (current.left == null) {
                return current.right; // 只有右子树
            } else if (current.right == null) {
                return current.left; // 只有左子树
            } else {
                // 两个子节点 → 找右子树最小值替代
                int smallestValue = findSmallestValue(current.right);
                current.value = smallestValue;
                current.right = deleteRecursive(current.right, smallestValue);
            }
        }
        return current;
    }

    /** 查找 */
    private boolean searchRecursive(Node current, int value) {
        if (current == null) return false;
        if (value == current.value) return true;

        return value < current.value
                ? searchRecursive(current.left, value)
                : searchRecursive(current.right, value);
    }

    /** 找子树中最小值（用于删除节点时替代） */
    private int findSmallestValue(Node root) {
        return root.left == null ? root.value : findSmallestValue(root.left);
    }

    // ================== 其他方法 ==================

    /** 中序遍历（从小到大） */
    public void traverseInOrder() {
        traverseInOrder(root);
        System.out.println();
    }

    private void traverseInOrder(Node node) {
        if (node != null) {
            traverseInOrder(node.left);
            System.out.print(node.value + " ");
            traverseInOrder(node.right);
        }
    }

    /** 获取根节点（用于可视化） */
    public Node getRoot() {
        return root;
    }
}

