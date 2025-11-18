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
 * 二叉树构建面板 - 修复根节点变色问题
 * 展示二叉树的构建过程和遍历动画
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

    // 颜色定义
    private final Color DEFAULT_NODE_COLOR = new Color(200, 220, 255); // 默认节点颜色
    private final Color CURRENT_NODE_COLOR = Color.YELLOW; // 当前访问节点
    private final Color VISITED_NODE_COLOR = new Color(255, 165, 0); // 已访问节点
    private final Color LEAF_NODE_COLOR = new Color(144, 238, 144); // 叶子节点
    private final Color ROOT_NODE_COLOR = new Color(173, 216, 230); // 根节点
    private final Color INTERNAL_NODE_COLOR = new Color(176, 224, 230); // 内部节点

    public BinaryTreePanel() {
        initializePanel();
    }

    // 序列化状态类
    public static class BinaryTreeState implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Integer> nodeValues; // 层序遍历的节点值

        public BinaryTreeState(List<Integer> values) {
            this.nodeValues = new ArrayList<>(values);
        }
    }

    // 获取当前状态（层序遍历）
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

    // 从状态恢复（层序构建）
    public void restoreFromState(BinaryTreeState state) {
        if (state == null || state.nodeValues.isEmpty()) {
            root = null;
            repaint();
            return;
        }

        // 使用层序构建恢复树
        List<TreeNode> nodes = new ArrayList<>();
        for (Integer value : state.nodeValues) {
            nodes.add(new TreeNode(value));
        }

        // 构建树结构
        for (int i = 0; i < nodes.size(); i++) {
            TreeNode node = nodes.get(i);
            int leftIndex = 2 * i + 1;
            int rightIndex = 2 * i + 2;

            if (leftIndex < nodes.size()) {
                node.left = nodes.get(leftIndex);
            }
            if (rightIndex < nodes.size()) {
                node.right = nodes.get(rightIndex);
            }
        }

        root = nodes.get(0);
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

        valueField = new JTextField(10);

        JButton addButton = new JButton("添加节点");
        JButton levelOrderButton = new JButton("层序构建");
        JButton preorderButton = new JButton("先序遍历");
        JButton inorderButton = new JButton("中序遍历");
        JButton postorderButton = new JButton("后序遍历");
        JButton clearButton = new JButton("清空树");

        addButton.addActionListener(this::addNode);
        levelOrderButton.addActionListener(e -> buildLevelOrderTree());
        preorderButton.addActionListener(e -> startPreorderTraversal());
        inorderButton.addActionListener(e -> startInorderTraversal());
        postorderButton.addActionListener(e -> startPostorderTraversal());
        clearButton.addActionListener(e -> clearTree());

        panel.add(new JLabel("值:"));
        panel.add(valueField);
        panel.add(addButton);
        panel.add(levelOrderButton);
        panel.add(preorderButton);
        panel.add(inorderButton);
        panel.add(postorderButton);
        panel.add(clearButton);

        return panel;
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
                // 使用层序遍历找到第一个可以插入的位置
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

            // 尝试插入左孩子
            if (current.left == null) {
                current.left = new TreeNode(value);
                return current;
            } else {
                queue.offer(current.left);
            }

            // 尝试插入右孩子
            if (current.right == null) {
                current.right = new TreeNode(value);
                return current;
            } else {
                queue.offer(current.right);
            }
        }
        return null;
    }

    private void buildLevelOrderTree() {
        try {
            String input = valueField.getText().trim();
            if (input.isEmpty()) {
                log("错误: 请输入节点值");
                return;
            }

            String[] parts = input.split(",");
            if (parts.length == 0) {
                log("错误: 请输入节点值，用逗号分隔");
                return;
            }

            clearTree();

            // 构建完全二叉树
            List<TreeNode> nodes = new ArrayList<>();
            for (String part : parts) {
                int value = Integer.parseInt(part.trim());
                if (value < -9999 || value > 9999) {
                    log("警告: 节点值 " + value + " 超出范围，已跳过");
                    continue;
                }
                nodes.add(new TreeNode(value));
            }

            // 构建树结构
            for (int i = 0; i < nodes.size(); i++) {
                TreeNode node = nodes.get(i);
                int leftIndex = 2 * i + 1;
                int rightIndex = 2 * i + 2;

                if (leftIndex < nodes.size()) {
                    node.left = nodes.get(leftIndex);
                }
                if (rightIndex < nodes.size()) {
                    node.right = nodes.get(rightIndex);
                }
            }

            root = nodes.get(0);
            valueField.setText("");
            resetTraversal();
            repaint();
            log("层序构建完成: " + input + " (共 " + nodes.size() + " 个节点)");

        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的数字，用逗号分隔");
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
        }
    }

    private void startPreorderTraversal() {
        if (root == null) {
            log("树为空，无法遍历");
            return;
        }
        startTraversal("preorder");
    }

    private void startInorderTraversal() {
        if (root == null) {
            log("树为空，无法遍历");
            return;
        }
        startTraversal("inorder");
    }

    private void startPostorderTraversal() {
        if (root == null) {
            log("树为空，无法遍历");
            return;
        }
        startTraversal("postorder");
    }

    private void startTraversal(String type) {
        if (isTraversing) {
            log("正在执行遍历动画，请等待完成");
            return;
        }

        traversalPath = new ArrayList<>();
        currentTraversalType = type;

        switch (type) {
            case "preorder":
                preorderTraversal(root, traversalPath);
                log("开始先序遍历");
                break;
            case "inorder":
                inorderTraversal(root, traversalPath);
                log("开始中序遍历");
                break;
            case "postorder":
                postorderTraversal(root, traversalPath);
                log("开始后序遍历");
                break;
        }

        if (traversalPath.isEmpty()) {
            log("遍历路径为空");
            return;
        }

        currentTraversalIndex = 0;
        isTraversing = true;

        traversalTimer = new Timer(1000, e -> {
            if (currentTraversalIndex < traversalPath.size()) {
                TreeNode currentNode = traversalPath.get(currentTraversalIndex);
                String traversalName = getTraversalName(currentTraversalType);
                log(traversalName + " - 步骤 " + (currentTraversalIndex + 1) + ": 访问节点 " + currentNode.value);
                currentTraversalIndex++;
                repaint();
            } else {
                traversalTimer.stop();
                isTraversing = false;
                String traversalName = getTraversalName(currentTraversalType);
                log("✓ " + traversalName + "完成! 共访问 " + traversalPath.size() + " 个节点");
                repaint();
            }
        });

        traversalTimer.start();
    }

    private String getTraversalName(String type) {
        switch (type) {
            case "preorder": return "先序遍历";
            case "inorder": return "中序遍历";
            case "postorder": return "后序遍历";
            default: return "遍历";
        }
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
        if (isTraversing) {
            log("正在执行遍历动画，请等待完成");
            return;
        }

        root = null;
        resetTraversal();
        repaint();
        log("清空二叉树");
    }

    private void resetTraversal() {
        if (traversalTimer != null) {
            traversalTimer.stop();
        }
        traversalPath = null;
        currentTraversalIndex = 0;
        isTraversing = false;
        currentTraversalType = null;
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

        // 绘制标题
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 16));
        g2d.drawString("二叉树构建与遍历 - 修复根节点变色", 20, 30);

        if (root != null) {
            drawBinaryTree(g2d, root, getWidth() / 2, 80, getWidth() / 4);
        } else {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("宋体", Font.BOLD, 16));
            g2d.drawString("树为空，请添加节点", getWidth() / 2 - 100, getHeight() / 2);
        }

        // 绘制遍历状态
        drawTraversalInfo(g2d);
    }

    private void drawBinaryTree(Graphics2D g2d, TreeNode node, int x, int y, int hGap) {
        int radius = 25;

        // 绘制左子树
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);

            // 绘制左子树连接线标签
            g2d.setFont(new Font("宋体", Font.PLAIN, 12));
            g2d.drawString("左子树", (x + childX) / 2 - 20, (y + childY) / 2);

            drawBinaryTree(g2d, node.left, childX, childY, hGap / 2);
        }

        // 绘制右子树
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);

            // 绘制右子树连接线标签
            g2d.setFont(new Font("宋体", Font.PLAIN, 12));
            g2d.drawString("右子树", (x + childX) / 2 - 20, (y + childY) / 2);

            drawBinaryTree(g2d, node.right, childX, childY, hGap / 2);
        }

        // 绘制当前节点 - 修复的颜色逻辑
        Color nodeColor = getNodeColor(node);

        g2d.setColor(nodeColor);
        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 绘制节点值
        String valueStr = String.valueOf(node.value);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(valueStr);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("宋体", Font.BOLD, 14));
        g2d.drawString(valueStr, x - textWidth / 2, y + 5);

        // 如果是遍历路径上的节点，显示访问顺序
        if (traversalPath != null && traversalPath.contains(node)) {
            int order = traversalPath.indexOf(node) + 1;
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("宋体", Font.BOLD, 12));
            g2d.drawString("(" + order + ")", x - 8, y - radius - 5);
        }

        // 显示节点类型
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("宋体", Font.PLAIN, 10));
        if (node.left == null && node.right == null) {
            g2d.drawString("叶子", x - 10, y + radius + 15);
        } else if (node == root) {
            g2d.drawString("根节点", x - 15, y + radius + 15);
        }
    }

    private Color getNodeColor(TreeNode node) {
        // 修复：确保根节点在遍历过程中能够正确变色

        // 1. 首先检查是否是当前正在访问的节点（最高优先级）
        if (isTraversing && currentTraversalIndex > 0 &&
                currentTraversalIndex <= traversalPath.size()) {
            TreeNode currentNode = traversalPath.get(currentTraversalIndex - 1);
            if (node == currentNode) {
                return CURRENT_NODE_COLOR;
            }
        }

        // 2. 检查是否在遍历路径中（已访问过的节点）
        if (traversalPath != null && traversalPath.contains(node)) {
            int index = traversalPath.indexOf(node);

            // 如果遍历已完成，所有节点都显示为已访问
            if (!isTraversing) {
                // 遍历完成后的渐变色效果
                float ratio = (float) index / (traversalPath.size() - 1);
                int red = 255;
                int green = (int) (165 + (90 * ratio)); // 从橙色到更亮的黄色
                int blue = (int) (100 * ratio);
                return new Color(red, green, blue);
            }

            // 遍历过程中，已访问的节点显示渐变色
            if (index < currentTraversalIndex) {
                float ratio = (float) index / (currentTraversalIndex - 1);
                int red = 255;
                int green = (int) (165 + (90 * ratio));
                int blue = (int) (100 * ratio);
                return new Color(red, green, blue);
            }
        }

        // 3. 根据节点类型设置默认颜色（最低优先级）
        if (node.left == null && node.right == null) {
            return LEAF_NODE_COLOR; // 叶子节点
        } else if (node == root) {
            return ROOT_NODE_COLOR; // 根节点
        } else {
            return INTERNAL_NODE_COLOR; // 内部节点
        }
    }

    private void drawTraversalInfo(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.PLAIN, 14));

        if (isTraversing) {
            String traversalName = getTraversalName(currentTraversalType);
            g2d.drawString("正在执行" + traversalName + "... 当前步骤: " + currentTraversalIndex + "/" + traversalPath.size(),
                    20, getHeight() - 50);
        } else if (traversalPath != null && !traversalPath.isEmpty()) {
            String traversalName = getTraversalName(currentTraversalType);
            g2d.drawString(traversalName + "完成，共访问 " + traversalPath.size() + " 个节点", 20, getHeight() - 50);
        }

        // 绘制图例
        drawLegend(g2d);
    }

    private void drawLegend(Graphics2D g2d) {
        int startX = getWidth() - 150;
        int startY = 30;

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("宋体", Font.BOLD, 12));
        g2d.drawString("图例:", startX, startY);

        startY += 20;

        // 当前访问节点
        g2d.setColor(CURRENT_NODE_COLOR);
        g2d.fillRect(startX, startY, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(startX, startY, 15, 15);
        g2d.drawString("当前节点", startX + 20, startY + 12);

        startY += 20;

        // 已访问节点
        g2d.setColor(VISITED_NODE_COLOR);
        g2d.fillRect(startX, startY, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(startX, startY, 15, 15);
        g2d.drawString("已访问", startX + 20, startY + 12);

        startY += 20;

        // 叶子节点
        g2d.setColor(LEAF_NODE_COLOR);
        g2d.fillRect(startX, startY, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(startX, startY, 15, 15);
        g2d.drawString("叶子节点", startX + 20, startY + 12);

        startY += 20;

        // 内部节点
        g2d.setColor(INTERNAL_NODE_COLOR);
        g2d.fillRect(startX, startY, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(startX, startY, 15, 15);
        g2d.drawString("内部节点", startX + 20, startY + 12);

        startY += 20;

        // 根节点
        g2d.setColor(ROOT_NODE_COLOR);
        g2d.fillRect(startX, startY, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(startX, startY, 15, 15);
        g2d.drawString("根节点", startX + 20, startY + 12);
    }

    // 二叉树节点类 - 实现序列化
    private static class TreeNode implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;
        TreeNode left;
        TreeNode right;

        TreeNode(int value) {
            this.value = value;
        }
    }
}