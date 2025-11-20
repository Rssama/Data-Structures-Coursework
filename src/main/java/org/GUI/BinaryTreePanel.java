package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

/**
 * 二叉树构建面板
 * 修改说明：增加了批量添加功能，区别于“层序构建”（后者会清空树）
 * 批量添加是在现有树的基础上继续添加。
 */
public class BinaryTreePanel extends JPanel {
    private TreeNode root;
    private JTextField valueField;
    private JTextArea logArea;
    private List<TreeNode> traversalPath;
    private int currentTraversalIndex;
    private Timer traversalTimer;
    private boolean isTraversing;
    private String currentTraversalType;

    private final Color DEFAULT_NODE_COLOR = new Color(200, 220, 255);
    private final Color CURRENT_NODE_COLOR = Color.YELLOW;
    private final Color VISITED_NODE_COLOR = new Color(255, 165, 0);
    private final Color LEAF_NODE_COLOR = new Color(144, 238, 144);
    private final Color ROOT_NODE_COLOR = new Color(173, 216, 230);
    private final Color INTERNAL_NODE_COLOR = new Color(176, 224, 230);

    public BinaryTreePanel() {
        initializePanel();
    }

    public static class BinaryTreeState implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Integer> nodeValues;
        public BinaryTreeState(List<Integer> values) {
            this.nodeValues = new ArrayList<>(values);
        }
    }

    public BinaryTreeState getCurrentState() {
        List<Integer> values = new ArrayList<>();
        if (root != null) {
            Queue<TreeNode> queue = new LinkedList<>();
            queue.offer(root);
            while (!queue.isEmpty()) {
                TreeNode current = queue.poll();
                values.add(current.value);
                if (current.left != null) queue.offer(current.left);
                if (current.right != null) queue.offer(current.right);
            }
        }
        return new BinaryTreeState(values);
    }

    public void restoreFromState(BinaryTreeState state) {
        if (state == null || state.nodeValues == null || state.nodeValues.isEmpty()) {
            root = null;
            resetTraversal();
            repaint();
            return;
        }
        root = null;
        List<TreeNode> nodes = new ArrayList<>();
        for (Integer value : state.nodeValues) {
            nodes.add(new TreeNode(value));
        }
        for (int i = 0; i < nodes.size(); i++) {
            TreeNode node = nodes.get(i);
            int leftIndex = 2 * i + 1;
            int rightIndex = 2 * i + 2;
            if (leftIndex < nodes.size()) node.left = nodes.get(leftIndex);
            if (rightIndex < nodes.size()) node.right = nodes.get(rightIndex);
        }
        root = nodes.isEmpty() ? null : nodes.get(0);
        resetTraversal();
        repaint();
        log("从保存状态恢复二叉树，节点数: " + state.nodeValues.size());
    }

    private void initializePanel() {
        setLayout(new BorderLayout());
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);
        logArea = new JTextArea(5, 30);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        valueField = new JTextField(20); // 增加宽度

        JButton addButton = new JButton("添加节点");
        JButton batchAddButton = new JButton("批量添加"); // 新增按钮
        JButton levelOrderButton = new JButton("层序构建(重建)"); // 区分名称
        JButton preorderButton = new JButton("先序遍历");
        JButton inorderButton = new JButton("中序遍历");
        JButton postorderButton = new JButton("后序遍历");
        JButton clearButton = new JButton("清空树");

        addButton.addActionListener(this::addNode);
        batchAddButton.addActionListener(e -> batchAddNodes()); // 绑定事件
        levelOrderButton.addActionListener(e -> buildLevelOrderTree());
        preorderButton.addActionListener(e -> startPreorderTraversal());
        inorderButton.addActionListener(e -> startInorderTraversal());
        postorderButton.addActionListener(e -> startPostorderTraversal());
        clearButton.addActionListener(e -> clearTree());

        panel.add(new JLabel("值(批量用,隔开):"));
        panel.add(valueField);
        panel.add(addButton);
        panel.add(batchAddButton);
        panel.add(levelOrderButton);
        panel.add(preorderButton);
        panel.add(inorderButton);
        panel.add(postorderButton);
        panel.add(clearButton);

        return panel;
    }

    // 新增：批量添加方法
    private void batchAddNodes() {
        String input = valueField.getText().trim();
        if (input.isEmpty()) {
            log("错误: 请输入数值");
            return;
        }

        String[] parts = input.split("[,，]");
        int successCount = 0;

        for (String part : parts) {
            try {
                String valStr = part.trim();
                if (valStr.isEmpty()) continue;
                int value = Integer.parseInt(valStr);
                if (root == null) {
                    root = new TreeNode(value);
                } else {
                    insertLevelOrder(value);
                }
                successCount++;
            } catch (NumberFormatException ex) {
                log("警告: '" + part + "' 不是有效的整数，已跳过");
            }
        }

        valueField.setText("");
        resetTraversal();
        repaint();
        log("批量添加完成: 成功添加 " + successCount + " 个节点");
    }

    private void addNode(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (value < -9999 || value > 9999) {
                log("错误: 数值范围应在 -9999 到 9999 之间");
                return;
            }
            if (root == null) {
                root = new TreeNode(value);
                log("创建根节点: " + value);
            } else {
                TreeNode insertedNode = insertLevelOrder(value);
                if (insertedNode != null) {
                    log("添加节点: " + value + " (父节点: " + insertedNode.value + ")");
                }
            }
            valueField.setText("");
            resetTraversal();
            repaint();
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
        }
    }

    private TreeNode insertLevelOrder(int value) {
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current.left == null) {
                current.left = new TreeNode(value);
                return current;
            } else queue.offer(current.left);

            if (current.right == null) {
                current.right = new TreeNode(value);
                return current;
            } else queue.offer(current.right);
        }
        return null;
    }

    private void buildLevelOrderTree() {
        try {
            String input = valueField.getText().trim();
            if (input.isEmpty()) { log("错误: 请输入节点值"); return; }
            String[] parts = input.split("[,，]");
            clearTree();
            List<TreeNode> nodes = new ArrayList<>();
            for (String part : parts) {
                try {
                    int value = Integer.parseInt(part.trim());
                    nodes.add(new TreeNode(value));
                } catch(Exception ignored) {}
            }
            for (int i = 0; i < nodes.size(); i++) {
                TreeNode node = nodes.get(i);
                int left = 2 * i + 1;
                int right = 2 * i + 2;
                if (left < nodes.size()) node.left = nodes.get(left);
                if (right < nodes.size()) node.right = nodes.get(right);
            }
            root = nodes.isEmpty() ? null : nodes.get(0);
            valueField.setText("");
            resetTraversal();
            repaint();
            log("层序构建完成 (重建模式)");
        } catch (Exception ex) { log("系统错误: " + ex.getMessage()); }
    }

    private void startPreorderTraversal() { startTraversal("preorder"); }
    private void startInorderTraversal() { startTraversal("inorder"); }
    private void startPostorderTraversal() { startTraversal("postorder"); }

    private void startTraversal(String type) {
        if (isTraversing || root == null) return;
        traversalPath = new ArrayList<>();
        currentTraversalType = type;
        if (type.equals("preorder")) preorderTraversal(root, traversalPath);
        else if (type.equals("inorder")) inorderTraversal(root, traversalPath);
        else postorderTraversal(root, traversalPath);

        currentTraversalIndex = 0;
        isTraversing = true;
        traversalTimer = new Timer(1000, e -> {
            if (currentTraversalIndex < traversalPath.size()) {
                log(type + " - 访问: " + traversalPath.get(currentTraversalIndex).value);
                currentTraversalIndex++;
                repaint();
            } else {
                traversalTimer.stop();
                isTraversing = false;
                log("遍历完成");
                repaint();
            }
        });
        traversalTimer.start();
    }

    private void preorderTraversal(TreeNode node, List<TreeNode> path) {
        if (node == null) return;
        path.add(node);
        preorderTraversal(node.left, path);
        preorderTraversal(node.right, path);
    }

    private void inorderTraversal(TreeNode node, List<TreeNode> path) {
        if (node == null) return;
        inorderTraversal(node.left, path);
        path.add(node);
        inorderTraversal(node.right, path);
    }

    private void postorderTraversal(TreeNode node, List<TreeNode> path) {
        if (node == null) return;
        postorderTraversal(node.left, path);
        postorderTraversal(node.right, path);
        path.add(node);
    }

    private void clearTree() {
        if (isTraversing) return;
        root = null;
        resetTraversal();
        repaint();
        log("清空二叉树");
    }

    private void resetTraversal() {
        if (traversalTimer != null) traversalTimer.stop();
        traversalPath = null;
        currentTraversalIndex = 0;
        isTraversing = false;
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 16));
        g2d.drawString("二叉树构建与遍历", 20, 30);
        if (root != null) drawBinaryTree(g2d, root, getWidth() / 2, 80, getWidth() / 4);
        else {
            g2d.setColor(Color.RED);
            g2d.drawString("树为空，请添加节点", getWidth() / 2 - 100, getHeight() / 2);
        }
    }

    private void drawBinaryTree(Graphics2D g2d, TreeNode node, int x, int y, int hGap) {
        int radius = 25;
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawBinaryTree(g2d, node.left, childX, childY, hGap / 2);
        }
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawBinaryTree(g2d, node.right, childX, childY, hGap / 2);
        }
        Color nodeColor = getNodeColor(node);
        g2d.setColor(nodeColor);
        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        g2d.drawString(String.valueOf(node.value), x - 5, y + 5);
    }

    private Color getNodeColor(TreeNode node) {
        if (isTraversing && currentTraversalIndex > 0 && node == traversalPath.get(currentTraversalIndex - 1))
            return CURRENT_NODE_COLOR;
        if (traversalPath != null && traversalPath.contains(node)) {
            if (!isTraversing) return VISITED_NODE_COLOR;
            if (traversalPath.indexOf(node) < currentTraversalIndex) return VISITED_NODE_COLOR;
        }
        return INTERNAL_NODE_COLOR;
    }

    private static class TreeNode implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;
        TreeNode left;
        TreeNode right;
        TreeNode(int value) { this.value = value; }
    }
}