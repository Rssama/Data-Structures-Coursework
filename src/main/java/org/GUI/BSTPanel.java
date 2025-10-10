package org.GUI;




import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class BSTPanel extends JPanel {
    private BSTNode root;
    private JTextField valueField;
    private JTextArea logArea;
    private BSTNode highlightedNode;

    public BSTPanel() {
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

        JButton addButton = new JButton("添加节点");
        JButton searchButton = new JButton("查找节点");
        JButton deleteButton = new JButton("删除节点");
        JButton clearButton = new JButton("清空树");

        addButton.addActionListener(this::addNode);
        searchButton.addActionListener(e -> searchNode());
        deleteButton.addActionListener(e -> deleteNode());
        clearButton.addActionListener(e -> clearTree());

        panel.add(new JLabel("值:"));
        panel.add(valueField);
        panel.add(addButton);
        panel.add(searchButton);
        panel.add(deleteButton);
        panel.add(clearButton);

        return panel;
    }

    private void addNode(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText());
            root = insertBST(root, value);
            valueField.setText("");
            highlightedNode = null;
            repaint();
            log("添加节点: " + value);
        } catch (NumberFormatException ex) {
            log("请输入有效的数字");
        }
    }

    private void searchNode() {
        try {
            int value = Integer.parseInt(valueField.getText());
            BSTNode found = searchBST(root, value);
            highlightedNode = found;
            repaint();
            if (found != null) {
                log("找到节点: " + value);
            } else {
                log("未找到节点: " + value);
            }
        } catch (NumberFormatException ex) {
            log("请输入有效的数字");
        }
    }

    private void deleteNode() {
        try {
            int value = Integer.parseInt(valueField.getText());
            root = deleteBST(root, value);
            valueField.setText("");
            highlightedNode = null;
            repaint();
            log("删除节点: " + value);
        } catch (NumberFormatException ex) {
            log("请输入有效的数字");
        }
    }

    private BSTNode insertBST(BSTNode node, int value) {
        if (node == null) {
            return new BSTNode(value);
        }

        if (value < node.value) {
            node.left = insertBST(node.left, value);
        } else if (value > node.value) {
            node.right = insertBST(node.right, value);
        }

        return node;
    }

    private BSTNode searchBST(BSTNode node, int value) {
        if (node == null || node.value == value) {
            return node;
        }

        if (value < node.value) {
            return searchBST(node.left, value);
        } else {
            return searchBST(node.right, value);
        }
    }

    private BSTNode deleteBST(BSTNode node, int value) {
        if (node == null) {
            return null;
        }

        if (value < node.value) {
            node.left = deleteBST(node.left, value);
        } else if (value > node.value) {
            node.right = deleteBST(node.right, value);
        } else {
            // 找到要删除的节点
            if (node.left == null) {
                return node.right;
            } else if (node.right == null) {
                return node.left;
            }

            // 有两个子节点的情况，找到右子树的最小值
            BSTNode minNode = findMin(node.right);
            node.value = minNode.value;
            node.right = deleteBST(node.right, minNode.value);
        }
        return node;
    }

    private BSTNode findMin(BSTNode node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private void clearTree() {
        root = null;
        highlightedNode = null;
        repaint();
        log("清空二叉树");
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

        if (root != null) {
            drawTree(g2d, root, getWidth() / 2, 50, getWidth() / 4);
        } else {
            g2d.setColor(Color.RED);
            g2d.drawString("树为空", getWidth() / 2 - 20, getHeight() / 2);
        }
    }

    private void drawTree(Graphics2D g2d, BSTNode node, int x, int y, int hGap) {
        int radius = 20;

        // 绘制左子树
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawTree(g2d, node.left, childX, childY, hGap / 2);
        }

        // 绘制右子树
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawTree(g2d, node.right, childX, childY, hGap / 2);
        }

        // 绘制当前节点
        if (node == highlightedNode) {
            g2d.setColor(Color.YELLOW); // 高亮显示找到的节点
        } else {
            g2d.setColor(Color.CYAN);
        }

        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 绘制节点值
        String valueStr = String.valueOf(node.value);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(valueStr);
        int textHeight = fm.getHeight();
        g2d.drawString(valueStr, x - textWidth / 2, y + textHeight / 4);
    }

    // 二叉搜索树节点类
    private static class BSTNode {
        int value;
        BSTNode left;
        BSTNode right;

        BSTNode(int value) {
            this.value = value;
        }
    }
}