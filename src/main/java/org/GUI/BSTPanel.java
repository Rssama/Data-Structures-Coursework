package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 二叉搜索树面板 (完整功能版)
 * 包含：插入、删除、查找、批量构建、转链表
 * 特性：全过程动画，支持查找路径留痕
 */
public class BSTPanel extends JPanel {
    private BSTNode root;
    private JTextField valueField;
    private JTextArea logArea;

    // ================== 动画系统 ==================
    private Timer animationTimer;
    private List<AnimStep> animationSteps; // 存储预计算的动画步骤
    private int currentStepIndex = 0;
    private boolean isAnimating = false;

    // 绘图常量
    private final int NODE_RADIUS = 25;
    private final int VERTICAL_GAP = 60;

    // 颜色定义
    private final Color COLOR_DEFAULT = new Color(200, 220, 255); // 默认蓝
    private final Color COLOR_COMPARE = Color.YELLOW;             // 当前比较中
    private final Color COLOR_TARGET = Color.GREEN;               // 找到目标/插入位
    private final Color COLOR_DELETE = Color.RED;                 // 待删除/未找到
    private final Color COLOR_PATH = new Color(255, 165, 0);      // 路径经过(橙色)

    public BSTPanel() {
        initializePanel();
    }

    // ================== 内部类：动画步骤帧 ==================
    private static class AnimStep {
        BSTNode highlightNode; // 当前关注的节点
        Color color;           // 节点显示的颜色
        String description;    // 顶部显示的算法解释
        Runnable action;       // 这一步要执行的实际数据修改（可选）

        AnimStep(BSTNode node, Color color, String desc, Runnable action) {
            this.highlightNode = node;
            this.color = color;
            this.description = desc;
            this.action = action;
        }
    }

    // ================== 序列化状态 ==================
    public static class BSTState implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Integer> nodeValues;
        public BSTState(List<Integer> values) { this.nodeValues = new ArrayList<>(values); }
    }

    public BSTState getCurrentState() {
        List<Integer> values = new ArrayList<>();
        preorderTraversalValues(root, values); // 先序保存以恢复结构
        return new BSTState(values);
    }

    private void preorderTraversalValues(BSTNode node, List<Integer> values) {
        if (node == null) return;
        values.add(node.value);
        preorderTraversalValues(node.left, values);
        preorderTraversalValues(node.right, values);
    }

    public void restoreFromState(BSTState state) {
        stopAnimation();
        root = null;
        if (state != null && state.nodeValues != null) {
            for (Integer value : state.nodeValues) root = insertBST(root, value);
        }
        repaint();
        log("恢复状态完成，节点数: " + (state == null ? 0 : state.nodeValues.size()));
    }

    // ================== UI 初始化 ==================
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
        valueField = new JTextField(15);

        // 按钮定义
        JButton insertButton = new JButton("插入(动画)");
        JButton searchButton = new JButton("查找(动画)"); // 恢复查找按钮
        JButton batchButton = new JButton("批量添加");
        JButton deleteButton = new JButton("删除(动画)");
        JButton clearButton = new JButton("清空");
        JButton toListButton = new JButton("转为链表");

        // 事件绑定
        insertButton.addActionListener(this::startInsertAnimation);
        searchButton.addActionListener(this::startSearchAnimation); // 绑定查找事件
        batchButton.addActionListener(e -> batchAddNodes());
        deleteButton.addActionListener(this::startDeleteAnimation);
        clearButton.addActionListener(e -> clearTree());
        toListButton.addActionListener(e -> convertToLinkedList());

        panel.add(new JLabel("值:"));
        panel.add(valueField);
        panel.add(batchButton);
        panel.add(insertButton);
        panel.add(searchButton);
        panel.add(deleteButton);
        panel.add(clearButton);
        panel.add(toListButton);

        return panel;
    }

    // ================== 动画逻辑核心 ==================

    private void playAnimation(List<AnimStep> steps) {
        if (isAnimating || steps.isEmpty()) return;
        this.animationSteps = steps;
        this.currentStepIndex = 0;
        this.isAnimating = true;

        animationTimer = new Timer(1000, e -> { // 1秒一帧
            if (currentStepIndex < animationSteps.size()) {
                AnimStep step = animationSteps.get(currentStepIndex);
                if (step.action != null) step.action.run(); // 执行数据修改
                log(step.description);
                repaint();
                currentStepIndex++;
            } else {
                stopAnimation();
                log("=== 动画结束 ===");
                repaint();
            }
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null) animationTimer.stop();
        isAnimating = false;
        // 保留 currentStepIndex 和 animationSteps 片刻以便重绘最后一帧的状态，或者清空
        // 这里选择不清空 animationSteps 以便在动画结束后依然能看到最后的路径（直到下次操作）
        // 但为了避免混淆，通常动画结束就恢复默认颜色，或者保持最后状态。
        // 为了实现"查找后路径高亮保留"，我们这里不设为null，只在下次开始时重置。
    }

    // ---------------- 1. 查找动画 (新增) ----------------
    private void startSearchAnimation(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (root == null) { log("树为空"); return; }

            List<AnimStep> steps = new ArrayList<>();
            buildSearchSteps(root, value, steps);

            // 如果最后一步不是找到，说明未找到
            if (steps.isEmpty() || steps.get(steps.size()-1).color != COLOR_TARGET) {
                // 可以加一个未找到的提示帧
                // steps.add(new AnimStep(null, COLOR_DELETE, "未找到节点 " + value, null));
                // 但由于AnimStep需要绑定节点，这里暂不处理null节点的高亮
            }

            playAnimation(steps);
        } catch (Exception ex) { log("输入错误: " + ex.getMessage()); }
    }

    private void buildSearchSteps(BSTNode node, int value, List<AnimStep> steps) {
        if (node == null) return;

        // 步骤：比较
        String relation = (value == node.value) ? " = " : (value < node.value ? " < " : " > ");
        steps.add(new AnimStep(node, COLOR_COMPARE, "比较: " + value + relation + node.value, null));

        if (value == node.value) {
            steps.add(new AnimStep(node, COLOR_TARGET, "✓ 查找成功: 节点 " + value, null));
            return;
        } else if (value < node.value) {
            if (node.left != null) {
                // 记录路径颜色(这里用 COLOR_PATH 表示经过)
                steps.add(new AnimStep(node, COLOR_PATH, "向左寻找...", null));
                buildSearchSteps(node.left, value, steps);
            } else {
                steps.add(new AnimStep(node, COLOR_DELETE, "✗ 向左无路，未找到 " + value, null));
            }
        } else {
            if (node.right != null) {
                steps.add(new AnimStep(node, COLOR_PATH, "向右寻找...", null));
                buildSearchSteps(node.right, value, steps);
            } else {
                steps.add(new AnimStep(node, COLOR_DELETE, "✗ 向右无路，未找到 " + value, null));
            }
        }
    }

    // ---------------- 2. 插入动画 ----------------
    private void startInsertAnimation(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (searchBST(root, value)) { log("节点 " + value + " 已存在"); return; }

            List<AnimStep> steps = new ArrayList<>();
            if (root == null) {
                steps.add(new AnimStep(null, COLOR_TARGET, "创建根节点 " + value, () -> root = new BSTNode(value)));
            } else {
                buildInsertSteps(root, value, steps);
            }
            playAnimation(steps);
            valueField.setText("");
        } catch (Exception ex) { log("输入错误: " + ex.getMessage()); }
    }

    private void buildInsertSteps(BSTNode node, int value, List<AnimStep> steps) {
        if (node == null) return;
        steps.add(new AnimStep(node, COLOR_COMPARE, "比较: " + value + (value < node.value ? " < " : " > ") + node.value, null));

        if (value < node.value) {
            if (node.left == null) {
                steps.add(new AnimStep(node, COLOR_TARGET, "左子树为空，插入 " + value, () -> node.left = new BSTNode(value)));
            } else {
                steps.add(new AnimStep(node, COLOR_PATH, "向左移动", null));
                buildInsertSteps(node.left, value, steps);
            }
        } else {
            if (node.right == null) {
                steps.add(new AnimStep(node, COLOR_TARGET, "右子树为空，插入 " + value, () -> node.right = new BSTNode(value)));
            } else {
                steps.add(new AnimStep(node, COLOR_PATH, "向右移动", null));
                buildInsertSteps(node.right, value, steps);
            }
        }
    }

    // ---------------- 3. 删除动画 ----------------
    private void startDeleteAnimation(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (!searchBST(root, value)) { log("节点 " + value + " 不存在"); return; }

            List<AnimStep> steps = new ArrayList<>();
            buildDeleteSearchSteps(root, null, value, steps);
            playAnimation(steps);
            valueField.setText("");
        } catch (Exception ex) { log("输入错误: " + ex.getMessage()); }
    }

    private void buildDeleteSearchSteps(BSTNode node, BSTNode parent, int value, List<AnimStep> steps) {
        if (node == null) return;
        steps.add(new AnimStep(node, COLOR_COMPARE, "查找删除目标 " + value, null));

        if (value < node.value) {
            steps.add(new AnimStep(node, COLOR_PATH, "向左查找", null));
            buildDeleteSearchSteps(node.left, node, value, steps);
        } else if (value > node.value) {
            steps.add(new AnimStep(node, COLOR_PATH, "向右查找", null));
            buildDeleteSearchSteps(node.right, node, value, steps);
        } else {
            steps.add(new AnimStep(node, COLOR_DELETE, "锁定目标 " + value, null));
            generateDeleteActionSteps(node, parent, steps);
        }
    }

    private void generateDeleteActionSteps(BSTNode target, BSTNode parent, List<AnimStep> steps) {
        if (target.left == null && target.right == null) {
            steps.add(new AnimStep(target, COLOR_DELETE, "叶子节点: 直接移除", () -> {
                if (parent == null) root = null;
                else if (parent.left == target) parent.left = null;
                else parent.right = null;
            }));
        } else if (target.left == null) {
            steps.add(new AnimStep(target, COLOR_DELETE, "单右子树: 父节点接管右子树", () -> {
                if (parent == null) root = target.right;
                else if (parent.left == target) parent.left = target.right;
                else parent.right = target.right;
            }));
        } else if (target.right == null) {
            steps.add(new AnimStep(target, COLOR_DELETE, "单左子树: 父节点接管左子树", () -> {
                if (parent == null) root = target.left;
                else if (parent.left == target) parent.left = target.left;
                else parent.right = target.left;
            }));
        } else {
            steps.add(new AnimStep(target, COLOR_DELETE, "双子节点: 寻找右子树最小后继", null));
            BSTNode successor = target.right;
            BSTNode succParent = target;
            steps.add(new AnimStep(successor, COLOR_PATH, "进入右子树", null));

            while (successor.left != null) {
                succParent = successor;
                successor = successor.left;
                steps.add(new AnimStep(successor, COLOR_PATH, "向左寻找最小...", null));
            }

            final int succVal = successor.value;
            steps.add(new AnimStep(successor, COLOR_TARGET, "找到后继: " + succVal, null));
            steps.add(new AnimStep(target, COLOR_TARGET, "值替换: " + target.value + " -> " + succVal, () -> target.value = succVal));

            final BSTNode finalSucc = successor;
            final BSTNode finalSuccParent = succParent;
            steps.add(new AnimStep(finalSucc, COLOR_DELETE, "移除后继节点原位置", () -> {
                if (finalSuccParent.left == finalSucc) finalSuccParent.left = finalSucc.right;
                else finalSuccParent.right = finalSucc.right;
            }));
        }
    }

    // ================== 绘图逻辑 (升级版：路径留痕) ==================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 16));
        g2d.drawString("二叉搜索树 (算法动画)", 20, 30);

        // 显示当前步骤说明
        if (isAnimating && currentStepIndex > 0 && currentStepIndex <= animationSteps.size()) {
            String desc = animationSteps.get(currentStepIndex - 1).description;
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("微软雅黑", Font.BOLD, 14));
            g2d.drawString("当前操作: " + desc, 20, 60);
        }

        if (root != null) {
            drawTree(g2d, root, getWidth() / 2, 100, getWidth() / 4);
        } else {
            g2d.setColor(Color.RED);
            g2d.drawString("树为空", getWidth() / 2 - 20, getHeight() / 2);
        }

        drawLegend(g2d);
    }

    private void drawTree(Graphics2D g2d, BSTNode node, int x, int y, int hGap) {
        // 1. 绘制连线
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + VERTICAL_GAP;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y, childX, childY);
            drawTree(g2d, node.left, childX, childY, hGap / 2);
        }
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + VERTICAL_GAP;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y, childX, childY);
            drawTree(g2d, node.right, childX, childY, hGap / 2);
        }

        // 2. 确定节点颜色
        Color nodeColor = COLOR_DEFAULT;

        // 核心修改：路径染色逻辑
        if (isAnimating && animationSteps != null) {
            // 倒序遍历历史步骤，确定该节点是否被访问过
            // 我们倒序是为了让最新的状态（比如当前正在比较）覆盖旧的状态
            boolean isVisited = false;

            // 检查历史路径 (0 到 currentStepIndex - 1)
            for (int i = 0; i < currentStepIndex; i++) {
                if (i >= animationSteps.size()) break;
                AnimStep step = animationSteps.get(i);
                if (step.highlightNode == node) {
                    // 如果这个节点在历史步骤中出现过，将其标记为路径色
                    // 特别是 COMPARE 或 PATH 类型的操作
                    if (step.color == COLOR_COMPARE || step.color == COLOR_PATH) {
                        nodeColor = COLOR_PATH; // 留痕颜色 (橙色)
                    }
                    // 如果是找到目标，保持绿色
                    if (step.color == COLOR_TARGET) {
                        nodeColor = COLOR_TARGET;
                    }
                    // 如果是未找到/删除，保持红色
                    if (step.color == COLOR_DELETE) {
                        nodeColor = COLOR_DELETE;
                    }
                }
            }

            // 覆盖当前帧的颜色 (高亮当前正在操作的节点)
            if (currentStepIndex > 0 && currentStepIndex <= animationSteps.size()) {
                AnimStep currentStep = animationSteps.get(currentStepIndex - 1);
                if (currentStep.highlightNode == node) {
                    nodeColor = currentStep.color;
                }
            }
        }

        // 3. 绘制节点
        g2d.setColor(nodeColor);
        g2d.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        String valStr = String.valueOf(node.value);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(valStr, x - fm.stringWidth(valStr) / 2, y + 5);
    }

    private void drawLegend(Graphics2D g2d) {
        int startX = 20;
        int startY = getHeight() - 40;
        g2d.setFont(new Font("宋体", Font.PLAIN, 12));

        drawLegendItem(g2d, COLOR_COMPARE, "当前比较", startX, startY);
        drawLegendItem(g2d, COLOR_PATH, "已遍历路径", startX + 100, startY);
        drawLegendItem(g2d, COLOR_TARGET, "找到目标", startX + 200, startY);
        drawLegendItem(g2d, COLOR_DELETE, "删除/未找到", startX + 300, startY);
    }

    private void drawLegendItem(Graphics2D g, Color c, String text, int x, int y) {
        g.setColor(c);
        g.fillRect(x, y, 15, 15);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, 15, 15);
        g.drawString(text, x + 20, y + 12);
    }

    // ================== 基础数据操作 ==================

    private void batchAddNodes() {
        if (isAnimating) return;
        String input = valueField.getText().trim();
        if(input.isEmpty()) return;
        String[] parts = input.split("[,，]");
        for(String p : parts) {
            try {
                int val = Integer.parseInt(p.trim());
                root = insertBST(root, val);
            } catch(Exception ignored){}
        }
        valueField.setText("");
        repaint();
        log("批量添加完成");
    }

    private BSTNode insertBST(BSTNode node, int value) {
        if (node == null) return new BSTNode(value);
        if (value < node.value) node.left = insertBST(node.left, value);
        else if (value > node.value) node.right = insertBST(node.right, value);
        return node;
    }

    private boolean searchBST(BSTNode node, int value) {
        if (node == null) return false;
        if (value == node.value) return true;
        return value < node.value ? searchBST(node.left, value) : searchBST(node.right, value);
    }

    private void clearTree() {
        stopAnimation();
        root = null;
        repaint();
        log("清空树");
    }

    private void convertToLinkedList() {
        if (root == null) { log("BST为空"); return; }
        try {
            List<Integer> values = new ArrayList<>();
            inorderTraversalValues(root, values);
            LinkedListPanel.LinkedListState state = new LinkedListPanel.LinkedListState(values);
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (topFrame instanceof DataStructureVisualizer) {
                DataStructureVisualizer mainFrame = (DataStructureVisualizer) topFrame;
                LinkedListPanel panel = (LinkedListPanel) mainFrame.getPanel("LinkedList");
                if (panel != null) {
                    mainFrame.switchToPanel("LinkedList");
                    SwingUtilities.invokeLater(() -> {
                        panel.restoreFromState(state);
                        log("已转换为链表");
                    });
                }
            }
        } catch (Exception e) { log("转换失败: " + e.getMessage()); }
    }

    private void inorderTraversalValues(BSTNode node, List<Integer> values) {
        if (node == null) return;
        inorderTraversalValues(node.left, values);
        values.add(node.value);
        inorderTraversalValues(node.right, values);
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static class BSTNode implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;
        BSTNode left;
        BSTNode right;
        BSTNode(int value) { this.value = value; }
    }
}