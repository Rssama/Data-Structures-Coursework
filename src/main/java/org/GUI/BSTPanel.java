package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 二叉搜索树面板
 * 修改说明：
 * 1. 增加了批量添加功能
 * 2. 修复了保存逻辑：将保存时的遍历方式由“中序”改为“先序”，
 * 确保载入时能恢复树的结构，避免因读取排序后的数据导致树退化成链表。
 */
public class BSTPanel extends JPanel {
    private BSTNode root;
    private JTextField valueField;
    private JTextArea logArea;
    private BSTNode highlightedNode;
    private List<BSTNode> searchPath;
    private int currentSearchIndex;
    private Timer searchTimer;
    private boolean isSearching;
    private String currentOperation;

    private final Color DEFAULT_NODE_COLOR = new Color(200, 220, 255);
    private final Color CURRENT_NODE_COLOR = Color.YELLOW;
    private final Color VISITED_NODE_COLOR = new Color(255, 165, 0);
    private final Color FOUND_NODE_COLOR = new Color(50, 205, 50);
    private final Color NOT_FOUND_COLOR = new Color(220, 20, 60);
    private final Color LEAF_NODE_COLOR = new Color(144, 238, 144);
    private final Color INTERNAL_NODE_COLOR = new Color(176, 224, 230);
    private final Color ROOT_NODE_COLOR = new Color(173, 216, 230);

    public BSTPanel() {
        initializePanel();
    }

    public static class BSTState implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Integer> nodeValues;

        public BSTState(List<Integer> values) {
            this.nodeValues = new ArrayList<>(values);
        }
    }

    // ================= 修改开始：保存逻辑改为先序遍历 =================

    /**
     * 获取当前状态
     * 修改：使用先序遍历保存，这样恢复时能保留树的结构
     */
    public BSTState getCurrentState() {
        List<Integer> values = new ArrayList<>();
        // 原来是 inorderTraversalValues(root, values);
        // 现在改为先序遍历
        preorderTraversalValues(root, values);
        return new BSTState(values);
    }

    /**
     * 先序遍历获取值（根 -> 左 -> 右）
     * 用于保存状态，保证加载时根节点先被插入
     */
    private void preorderTraversalValues(BSTNode node, List<Integer> values) {
        if (node == null) return;
        values.add(node.value);      // 先保存根
        preorderTraversalValues(node.left, values);
        preorderTraversalValues(node.right, values);
    }

    // 保留中序遍历用于"转为链表"等功能
    private void inorderTraversalValues(BSTNode node, List<Integer> values) {
        if (node == null) return;
        inorderTraversalValues(node.left, values);
        values.add(node.value);
        inorderTraversalValues(node.right, values);
    }

    // ================= 修改结束 =================

    public void restoreFromState(BSTState state) {
        if (state == null || state.nodeValues == null || state.nodeValues.isEmpty()) {
            root = null;
            resetSearch();
            repaint();
            return;
        }
        root = null;
        // 依然是依次插入，但因为输入源变成了先序序列，所以树结构会被还原
        for (Integer value : state.nodeValues) {
            root = insertBST(root, value);
        }
        resetSearch();
        repaint();
        log("从保存状态恢复二叉搜索树，节点数: " + state.nodeValues.size());
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

        valueField = new JTextField(20);

        JButton addButton = new JButton("添加节点");
        JButton batchAddButton = new JButton("批量添加");
        JButton searchButton = new JButton("动画查找");
        JButton deleteButton = new JButton("删除节点");
        JButton clearButton = new JButton("清空树");
        JButton traverseButton = new JButton("中序遍历");
        JButton toLinkedListButton = new JButton("转为链表");

        addButton.addActionListener(this::addNode);
        batchAddButton.addActionListener(e -> batchAddNodes());
        searchButton.addActionListener(e -> startAnimatedSearch());
        deleteButton.addActionListener(e -> deleteNode());
        clearButton.addActionListener(e -> clearTree());
        traverseButton.addActionListener(e -> startTraversal());
        toLinkedListButton.addActionListener(e -> convertToLinkedList());

        panel.add(new JLabel("值(批量用,隔开):"));
        panel.add(valueField);
        panel.add(addButton);
        panel.add(batchAddButton);
        panel.add(searchButton);
        panel.add(deleteButton);
        panel.add(traverseButton);
        panel.add(clearButton);
        panel.add(toLinkedListButton);

        return panel;
    }

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
                if (value < -9999 || value > 9999) {
                    log("警告: 数值 " + value + " 超出范围，已跳过");
                    continue;
                }
                root = insertBST(root, value);
                successCount++;
            } catch (NumberFormatException ex) {
                log("警告: '" + part + "' 不是有效的整数，已跳过");
            }
        }

        valueField.setText("");
        resetSearch();
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
            root = insertBST(root, value);
            valueField.setText("");
            resetSearch();
            repaint();
            log("添加节点: " + value);
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
        }
    }

    private void startAnimatedSearch() {
        if (isSearching) {
            log("正在执行查找动画，请等待完成");
            return;
        }
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            searchPath = new ArrayList<>();
            boolean found = recordSearchPath(root, value, searchPath);

            if (searchPath.isEmpty()) {
                log("树为空，无法查找");
                return;
            }

            currentSearchIndex = 0;
            isSearching = true;
            currentOperation = "search";

            searchTimer = new Timer(800, e -> {
                if (currentSearchIndex < searchPath.size()) {
                    highlightedNode = searchPath.get(currentSearchIndex);
                    repaint();
                    currentSearchIndex++;
                } else {
                    searchTimer.stop();
                    isSearching = false;
                    BSTNode lastNode = searchPath.get(searchPath.size() - 1);
                    if (lastNode.value == value) {
                        log("✓ 查找成功! 找到节点: " + value);
                        highlightedNode = lastNode;
                    } else {
                        log("✗ 查找失败! 未找到节点: " + value);
                        highlightedNode = null;
                    }
                    repaint();
                }
            });
            searchTimer.start();
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        }
    }

    private void startTraversal() {
        if (isSearching) return;
        if (root == null) {
            log("树为空，无法遍历");
            return;
        }
        searchPath = new ArrayList<>();
        inorderTraversal(root, searchPath);

        currentSearchIndex = 0;
        isSearching = true;
        currentOperation = "traversal";
        log("开始中序遍历二叉搜索树");

        searchTimer = new Timer(500, e -> {
            if (currentSearchIndex < searchPath.size()) {
                highlightedNode = searchPath.get(currentSearchIndex);
                currentSearchIndex++;
                repaint();
            } else {
                searchTimer.stop();
                isSearching = false;
                log("✓ 中序遍历完成");
                highlightedNode = null;
                repaint();
            }
        });
        searchTimer.start();
    }

    private void inorderTraversal(BSTNode node, List<BSTNode> path) {
        if (node == null) return;
        inorderTraversal(node.left, path);
        path.add(node);
        inorderTraversal(node.right, path);
    }

    private boolean recordSearchPath(BSTNode node, int value, List<BSTNode> path) {
        if (node == null) return false;
        path.add(node);
        if (node.value == value) return true;
        if (value < node.value) return recordSearchPath(node.left, value, path);
        else return recordSearchPath(node.right, value, path);
    }

    private void deleteNode() {
        if (isSearching) return;
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            boolean existed = searchBST(root, value);
            root = deleteBST(root, value);
            valueField.setText("");
            resetSearch();
            repaint();
            if (existed) log("删除节点: " + value);
            else log("节点 " + value + " 不存在");
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        }
    }

    private BSTNode insertBST(BSTNode node, int value) {
        if (node == null) return new BSTNode(value);
        if (value < node.value) node.left = insertBST(node.left, value);
        else if (value > node.value) node.right = insertBST(node.right, value);
        return node;
    }

    private boolean searchBST(BSTNode node, int value) {
        if (node == null) return false;
        if (node.value == value) return true;
        return value < node.value ? searchBST(node.left, value) : searchBST(node.right, value);
    }

    private BSTNode deleteBST(BSTNode node, int value) {
        if (node == null) return null;
        if (value < node.value) node.left = deleteBST(node.left, value);
        else if (value > node.value) node.right = deleteBST(node.right, value);
        else {
            if (node.left == null) return node.right;
            else if (node.right == null) return node.left;
            BSTNode minNode = findMin(node.right);
            node.value = minNode.value;
            node.right = deleteBST(node.right, minNode.value);
        }
        return node;
    }

    private BSTNode findMin(BSTNode node) {
        while (node.left != null) node = node.left;
        return node;
    }

    private void convertToLinkedList() {
        if (root == null) {
            log("BST为空，无法转换");
            return;
        }
        try {
            List<Integer> values = new ArrayList<>();
            // 转链表依然使用中序遍历，因为链表需要有序
            inorderTraversalValues(root, values);

            LinkedListPanel.LinkedListState linkedListState = new LinkedListPanel.LinkedListState(values);
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (topFrame instanceof DataStructureVisualizer) {
                DataStructureVisualizer mainFrame = (DataStructureVisualizer) topFrame;
                LinkedListPanel linkedListPanel = (LinkedListPanel) mainFrame.getPanel("LinkedList");
                if (linkedListPanel != null) {
                    mainFrame.switchToPanel("LinkedList");
                    SwingUtilities.invokeLater(() -> {
                        linkedListPanel.restoreFromState(linkedListState);
                        log("✓ BST已转换为链表");
                    });
                }
            }
        } catch (Exception ex) {
            log("转换失败: " + ex.getMessage());
        }
    }

    private void resetSearch() {
        if (searchTimer != null && searchTimer.isRunning()) searchTimer.stop();
        highlightedNode = null;
        searchPath = null;
        currentSearchIndex = 0;
        isSearching = false;
        currentOperation = null;
    }

    private void clearTree() {
        if (isSearching) return;
        root = null;
        resetSearch();
        repaint();
        log("清空二叉搜索树");
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
        g2d.drawString("二叉搜索树", 20, 30);

        if (root != null) drawTree(g2d, root, getWidth() / 2, 100, getWidth() / 4);
        else {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("宋体", Font.BOLD, 16));
            g2d.drawString("树为空，请添加节点", getWidth() / 2 - 80, getHeight() / 2);
        }
        drawSearchInfo(g2d);
        drawLegend(g2d);
    }

    private void drawSearchInfo(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.PLAIN, 14));
        if (isSearching) {
            if ("search".equals(currentOperation)) g2d.drawString("🔍 正在查找中...", 20, 60);
            else if ("traversal".equals(currentOperation)) g2d.drawString("🔄 正在遍历中...", 20, 60);
        }
    }

    private void drawTree(Graphics2D g2d, BSTNode node, int x, int y, int hGap) {
        int radius = 25;
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawTree(g2d, node.left, childX, childY, hGap / 2);
        }
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawTree(g2d, node.right, childX, childY, hGap / 2);
        }

        Color nodeColor = getNodeColor(node);
        g2d.setColor(nodeColor);
        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        String valueStr = String.valueOf(node.value);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(Color.BLACK);
        g2d.drawString(valueStr, x - fm.stringWidth(valueStr) / 2, y + fm.getHeight() / 4);
    }

    private Color getNodeColor(BSTNode node) {
        if (isSearching && currentSearchIndex > 0 && currentSearchIndex <= searchPath.size()) {
            if (node == searchPath.get(currentSearchIndex - 1)) return CURRENT_NODE_COLOR;
        }
        if (searchPath != null && searchPath.contains(node)) {
            if (!isSearching) return VISITED_NODE_COLOR;
            if (searchPath.indexOf(node) < currentSearchIndex) return VISITED_NODE_COLOR;
        }
        if (node.left == null && node.right == null) return LEAF_NODE_COLOR;
        if (node == root) return ROOT_NODE_COLOR;
        return INTERNAL_NODE_COLOR;
    }

    private void drawLegend(Graphics2D g2d) {
        int startX = getWidth() - 150;
        int startY = 80;
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("宋体", Font.BOLD, 12));
        g2d.drawString("图例:", startX, startY);
        startY += 20;
        drawLegendItem(g2d, CURRENT_NODE_COLOR, "当前节点", startX, startY); startY += 20;
        drawLegendItem(g2d, VISITED_NODE_COLOR, "已访问", startX, startY); startY += 20;
        drawLegendItem(g2d, LEAF_NODE_COLOR, "叶子节点", startX, startY);
    }

    private void drawLegendItem(Graphics2D g2d, Color color, String text, int x, int y) {
        g2d.setColor(color);
        g2d.fillRect(x, y, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, 15, 15);
        g2d.drawString(text, x + 20, y + 12);
    }

    private static class BSTNode implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;
        BSTNode left;
        BSTNode right;
        BSTNode(int value) { this.value = value; }
    }
}