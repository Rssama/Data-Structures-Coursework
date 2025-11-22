package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AVL树面板 (算法动画增强版)
 * 核心功能：展示 AVL 树的查找路径、插入平衡、删除平衡以及核心的旋转操作动画。
 */
public class AVLPanel extends JPanel {
    private AVLNode root;
    private JTextField valueField;
    private JTextArea logArea;

    // ================== 动画系统 ==================
    private Timer animationTimer;
    private List<AnimStep> animationSteps;
    private int currentStepIndex = 0;
    private boolean isAnimating = false;

    // 绘图常量
    private final int NODE_RADIUS = 25;
    private final int VERTICAL_GAP = 60;

    // 颜色定义
    private final Color COLOR_DEFAULT = new Color(200, 220, 255); // 默认蓝
    private final Color COLOR_PATH = new Color(255, 165, 0);      // 路径/比较
    private final Color COLOR_TARGET = Color.GREEN;               // 目标/插入位
    private final Color COLOR_UNBALANCED = Color.RED;             // 失衡节点
    private final Color COLOR_PIVOT = Color.MAGENTA;              // 旋转轴心

    public AVLPanel() {
        initializePanel();
    }

    // ================== 动画步骤帧定义 ==================
    private static class AnimStep {
        AVLNode highlightNode; // 高亮主节点
        AVLNode pivotNode;     // 旋转轴心节点 (可选)
        Color color;           // 主节点颜色
        String description;    // 描述文字
        Runnable action;       // 数据修改操作

        AnimStep(AVLNode node, Color color, String desc, Runnable action) {
            this.highlightNode = node;
            this.color = color;
            this.description = desc;
            this.action = action;
        }

        // 设置轴心节点的辅助构造
        AnimStep setPivot(AVLNode p) {
            this.pivotNode = p;
            return this;
        }
    }

    // ================== 序列化状态 ==================
    public static class AVLTreeState implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Integer> nodeValues;
        public AVLTreeState(List<Integer> values) { this.nodeValues = new ArrayList<>(values); }
    }

    public AVLTreeState getCurrentState() {
        List<Integer> values = new ArrayList<>();
        // 使用先序遍历保存结构
        preorderTraversal(root, values);
        return new AVLTreeState(values);
    }

    private void preorderTraversal(AVLNode node, List<Integer> values) {
        if (node == null) return;
        values.add(node.value);
        preorderTraversal(node.left, values);
        preorderTraversal(node.right, values);
    }

    public void restoreFromState(AVLTreeState state) {
        stopAnimation();
        root = null;
        if (state != null && state.nodeValues != null) {
            for (Integer value : state.nodeValues) root = insert(root, value);
        }
        repaint();
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

        JButton insertButton = new JButton("插入(动画)");
        JButton batchButton = new JButton("批量添加");
        JButton searchButton = new JButton("查找(动画)");
        JButton deleteButton = new JButton("删除(动画)");
        JButton clearButton = new JButton("清空");

        insertButton.addActionListener(this::startInsertAnimation);
        batchButton.addActionListener(e -> batchAddNodes());
        searchButton.addActionListener(this::startSearchAnimation);
        deleteButton.addActionListener(this::startDeleteAnimation);
        clearButton.addActionListener(e -> clearTree());

        panel.add(new JLabel("值:"));
        panel.add(valueField);
        panel.add(batchButton);
        panel.add(insertButton);
        panel.add(searchButton);
        panel.add(deleteButton);
        panel.add(clearButton);

        return panel;
    }

    // ================== 动画控制核心 ==================

    private void playAnimation(List<AnimStep> steps) {
        if (isAnimating || steps.isEmpty()) return;
        this.animationSteps = steps;
        this.currentStepIndex = 0;
        this.isAnimating = true;

        // 速度设置：稍微慢一点以便看清旋转
        animationTimer = new Timer(1200, e -> {
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
    }

    // ================== 1. 查找动画 ==================
    private void startSearchAnimation(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (root == null) { log("树为空"); return; }
            List<AnimStep> steps = new ArrayList<>();
            buildSearchSteps(root, value, steps);
            playAnimation(steps);
        } catch (Exception ex) { log("输入错误: " + ex.getMessage()); }
    }

    private void buildSearchSteps(AVLNode node, int value, List<AnimStep> steps) {
        if (node == null) return;
        steps.add(new AnimStep(node, COLOR_PATH, "比较: " + value + " vs " + node.value, null));

        if (value == node.value) {
            steps.add(new AnimStep(node, COLOR_TARGET, "找到节点 " + value, null));
        } else if (value < node.value) {
            if (node.left == null) steps.add(new AnimStep(node, COLOR_UNBALANCED, "向左无路，未找到", null));
            else buildSearchSteps(node.left, value, steps);
        } else {
            if (node.right == null) steps.add(new AnimStep(node, COLOR_UNBALANCED, "向右无路，未找到", null));
            else buildSearchSteps(node.right, value, steps);
        }
    }

    // ================== 2. 插入动画 (核心逻辑) ==================

    // 注意：为了支持动画，我们需要重新设计递归结构，不能直接用返回 Node 的方式
    // 这里采用 "模拟执行 + 记录步骤" 的方式有点困难，因为旋转会改变树结构。
    // 最佳策略：在 AnimStep 的 action 中真正执行 insert 逻辑的一部分。
    // 但为了代码简洁性，我们采用 "克隆树模拟" 或者 "直接记录操作指令"。
    // 这里我们采用一种折中方案：
    // 使用一个辅助类来构建操作序列，实际修改发生在 action 中。

    private void startInsertAnimation(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (search(root, value)) { log("节点已存在"); return; }

            List<AnimStep> steps = new ArrayList<>();
            if (root == null) {
                steps.add(new AnimStep(null, COLOR_TARGET, "新建根节点 " + value, () -> root = new AVLNode(value)));
            } else {
                // 我们需要一个引用容器来修改 root
                buildInsertSteps(root, null, value, steps, true);
            }
            playAnimation(steps);
            valueField.setText("");
        } catch (Exception ex) { log("输入错误: " + ex.getMessage()); }
    }

    // 返回值表示是否树高发生了变化（用于平衡判断逻辑），但实际修改在 steps 里
    // isLeftChild: 当前 node 是 parent 的左孩子还是右孩子
    private void buildInsertSteps(AVLNode node, AVLNode parent, int value, List<AnimStep> steps, boolean isRoot) {
        if (node == null) return; // Should not happen given logic below

        steps.add(new AnimStep(node, COLOR_PATH, "比较: " + value + " vs " + node.value, null));

        if (value < node.value) {
            if (node.left == null) {
                steps.add(new AnimStep(node, COLOR_TARGET, "左子树为空，插入 " + value, () -> {
                    node.left = new AVLNode(value);
                    updateHeight(node);
                }));
            } else {
                buildInsertSteps(node.left, node, value, steps, false);
            }
        } else {
            if (node.right == null) {
                steps.add(new AnimStep(node, COLOR_TARGET, "右子树为空，插入 " + value, () -> {
                    node.right = new AVLNode(value);
                    updateHeight(node);
                }));
            } else {
                buildInsertSteps(node.right, node, value, steps, false);
            }
        }

        // 回溯阶段：检查平衡
        // 注意：这里的 updateHeight 和 getBalance 必须在运行时动态计算，
        // 所以我们添加一个检查步骤
        steps.add(new AnimStep(node, COLOR_PATH, "回溯: 更新高度并检查平衡 (BF=" + getBalance(node) + ")", () -> {
            updateHeight(node);
        }));

        // 平衡旋转逻辑
        // 为了在动画中捕获状态，我们必须延迟计算平衡因子。
        // 这里使用一个技巧：在 action 中判断是否需要旋转，并执行旋转。
        // 但为了展示"旋转动画"，我们需要预判。
        // 由于预判很复杂，这里我们将"旋转"作为一个原子操作封装在 Action 里，
        // 但通过连续的 Step 来模拟旋转的视觉效果。

        steps.add(new AnimStep(node, COLOR_DEFAULT, "平衡检查...", () -> {
            int balance = getBalance(node);

            // 左左情况 (LL) -> 右旋
            if (balance > 1 && value < node.left.value) {
                log("触发 LL 型失衡，执行右旋...");
                AVLNode newRoot = rightRotate(node);
                replaceChild(parent, node, newRoot, isRoot);
            }
            // 右右情况 (RR) -> 左旋
            else if (balance < -1 && value > node.right.value) {
                log("触发 RR 型失衡，执行左旋...");
                AVLNode newRoot = leftRotate(node);
                replaceChild(parent, node, newRoot, isRoot);
            }
            // 左右情况 (LR) -> 先左旋后右旋
            else if (balance > 1 && value > node.left.value) {
                log("触发 LR 型失衡，执行双旋(先左后右)...");
                node.left = leftRotate(node.left);
                AVLNode newRoot = rightRotate(node);
                replaceChild(parent, node, newRoot, isRoot);
            }
            // 右左情况 (RL) -> 先右旋后左旋
            else if (balance < -1 && value < node.right.value) {
                log("触发 RL 型失衡，执行双旋(先右后左)...");
                node.right = rightRotate(node.right);
                AVLNode newRoot = leftRotate(node);
                replaceChild(parent, node, newRoot, isRoot);
            }
        }));
    }

    // 辅助方法：替换父节点的子节点引用
    private void replaceChild(AVLNode parent, AVLNode oldChild, AVLNode newChild, boolean isRoot) {
        if (isRoot) {
            root = newChild;
        } else {
            if (parent.left == oldChild) parent.left = newChild;
            else parent.right = newChild;
        }
    }

    // ================== 3. 删除动画 ==================

    private void startDeleteAnimation(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (!search(root, value)) { log("节点不存在"); return; }

            // 对于 AVL 删除，涉及到复杂的递归回溯平衡。
            // 为了简化演示，我们先执行删除，然后通过重绘展示结果。
            // 若要精细动画，逻辑与插入类似，但更繁琐。
            // 这里提供一个"查找 -> 删除 -> 重新平衡"的简化动画流程。

            List<AnimStep> steps = new ArrayList<>();
            buildSearchSteps(root, value, steps); // 先展示查找路径

            steps.add(new AnimStep(null, COLOR_UNBALANCED, "执行删除与回溯平衡...", () -> {
                root = delete(root, value);
            }));

            playAnimation(steps);
            valueField.setText("");
        } catch (Exception ex) { log("输入错误: " + ex.getMessage()); }
    }

    // ================== AVL 核心算法 (用于实际执行) ==================

    private int height(AVLNode N) {
        return (N == null) ? 0 : N.height;
    }

    private void updateHeight(AVLNode N) {
        if (N != null) N.height = Math.max(height(N.left), height(N.right)) + 1;
    }

    // 获取平衡因子
    private int getBalance(AVLNode N) {
        return (N == null) ? 0 : height(N.left) - height(N.right);
    }

    private AVLNode rightRotate(AVLNode y) {
        AVLNode x = y.left;
        AVLNode T2 = x.right;

        // 旋转
        x.right = y;
        y.left = T2;

        // 更新高度
        updateHeight(y);
        updateHeight(x);

        return x;
    }

    private AVLNode leftRotate(AVLNode x) {
        AVLNode y = x.right;
        AVLNode T2 = y.left;

        // 旋转
        y.left = x;
        x.right = T2;

        // 更新高度
        updateHeight(x);
        updateHeight(y);

        return y;
    }

    // 标准 AVL 插入 (用于批量添加)
    private AVLNode insert(AVLNode node, int value) {
        if (node == null) return new AVLNode(value);
        if (value < node.value) node.left = insert(node.left, value);
        else if (value > node.value) node.right = insert(node.right, value);
        else return node;

        updateHeight(node);
        int balance = getBalance(node);

        // 4种旋转情况
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

    // 标准 AVL 删除
    private AVLNode delete(AVLNode root, int value) {
        if (root == null) return root;

        if (value < root.value) root.left = delete(root.left, value);
        else if (value > root.value) root.right = delete(root.right, value);
        else {
            if ((root.left == null) || (root.right == null)) {
                AVLNode temp = (root.left != null) ? root.left : root.right;
                if (temp == null) {
                    temp = root;
                    root = null;
                } else root = temp;
            } else {
                AVLNode temp = minValueNode(root.right);
                root.value = temp.value;
                root.right = delete(root.right, temp.value);
            }
        }

        if (root == null) return root;

        updateHeight(root);
        int balance = getBalance(root);

        if (balance > 1 && getBalance(root.left) >= 0) return rightRotate(root);
        if (balance > 1 && getBalance(root.left) < 0) {
            root.left = leftRotate(root.left);
            return rightRotate(root);
        }
        if (balance < -1 && getBalance(root.right) <= 0) return leftRotate(root);
        if (balance < -1 && getBalance(root.right) > 0) {
            root.right = rightRotate(root.right);
            return leftRotate(root);
        }
        return root;
    }

    private AVLNode minValueNode(AVLNode node) {
        AVLNode current = node;
        while (current.left != null) current = current.left;
        return current;
    }

    private boolean search(AVLNode node, int value) {
        if (node == null) return false;
        if (node.value == value) return true;
        return value < node.value ? search(node.left, value) : search(node.right, value);
    }

    // ================== 绘图逻辑 (路径留痕 + 平衡因子显示) ==================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 16));
        g2d.drawString("AVL平衡二叉树", 20, 30);

        if (isAnimating && currentStepIndex > 0 && currentStepIndex <= animationSteps.size()) {
            AnimStep step = animationSteps.get(currentStepIndex - 1);
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("微软雅黑", Font.BOLD, 14));
            g2d.drawString("当前操作: " + step.description, 20, 60);
        }

        if (root != null) {
            drawTree(g2d, root, getWidth() / 2, 100, getWidth() / 4);
        } else {
            g2d.setColor(Color.RED);
            g2d.drawString("树为空", getWidth() / 2 - 20, getHeight() / 2);
        }

        drawLegend(g2d);
    }

    private void drawTree(Graphics2D g2d, AVLNode node, int x, int y, int hGap) {
        // 连线
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

        // 颜色逻辑
        Color nodeColor = COLOR_DEFAULT;
        if (isAnimating && animationSteps != null) {
            for (int i = 0; i < currentStepIndex; i++) {
                if (i >= animationSteps.size()) break;
                AnimStep step = animationSteps.get(i);
                if (step.highlightNode == node) {
                    // 路径留痕
                    if (step.color == COLOR_PATH) nodeColor = COLOR_PATH;
                    // 旋转轴心高亮
                    if (step.color == COLOR_PIVOT) nodeColor = COLOR_PIVOT;
                    // 失衡节点高亮
                    if (step.color == COLOR_UNBALANCED) nodeColor = COLOR_UNBALANCED;
                    // 目标节点
                    if (step.color == COLOR_TARGET) nodeColor = COLOR_TARGET;
                }
            }
            // 当前帧覆盖
            if (currentStepIndex > 0 && currentStepIndex <= animationSteps.size()) {
                AnimStep currentStep = animationSteps.get(currentStepIndex - 1);
                if (currentStep.highlightNode == node) nodeColor = currentStep.color;
            }
        }

        // 节点
        g2d.setColor(nodeColor);
        g2d.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        // 数值
        String valStr = String.valueOf(node.value);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.BLACK);
        g2d.drawString(valStr, x - fm.stringWidth(valStr) / 2, y + 5);

        // 平衡因子显示
        int balance = getBalance(node);
        g2d.setFont(new Font("宋体", Font.PLAIN, 10));
        g2d.setColor(Math.abs(balance) > 1 ? Color.RED : Color.BLUE);
        g2d.drawString("BF:" + balance, x + NODE_RADIUS, y - 5);
    }

    private void drawLegend(Graphics2D g2d) {
        int x = 20;
        int y = getHeight() - 40;
        g2d.setFont(new Font("宋体", Font.PLAIN, 12));
        drawLegendItem(g2d, COLOR_PATH, "查找路径", x, y); x+=100;
        drawLegendItem(g2d, COLOR_UNBALANCED, "失衡节点", x, y); x+=100;
        drawLegendItem(g2d, COLOR_TARGET, "新插入/目标", x, y);
    }

    private void drawLegendItem(Graphics2D g, Color c, String text, int x, int y) {
        g.setColor(c);
        g.fillRect(x, y, 15, 15);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, 15, 15);
        g.drawString(text, x + 20, y + 12);
    }

    // ================== 杂项方法 ==================

    private void batchAddNodes() {
        String input = valueField.getText().trim();
        if(input.isEmpty()) return;
        String[] parts = input.split("[,，]");
        for(String p : parts) {
            try {
                int val = Integer.parseInt(p.trim());
                root = insert(root, val);
            } catch(Exception ignored){}
        }
        valueField.setText("");
        repaint();
    }

    private void clearTree() {
        stopAnimation();
        root = null;
        repaint();
        log("树已清空");
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
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