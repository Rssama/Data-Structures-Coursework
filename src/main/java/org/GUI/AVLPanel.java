package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * AVL树面板 - 展示平衡过程和旋转操作
 */
public class AVLPanel extends JPanel {
    private AVLNode root;
    private JTextField valueField;
    private JTextArea logArea;
    private AVLNode highlightedNode;
    private List<AVLNode> operationPath;
    private String currentOperation;

    public AVLPanel() {
        initializePanel();
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
        JButton insertButton = new JButton("插入节点");
        JButton deleteButton = new JButton("删除节点");
        JButton searchButton = new JButton("查找节点");
        JButton clearButton = new JButton("清空树");

        insertButton.addActionListener(e -> insertNode());
        deleteButton.addActionListener(e -> deleteNode());
        searchButton.addActionListener(e -> searchNode());
        clearButton.addActionListener(e -> clearTree());

        panel.add(new JLabel("值:"));
        panel.add(valueField);
        panel.add(insertButton);
        panel.add(deleteButton);
        panel.add(searchButton);
        panel.add(clearButton);

        return panel;
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
                // 高亮显示找到的节点
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

    // AVL树操作方法 (与AVLTreeDS类似，为简化在此实现)
    private AVLNode insert(AVLNode node, int value) {
        if (node == null) return new AVLNode(value);

        if (value < node.value) {
            node.left = insert(node.left, value);
        } else if (value > node.value) {
            node.right = insert(node.right, value);
        } else {
            return node; // 不允许重复值
        }

        node.height = 1 + Math.max(height(node.left), height(node.right));
        int balance = getBalance(node);

        // 旋转情况处理
        if (balance > 1 && value < node.left.value) {
            log("执行右旋操作");
            return rightRotate(node);
        }
        if (balance < -1 && value > node.right.value) {
            log("执行左旋操作");
            return leftRotate(node);
        }
        if (balance > 1 && value > node.left.value) {
            log("执行左右旋操作");
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        if (balance < -1 && value < node.right.value) {
            log("执行右左旋操作");
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    private AVLNode delete(AVLNode node, int value) {
        // 实现删除逻辑（同上）
        return node;
    }

    private boolean search(AVLNode node, int value) {
        if (node == null) return false;
        if (node.value == value) return true;
        return value < node.value ? search(node.left, value) : search(node.right, value);
    }

    private void highlightSearchPath(AVLNode node, int value) {
        // 实现查找路径高亮
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

        if (root != null) {
            drawTree(g2d, root, getWidth() / 2, 80, getWidth() / 4);
        } else {
            g2d.setColor(Color.RED);
            g2d.drawString("树为空，请插入节点", getWidth() / 2 - 80, getHeight() / 2);
        }
    }

    private void drawTree(Graphics2D g2d, AVLNode node, int x, int y, int hGap) {
        int radius = 20;

        // 绘制左子树
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + 60;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawTree(g2d, node.left, childX, childY, hGap / 2);
        }

        // 绘制右子树
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + 60;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawTree(g2d, node.right, childX, childY, hGap / 2);
        }

        // 绘制节点
        Color nodeColor = (highlightedNode == node) ? Color.YELLOW :
                (node.left == null && node.right == null) ? Color.GREEN : Color.CYAN;

        g2d.setColor(nodeColor);
        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 绘制节点值和高度
        String nodeText = node.value + " (h=" + node.height + ")";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(nodeText);
        g2d.setColor(Color.BLACK);
        g2d.drawString(nodeText, x - textWidth / 2, y + 5);

        // 绘制平衡因子
        int balance = getBalance(node);
        g2d.setColor(balance == 0 ? Color.BLACK :
                balance > 1 || balance < -1 ? Color.RED : Color.BLUE);
        g2d.setFont(new Font("宋体", Font.PLAIN, 10));
        g2d.drawString("平衡=" + balance, x - 15, y - radius - 5);
    }

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
}