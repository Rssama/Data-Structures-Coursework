package org.datastructure;

/**
 * AVL平衡二叉树实现
 */
public class AVLTreeDS implements DataStructure {

    static class AVLNode {
        int value;
        int height;
        AVLNode left;
        AVLNode right;

        AVLNode(int value) {
            this.value = value;
            this.height = 1;
        }
    }

    private AVLNode root;

    public AVLTreeDS() {
        root = null;
    }

    @Override
    public void insert(int value) {
        root = insert(root, value);
    }

    @Override
    public void delete(int value) {
        root = delete(root, value);
    }

    @Override
    public boolean search(int value) {
        return search(root, value);
    }

    private AVLNode insert(AVLNode node, int value) {
        if (node == null) return new AVLNode(value);

        if (value < node.value) {
            node.left = insert(node.left, value);
        } else if (value > node.value) {
            node.right = insert(node.right, value);
        } else {
            return node; // 不允许重复值
        }

        // 更新高度
        node.height = 1 + Math.max(height(node.left), height(node.right));

        // 获取平衡因子
        int balance = getBalance(node);

        // 左左情况
        if (balance > 1 && value < node.left.value) {
            return rightRotate(node);
        }

        // 右右情况
        if (balance < -1 && value > node.right.value) {
            return leftRotate(node);
        }

        // 左右情况
        if (balance > 1 && value > node.left.value) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        // 右左情况
        if (balance < -1 && value < node.right.value) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    private AVLNode delete(AVLNode node, int value) {
        if (node == null) return null;

        if (value < node.value) {
            node.left = delete(node.left, value);
        } else if (value > node.value) {
            node.right = delete(node.right, value);
        } else {
            if (node.left == null || node.right == null) {
                AVLNode temp = (node.left != null) ? node.left : node.right;
                if (temp == null) {
                    temp = node;
                    node = null;
                } else {
                    node = temp;
                }
            } else {
                AVLNode temp = minValueNode(node.right);
                node.value = temp.value;
                node.right = delete(node.right, temp.value);
            }
        }

        if (node == null) return null;

        // 更新高度
        node.height = 1 + Math.max(height(node.left), height(node.right));

        // 获取平衡因子
        int balance = getBalance(node);

        // 左左情况
        if (balance > 1 && getBalance(node.left) >= 0) {
            return rightRotate(node);
        }

        // 左右情况
        if (balance > 1 && getBalance(node.left) < 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        // 右右情况
        if (balance < -1 && getBalance(node.right) <= 0) {
            return leftRotate(node);
        }

        // 右左情况
        if (balance < -1 && getBalance(node.right) > 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    private boolean search(AVLNode node, int value) {
        if (node == null) return false;
        if (node.value == value) return true;
        return value < node.value ? search(node.left, value) : search(node.right, value);
    }

    private int height(AVLNode node) {
        return node == null ? 0 : node.height;
    }

    private int getBalance(AVLNode node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    private AVLNode rightRotate(AVLNode y) {
        AVLNode x = y.left;
        AVLNode T2 = x.right;

        x.right = y;
        y.left = T2;

        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;

        return x;
    }

    private AVLNode leftRotate(AVLNode x) {
        AVLNode y = x.right;
        AVLNode T2 = y.left;

        y.left = x;
        x.right = T2;

        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;

        return y;
    }

    private AVLNode minValueNode(AVLNode node) {
        AVLNode current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }

    public AVLNode getRoot() {
        return root;
    }
}