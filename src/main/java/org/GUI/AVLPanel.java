package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AVL树面板
 * 修改说明：增加了批量添加功能
 */
public class AVLPanel extends JPanel {
    private AVLNode root;
    private JTextField valueField;
    private JTextArea logArea;
    private AVLNode highlightedNode;
    private List<AVLNode> operationPath;

    public AVLPanel() {
        initializePanel();
    }

    public static class AVLTreeState implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Integer> nodeValues;
        public AVLTreeState(List<Integer> values) {
            this.nodeValues = new ArrayList<>(values);
        }
    }

    public AVLTreeState getCurrentState() {
        List<Integer> values = new ArrayList<>();
        inorderTraversalValues(root, values);
        return new AVLTreeState(values);
    }

    private void inorderTraversalValues(AVLNode node, List<Integer> values) {
        if (node == null) return;
        inorderTraversalValues(node.left, values);
        values.add(node.value);
        inorderTraversalValues(node.right, values);
    }

    public void restoreFromState(AVLTreeState state) {
        if (state == null) return;
        root = null;
        for (Integer value : state.nodeValues) {
            root = insert(root, value);
        }
        repaint();
        log("从保存状态恢复AVL树，节点数: " + state.nodeValues.size());
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

        JButton insertButton = new JButton("插入节点");
        JButton batchAddButton = new JButton("批量添加"); // 新增按钮
        JButton deleteButton = new JButton("删除节点");
        JButton searchButton = new JButton("查找节点");
        JButton clearButton = new JButton("清空树");

        insertButton.addActionListener(e -> insertNode());
        batchAddButton.addActionListener(e -> batchAddNodes()); // 绑定事件
        deleteButton.addActionListener(e -> deleteNode());
        searchButton.addActionListener(e -> searchNode());
        clearButton.addActionListener(e -> clearTree());

        panel.add(new JLabel("值(批量用,隔开):"));
        panel.add(valueField);
        panel.add(insertButton);
        panel.add(batchAddButton);
        panel.add(deleteButton);
        panel.add(searchButton);
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
                root = insert(root, value);
                successCount++;
            } catch (NumberFormatException ex) {
                log("警告: '" + part + "' 不是有效的整数，已跳过");
            }
        }

        valueField.setText("");
        repaint();
        log("批量添加完成: 成功添加 " + successCount + " 个节点");
    }

    private void insertNode() {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            root = insert(root, value);
            valueField.setText("");
            repaint();
            log("插入节点: " + value);
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        } catch (Exception ex) {
            log("错误: " + ex.getMessage());
        }
    }

    private void deleteNode() {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (search(root, value)) {
                root = delete(root, value);
                log("删除节点: " + value);
            } else {
                log("节点 " + value + " 不存在");
            }
            valueField.setText("");
            repaint();
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        }
    }

    private void searchNode() {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            boolean found = search(root, value);
            if (found) {
                log("找到节点: " + value);
                highlightSearchPath(root, value);
            } else {
                log("未找到节点: " + value);
            }
            valueField.setText("");
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        }
    }

    private void clearTree() {
        root = null;
        highlightedNode = null;
        operationPath = null;
        repaint();
        log("清空AVL树");
    }

    private AVLNode insert(AVLNode node, int value) {
        if (node == null) return new AVLNode(value);
        if (value < node.value) node.left = insert(node.left, value);
        else if (value > node.value) node.right = insert(node.right, value);
        else return node;

        node.height = 1 + Math.max(height(node.left), height(node.right));
        int balance = getBalance(node);

        if (balance > 1 && value < node.left.value) return rightRotate(node);
        if (balance < -1 && value > node.right.value) return leftRotate(node);
        if (balance > 1 && value > node.left.value) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        if (balance < -1 && value < node.right.value) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }
        return node;
    }

    private AVLNode delete(AVLNode node, int value) {
        if (node == null) return null;
        if (value < node.value) node.left = delete(node.left, value);
        else if (value > node.value) node.right = delete(node.right, value);
        else {
            if (node.left == null || node.right == null) {
                AVLNode temp = (node.left != null) ? node.left : node.right;
                if (temp == null) { temp = node; node = null; }
                else node = temp;
            } else {
                AVLNode temp = minValueNode(node.right);
                node.value = temp.value;
                node.right = delete(node.right, temp.value);
            }
        }
        if (node == null) return null;

        node.height = 1 + Math.max(height(node.left), height(node.right));
        int balance = getBalance(node);

        if (balance > 1 && getBalance(node.left) >= 0) return rightRotate(node);
        if (balance > 1 && getBalance(node.left) < 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        if (balance < -1 && getBalance(node.right) <= 0) return leftRotate(node);
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

    private void highlightSearchPath(AVLNode node, int value) {
        operationPath = new ArrayList<>();
        recordSearchPath(node, value, operationPath);
        highlightedNode = operationPath.isEmpty() ? null : operationPath.get(operationPath.size() - 1);
        repaint();
    }

    private boolean recordSearchPath(AVLNode node, int value, List<AVLNode> path) {
        if (node == null) return false;
        path.add(node);
        if (node.value == value) return true;
        if (value < node.value) return recordSearchPath(node.left, value, path);
        else return recordSearchPath(node.right, value, path);
    }

    private int height(AVLNode node) { return node == null ? 0 : node.height; }
    private int getBalance(AVLNode node) { return node == null ? 0 : height(node.left) - height(node.right); }

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
        while (current.left != null) current = current.left;
        return current;
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
        g2d.drawString("AVL平衡二叉树", 20, 30);

        if (root != null) drawTree(g2d, root, getWidth() / 2, 80, getWidth() / 4);
        else {
            g2d.setColor(Color.RED);
            g2d.drawString("树为空，请插入节点", getWidth() / 2 - 80, getHeight() / 2);
        }
    }

    private void drawTree(Graphics2D g2d, AVLNode node, int x, int y, int hGap) {
        int radius = 20;
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + 60;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawTree(g2d, node.left, childX, childY, hGap / 2);
        }
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + 60;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawTree(g2d, node.right, childX, childY, hGap / 2);
        }

        Color nodeColor = (highlightedNode == node) ? Color.YELLOW :
                (node.left == null && node.right == null) ? Color.GREEN : Color.CYAN;
        g2d.setColor(nodeColor);
        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        String nodeText = node.value + " (h=" + node.height + ")";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(Color.BLACK);
        g2d.drawString(nodeText, x - fm.stringWidth(nodeText) / 2, y + 5);

        int balance = getBalance(node);
        g2d.setColor(balance == 0 ? Color.BLACK : (Math.abs(balance) > 1 ? Color.RED : Color.BLUE));
        g2d.setFont(new Font("宋体", Font.PLAIN, 10));
        g2d.drawString("平衡=" + balance, x - 15, y - radius - 5);
    }

    static class AVLNode implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;
        int height;
        AVLNode left;
        AVLNode right;
        AVLNode(int value) { this.value = value; this.height = 1; }
    }
}