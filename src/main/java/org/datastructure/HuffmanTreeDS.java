package org.datastructure;

import java.util.PriorityQueue;

/**
 * 哈夫曼树实现
 */
public class HuffmanTreeDS implements DataStructure {

    // ================== 节点类 ==================
    static class Node implements Comparable<Node> {
        int value;      // 权重（频率）
        char symbol;    // 符号（可选：字符）
        Node left;
        Node right;

        Node(int value, char symbol) {
            this.value = value;
            this.symbol = symbol;
        }

        Node(int value, Node left, Node right) {
            this.value = value;
            this.left = left;
            this.right = right;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.value, other.value);
        }
    }

    private Node root; // 根节点

    public HuffmanTreeDS() {
        root = null;
    }

    // ================== DataStructure接口实现 ==================

    @Override
    public void insert(int value) {
        // 对哈夫曼树来说，insert 没太大意义，这里简单做添加叶子
        root = new Node(value, '\0');
    }

    @Override
    public void delete(int value) {
        // 哈夫曼树是固定的编码树，一般不支持删除
        throw new UnsupportedOperationException("HuffmanTree 不支持删除");
    }

    @Override
    public boolean search(int value) {
        return searchRecursive(root, value);
    }

    private boolean searchRecursive(Node current, int value) {
        if (current == null) return false;
        if (current.value == value) return true;
        return searchRecursive(current.left, value) || searchRecursive(current.right, value);
    }

    // ================== 哈夫曼树核心构建方法 ==================

    /**
     * 根据字符和权重数组构建哈夫曼树
     */
    public void build(char[] symbols, int[] weights) {
        PriorityQueue<Node> pq = new PriorityQueue<>();

        // 1. 把所有符号作为叶子节点放入优先队列
        for (int i = 0; i < symbols.length; i++) {
            pq.add(new Node(weights[i], symbols[i]));
        }

        // 2. 逐步合并
        while (pq.size() > 1) {
            Node left = pq.poll();   // 权重最小
            Node right = pq.poll();  // 第二小
            Node parent = new Node(left.value + right.value, left, right);
            pq.add(parent);
        }

        // 3. 队列最后一个元素就是根
        root = pq.poll();
    }

    // ================== 打印编码 ==================

    public void printCodes() {
        printCodesRecursive(root, "");
    }

    private void printCodesRecursive(Node node, String code) {
        if (node == null) return;

        // 叶子节点
        if (node.left == null && node.right == null) {
            System.out.println(node.symbol + ": " + code);
        }

        printCodesRecursive(node.left, code + "0");
        printCodesRecursive(node.right, code + "1");
    }

    // ================== 获取根节点（可视化用） ==================
    public Node getRoot() {
        return root;
    }
}
