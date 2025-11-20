package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 带动画查找的二叉搜索树面板
 * 修改说明：只保留转为链表功能，删除转为普通二叉树功能
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
    private String currentOperation; // "search" 或 "traversal"

    // 颜色定义
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

    // 序列化状态类
    public static class BSTState implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Integer> nodeValues;

        public BSTState(List<Integer> values) {
            this.nodeValues = new ArrayList<>(values);
        }
    }

    // 获取当前状态（中序遍历）
    public BSTState getCurrentState() {
        List<Integer> values = new ArrayList<>();
        inorderTraversalValues(root, values);
        return new BSTState(values);
    }

    private void inorderTraversalValues(BSTNode node, List<Integer> values) {
        if (node == null) return;
        inorderTraversalValues(node.left, values);
        values.add(node.value);
        inorderTraversalValues(node.right, values);
    }

    // 从状态恢复
    public void restoreFromState(BSTState state) {
        if (state == null || state.nodeValues == null || state.nodeValues.isEmpty()) {
            root = null;
            resetSearch();
            repaint();
            return;
        }

        // 清空当前树
        root = null;

        // 重新插入所有节点来构建BST
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

        valueField = new JTextField(10);

        JButton addButton = new JButton("添加节点");
        JButton searchButton = new JButton("动画查找");
        JButton deleteButton = new JButton("删除节点");
        JButton clearButton = new JButton("清空树");
        JButton traverseButton = new JButton("中序遍历");
        // 修改：只保留转为链表按钮
        JButton toLinkedListButton = new JButton("转为链表");

        addButton.addActionListener(this::addNode);
        searchButton.addActionListener(e -> startAnimatedSearch());
        deleteButton.addActionListener(e -> deleteNode());
        clearButton.addActionListener(e -> clearTree());
        traverseButton.addActionListener(e -> startTraversal());
        toLinkedListButton.addActionListener(e -> convertToLinkedList());

        panel.add(new JLabel("值:"));
        panel.add(valueField);
        panel.add(addButton);
        panel.add(searchButton);
        panel.add(deleteButton);
        panel.add(traverseButton);
        panel.add(clearButton);
        panel.add(toLinkedListButton); // 添加转为链表按钮

        return panel;
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
            if (value < -9999 || value > 9999) {
                log("错误: 数值范围应在 -9999 到 9999 之间");
                return;
            }

            // 记录查找路径
            searchPath = new ArrayList<>();
            boolean found = recordSearchPath(root, value, searchPath);

            if (searchPath.isEmpty()) {
                log("树为空，无法查找");
                return;
            }

            currentSearchIndex = 0;
            isSearching = true;
            currentOperation = "search";

            // 创建定时器，每800毫秒更新一次高亮节点
            searchTimer = new Timer(800, e -> {
                if (currentSearchIndex < searchPath.size()) {
                    highlightedNode = searchPath.get(currentSearchIndex);
                    repaint();

                    BSTNode currentNode = searchPath.get(currentSearchIndex);
                    if (currentSearchIndex > 0) {
                        BSTNode prevNode = searchPath.get(currentSearchIndex - 1);
                        String direction = (currentNode.value < prevNode.value) ? "左子树" : "右子树";
                        log("从节点 " + prevNode.value + " 移动到 " + direction + " 节点 " + currentNode.value);
                    } else {
                        log("开始查找: 从根节点 " + currentNode.value + " 开始");
                    }

                    currentSearchIndex++;
                } else {
                    // 查找完成
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
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
        }
    }

    private void startTraversal() {
        if (isSearching) {
            log("正在执行查找动画，请等待完成");
            return;
        }

        if (root == null) {
            log("树为空，无法遍历");
            return;
        }

        // 记录中序遍历路径
        searchPath = new ArrayList<>();
        inorderTraversal(root, searchPath);

        if (searchPath.isEmpty()) {
            log("遍历路径为空");
            return;
        }

        currentSearchIndex = 0;
        isSearching = true;
        currentOperation = "traversal";

        log("开始中序遍历二叉搜索树");

        // 创建定时器，每1000毫秒更新一次高亮节点
        searchTimer = new Timer(1000, e -> {
            if (currentSearchIndex < searchPath.size()) {
                highlightedNode = searchPath.get(currentSearchIndex);
                BSTNode currentNode = searchPath.get(currentSearchIndex);
                log("中序遍历 - 步骤 " + (currentSearchIndex + 1) + ": 访问节点 " + currentNode.value);
                currentSearchIndex++;
                repaint();
            } else {
                // 遍历完成
                searchTimer.stop();
                isSearching = false;
                log("✓ 中序遍历完成! 共访问 " + searchPath.size() + " 个节点");
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
        if (node == null) {
            return false;
        }

        path.add(node);

        if (node.value == value) {
            return true;
        }

        if (value < node.value) {
            return recordSearchPath(node.left, value, path);
        } else {
            return recordSearchPath(node.right, value, path);
        }
    }

    private void deleteNode() {
        if (isSearching) {
            log("正在执行查找动画，请等待完成");
            return;
        }

        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (value < -9999 || value > 9999) {
                log("错误: 数值范围应在 -9999 到 9999 之间");
                return;
            }

            boolean existed = searchBST(root, value);
            root = deleteBST(root, value);
            valueField.setText("");
            resetSearch();
            repaint();
            if (existed) {
                log("删除节点: " + value);
            } else {
                log("节点 " + value + " 不存在，删除操作无效");
            }
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
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

    private boolean searchBST(BSTNode node, int value) {
        if (node == null) return false;
        if (node.value == value) return true;
        return value < node.value ? searchBST(node.left, value) : searchBST(node.right, value);
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
            if (node.left == null) {
                return node.right;
            } else if (node.right == null) {
                return node.left;
            }
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

    /**
     * 将BST转换为链表
     */
    private void convertToLinkedList() {
        if (root == null) {
            log("BST为空，无法转换");
            return;
        }

        try {
            // 获取BST的节点值（中序遍历得到有序序列）
            List<Integer> values = new ArrayList<>();
            inorderTraversalValues(root, values);

            // 创建链表状态
            LinkedListPanel.LinkedListState linkedListState =
                    new LinkedListPanel.LinkedListState(values);

            // 切换到链表面板并恢复状态
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (topFrame instanceof DataStructureVisualizer) {
                DataStructureVisualizer mainFrame = (DataStructureVisualizer) topFrame;

                // 直接获取目标面板并恢复状态
                LinkedListPanel linkedListPanel = (LinkedListPanel) mainFrame.getPanel("LinkedList");
                if (linkedListPanel != null) {
                    mainFrame.switchToPanel("LinkedList");
                    // 等待面板切换完成
                    SwingUtilities.invokeLater(() -> {
                        linkedListPanel.restoreFromState(linkedListState);
                        log("✓ BST已转换为链表，节点数: " + values.size());
                    });
                    return;
                }
            }

            log("转换完成，请切换到链表结构面板查看结果");

        } catch (Exception ex) {
            log("转换失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void resetSearch() {
        if (searchTimer != null && searchTimer.isRunning()) {
            searchTimer.stop();
        }
        highlightedNode = null;
        searchPath = null;
        currentSearchIndex = 0;
        isSearching = false;
        currentOperation = null;
    }

    private void clearTree() {
        if (isSearching) {
            log("正在执行查找动画，请等待完成");
            return;
        }

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

        // 绘制标题
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 16));
        g2d.drawString("二叉搜索树", 20, 30);

        if (root != null) {
            drawTree(g2d, root, getWidth() / 2, 100, getWidth() / 4);
        } else {
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
            if ("search".equals(currentOperation)) {
                g2d.drawString("🔍 正在查找中...", 20, 60);
            } else if ("traversal".equals(currentOperation)) {
                g2d.drawString("🔄 正在遍历中...", 20, 60);
            }
        } else if (searchPath != null && !searchPath.isEmpty()) {
            BSTNode lastNode = searchPath.get(searchPath.size() - 1);
            String searchValue = valueField.getText().trim();

            if ("search".equals(currentOperation) && !searchValue.isEmpty()) {
                try {
                    int targetValue = Integer.parseInt(searchValue);
                    if (lastNode.value == targetValue) {
                        g2d.setColor(Color.GREEN);
                        g2d.drawString("✅ 查找完成 - 找到节点: " + targetValue, 20, 60);
                    } else {
                        g2d.setColor(Color.RED);
                        g2d.drawString("❌ 查找完成 - 未找到节点: " + targetValue, 20, 60);
                    }
                } catch (NumberFormatException e) {
                    g2d.setColor(Color.GREEN);
                    g2d.drawString("✅ 遍历完成 - 共访问 " + searchPath.size() + " 个节点", 20, 60);
                }
            } else {
                g2d.setColor(Color.GREEN);
                g2d.drawString("✅ 遍历完成 - 共访问 " + searchPath.size() + " 个节点", 20, 60);
            }
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
        int textWidth = fm.stringWidth(valueStr);
        int textHeight = fm.getHeight();
        g2d.setColor(Color.BLACK);
        g2d.drawString(valueStr, x - textWidth / 2, y + textHeight / 4);

        if (searchPath != null && searchPath.contains(node)) {
            int order = searchPath.indexOf(node) + 1;
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("宋体", Font.BOLD, 12));
            g2d.drawString("(" + order + ")", x - 8, y - radius - 5);
        }
    }

    private Color getNodeColor(BSTNode node) {
        if (isSearching && currentSearchIndex > 0 &&
                currentSearchIndex <= searchPath.size()) {
            BSTNode currentNode = searchPath.get(currentSearchIndex - 1);
            if (node == currentNode) {
                return CURRENT_NODE_COLOR;
            }
        }

        if (searchPath != null && searchPath.contains(node)) {
            int index = searchPath.indexOf(node);

            if (!isSearching) {
                if ("search".equals(currentOperation)) {
                    BSTNode lastNode = searchPath.get(searchPath.size() - 1);
                    String searchValue = valueField.getText().trim();

                    if (!searchValue.isEmpty()) {
                        try {
                            int targetValue = Integer.parseInt(searchValue);
                            if (node == lastNode && node.value == targetValue) {
                                return FOUND_NODE_COLOR;
                            }
                            else if (node == lastNode && node.value != targetValue) {
                                return NOT_FOUND_COLOR;
                            }
                        } catch (NumberFormatException e) {
                        }
                    }
                }

                float ratio = (float) index / (searchPath.size() - 1);
                int red = 255;
                int green = (int) (165 + (90 * ratio));
                int blue = (int) (100 * ratio);
                return new Color(red, green, blue);
            }

            if (index < currentSearchIndex) {
                float ratio = (float) index / (currentSearchIndex - 1);
                int red = 255;
                int green = (int) (165 + (90 * ratio));
                int blue = (int) (100 * ratio);
                return new Color(red, green, blue);
            }
        }

        if (node.left == null && node.right == null) {
            return LEAF_NODE_COLOR;
        } else if (node == root) {
            return ROOT_NODE_COLOR;
        } else {
            return INTERNAL_NODE_COLOR;
        }
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
        drawLegendItem(g2d, FOUND_NODE_COLOR, "找到节点", startX, startY); startY += 20;
        drawLegendItem(g2d, NOT_FOUND_COLOR, "未找到", startX, startY); startY += 20;
        drawLegendItem(g2d, LEAF_NODE_COLOR, "叶子节点", startX, startY); startY += 20;
        drawLegendItem(g2d, ROOT_NODE_COLOR, "根节点", startX, startY);
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

        BSTNode(int value) {
            this.value = value;
        }
    }
}