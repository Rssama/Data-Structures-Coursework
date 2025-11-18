package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;

/**
 * 动态构建哈夫曼树面板
 * 展示哈夫曼树的完整构建过程，带树形可视化
 */
public class HuffmanTreePanel extends JPanel {
    private HuffmanNode root;
    private JTextField inputField;
    private JTextArea logArea;
    private java.util.List<HuffmanNode> currentNodes;
    private java.util.List<ConstructionStep> constructionSteps;
    private int currentStep;
    private boolean isBuilding;
    private java.util.List<HuffmanCode> huffmanCodes;
    private java.util.List<HuffmanNode> forest; // 当前森林中的树

    public HuffmanTreePanel() {
        initializePanel();
    }

    // 序列化状态类
    public static class HuffmanTreeState implements Serializable {
        private static final long serialVersionUID = 1L;
        public java.util.List<Integer> weights;
        public java.util.List<HuffmanCode> codes;
        public boolean buildingCompleted;

        public HuffmanTreeState(java.util.List<Integer> weights, java.util.List<HuffmanCode> codes, boolean buildingCompleted) {
            this.weights = weights != null ? new ArrayList<>(weights) : new ArrayList<>();
            this.codes = codes != null ? new ArrayList<>(codes) : new ArrayList<>();
            this.buildingCompleted = buildingCompleted;
        }
    }

    // 获取当前状态
    public HuffmanTreeState getCurrentState() {
        java.util.List<Integer> weightList = new ArrayList<>();
        boolean completed = false;

        if (root != null) {
            // 获取所有权重值
            extractWeights(root, weightList);
            completed = true;
        } else if (constructionSteps != null && !constructionSteps.isEmpty()) {
            // 如果正在构建过程中，保存当前权重
            ConstructionStep step = constructionSteps.get(currentStep);
            for (HuffmanNode node : step.nodes) {
                if (node.left == null && node.right == null) {
                    weightList.add(node.weight);
                }
            }
            completed = (currentStep == constructionSteps.size() - 1);
        }

        return new HuffmanTreeState(weightList, huffmanCodes, completed);
    }

    private void extractWeights(HuffmanNode node, java.util.List<Integer> weights) {
        if (node == null) return;
        if (node.left == null && node.right == null) {
            weights.add(node.weight);
        }
        extractWeights(node.left, weights);
        extractWeights(node.right, weights);
    }

    // 从状态恢复
    public void restoreFromState(HuffmanTreeState state) {
        if (state == null || state.weights.isEmpty()) {
            resetConstruction();
            log("恢复状态为空或无效");
            return;
        }

        try {
            // 重置所有状态
            resetConstruction();

            // 使用权重重新构建哈夫曼树
            if (state.buildingCompleted) {
                // 直接构建完整的哈夫曼树
                buildCompleteHuffmanTree(state.weights);
                log("从保存状态恢复完整的哈夫曼树，权重数: " + state.weights.size());
            } else {
                // 准备构建过程
                prepareConstructionWithWeights(state.weights);
                log("从保存状态恢复哈夫曼树构建过程，权重数: " + state.weights.size());
            }

            // 恢复编码
            if (state.codes != null && !state.codes.isEmpty()) {
                huffmanCodes = new ArrayList<>(state.codes);
                displayHuffmanCodes();
            }

            repaint();

        } catch (Exception ex) {
            log("恢复状态时发生错误: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void buildCompleteHuffmanTree(java.util.List<Integer> weights) {
        if (weights == null || weights.isEmpty()) {
            return;
        }

        // 使用优先队列构建哈夫曼树
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>((a, b) -> Integer.compare(a.weight, b.weight));
        for (int weight : weights) {
            queue.offer(new HuffmanNode(weight));
        }

        while (queue.size() > 1) {
            HuffmanNode left = queue.poll();
            HuffmanNode right = queue.poll();
            HuffmanNode parent = new HuffmanNode(left.weight + right.weight);
            parent.left = left;
            parent.right = right;
            queue.offer(parent);
        }

        root = queue.poll();
        generateHuffmanCodes();
        isBuilding = false;
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

        inputField = new JTextField(20);
        inputField.setText("5,3,8,2,1,6");

        JButton buildButton = new JButton("开始构建");
        JButton nextButton = new JButton("下一步");
        JButton prevButton = new JButton("上一步");
        JButton resetButton = new JButton("重置");
        JButton completeButton = new JButton("直接完成");

        buildButton.addActionListener(e -> prepareConstruction());
        nextButton.addActionListener(e -> nextStep());
        prevButton.addActionListener(e -> prevStep());
        resetButton.addActionListener(e -> resetConstruction());
        completeButton.addActionListener(e -> completeConstruction());

        panel.add(new JLabel("输入权重(逗号分隔):"));
        panel.add(inputField);
        panel.add(buildButton);
        panel.add(prevButton);
        panel.add(nextButton);
        panel.add(completeButton);
        panel.add(resetButton);

        return panel;
    }

    private void prepareConstruction() {
        try {
            String input = inputField.getText().trim();
            if (input.isEmpty()) {
                log("错误: 请输入权重值");
                return;
            }

            String[] parts = input.split(",");
            java.util.List<Integer> weights = new ArrayList<>();

            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    int weight = Integer.parseInt(trimmed);
                    if (weight <= 0) {
                        log("错误: 权重值必须为正整数");
                        return;
                    }
                    weights.add(weight);
                }
            }

            if (weights.size() < 2) {
                log("至少需要2个权重值");
                return;
            }

            prepareConstructionWithWeights(weights);

        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的正整数，用逗号分隔");
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void prepareConstructionWithWeights(java.util.List<Integer> weights) {
        try {
            // 初始化构建步骤
            constructionSteps = new ArrayList<>();
            currentNodes = new ArrayList<>();
            forest = new ArrayList<>();

            // 创建初始节点
            for (int weight : weights) {
                HuffmanNode node = new HuffmanNode(weight);
                currentNodes.add(node);
                forest.add(node);
            }

            // 排序节点
            Collections.sort(currentNodes, (a, b) -> Integer.compare(a.weight, b.weight));

            // 记录初始状态
            constructionSteps.add(new ConstructionStep(
                    new ArrayList<>(currentNodes),
                    new ArrayList<>(forest),
                    "初始状态: " + getNodeWeights(currentNodes)
            ));

            // 构建哈夫曼树步骤
            buildHuffmanTreeSteps();

            currentStep = 0;
            isBuilding = true;

            ConstructionStep step = constructionSteps.get(currentStep);
            currentNodes = step.nodes;
            forest = step.forest;

            updateDisplay();
            log("哈夫曼树构建准备完成，权重: " + weights);
            log("使用'下一步'按钮开始构建过程");

        } catch (Exception ex) {
            log("准备构建过程时发生错误: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void buildHuffmanTreeSteps() {
        if (currentNodes == null || currentNodes.isEmpty()) {
            return;
        }

        // 复制当前状态
        java.util.List<HuffmanNode> queueList = new ArrayList<>(currentNodes);
        java.util.List<HuffmanNode> currentForest = new ArrayList<>(forest);

        while (queueList.size() > 1) {
            // 排序以获取最小的两个节点
            Collections.sort(queueList, (a, b) -> Integer.compare(a.weight, b.weight));

            // 取出两个最小的节点
            HuffmanNode left = queueList.get(0);
            HuffmanNode right = queueList.get(1);

            // 从列表中移除这两个节点
            queueList.remove(left);
            queueList.remove(right);

            // 从森林中移除这两个节点
            currentForest.remove(left);
            currentForest.remove(right);

            // 创建新节点
            HuffmanNode parent = new HuffmanNode(left.weight + right.weight);
            parent.left = left;
            parent.right = right;

            // 记录合并前的状态
            java.util.List<HuffmanNode> beforeMerge = new ArrayList<>(queueList);
            beforeMerge.add(left);
            beforeMerge.add(right);
            Collections.sort(beforeMerge, (a, b) -> Integer.compare(a.weight, b.weight));

            constructionSteps.add(new ConstructionStep(
                    new ArrayList<>(beforeMerge),
                    new ArrayList<>(currentForest),
                    "准备合并: " + left.weight + " + " + right.weight
            ));

            // 添加新节点到队列和森林
            queueList.add(parent);
            currentForest.add(parent);

            // 记录合并后的状态
            Collections.sort(queueList, (a, b) -> Integer.compare(a.weight, b.weight));
            constructionSteps.add(new ConstructionStep(
                    new ArrayList<>(queueList),
                    new ArrayList<>(currentForest),
                    "合并完成: " + left.weight + " + " + right.weight + " = " + parent.weight
            ));
        }

        if (!queueList.isEmpty()) {
            root = queueList.get(0);
            constructionSteps.add(new ConstructionStep(
                    java.util.Collections.singletonList(root),
                    java.util.Collections.singletonList(root),
                    "构建完成! 根节点权重: " + root.weight
            ));

            // 生成哈夫曼编码
            generateHuffmanCodes();
        }
    }

    private void nextStep() {
        if (!isBuilding || constructionSteps == null || constructionSteps.isEmpty()) {
            log("请先点击'开始构建'准备哈夫曼树");
            return;
        }

        if (currentStep < constructionSteps.size() - 1) {
            currentStep++;
            ConstructionStep step = constructionSteps.get(currentStep);
            currentNodes = step.nodes;
            forest = step.forest;
            log(step.description);
            updateDisplay();

            if (currentStep == constructionSteps.size() - 1) {
                log("✓ 哈夫曼树构建完成!");
                displayHuffmanCodes();
                isBuilding = false;
            }
        } else {
            log("已经是最后一步了");
        }
    }

    private void prevStep() {
        if (!isBuilding || constructionSteps == null || constructionSteps.isEmpty()) {
            log("请先点击'开始构建'准备哈夫曼树");
            return;
        }

        if (currentStep > 0) {
            currentStep--;
            ConstructionStep step = constructionSteps.get(currentStep);
            currentNodes = step.nodes;
            forest = step.forest;
            log("回退到: " + step.description);
            updateDisplay();
        } else {
            log("已经是第一步了");
        }
    }

    private void completeConstruction() {
        if (!isBuilding || constructionSteps == null || constructionSteps.isEmpty()) {
            log("请先点击'开始构建'准备哈夫曼树");
            return;
        }

        currentStep = constructionSteps.size() - 1;
        ConstructionStep step = constructionSteps.get(currentStep);
        currentNodes = step.nodes;
        forest = step.forest;
        log("直接完成构建!");
        log(step.description);
        updateDisplay();
        displayHuffmanCodes();
        isBuilding = false;
    }

    private void resetConstruction() {
        root = null;
        currentNodes = null;
        constructionSteps = null;
        currentStep = 0;
        isBuilding = false;
        huffmanCodes = null;
        forest = null;

        updateDisplay();
        log("构建已重置");
    }

    private void generateHuffmanCodes() {
        huffmanCodes = new ArrayList<>();
        if (root != null) {
            generateCodes(root, "", huffmanCodes);
            Collections.sort(huffmanCodes, (a, b) -> Integer.compare(a.weight, b.weight));
        }
    }

    private void generateCodes(HuffmanNode node, String code, java.util.List<HuffmanCode> codes) {
        if (node == null) return;

        if (node.left == null && node.right == null) {
            codes.add(new HuffmanCode(node.weight, code.isEmpty() ? "0" : code));
            return;
        }

        generateCodes(node.left, code + "0", codes);
        generateCodes(node.right, code + "1", codes);
    }

    private void displayHuffmanCodes() {
        if (huffmanCodes == null || huffmanCodes.isEmpty()) {
            log("未生成哈夫曼编码");
            return;
        }

        log("=== 哈夫曼编码 ===");
        for (HuffmanCode hc : huffmanCodes) {
            log("权重 " + hc.weight + ": " + hc.code);
        }
        log("=================");
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void updateDisplay() {
        repaint();
    }

    private String getNodeWeights(java.util.List<HuffmanNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(nodes.get(i).weight);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制标题和状态
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 16));
        g2d.drawString("哈夫曼树 - 动态构建过程", 20, 30);

        if (constructionSteps != null && !constructionSteps.isEmpty() && currentStep < constructionSteps.size()) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("宋体", Font.PLAIN, 14));
            g2d.drawString("步骤 " + (currentStep + 1) + "/" + constructionSteps.size(), 20, 60);

            ConstructionStep step = constructionSteps.get(currentStep);
            g2d.setColor(Color.DARK_GRAY);
            g2d.setFont(new Font("宋体", Font.PLAIN, 12));
            g2d.drawString(step.description, 20, 80);
        }

        // 绘制当前森林中的树
        if (forest != null && !forest.isEmpty()) {
            drawForest(g2d);
        }

        // 如果构建完成，绘制完整的哈夫曼树
        else if (root != null) {
            drawHuffmanTree(g2d, root, getWidth() / 2, 150, getWidth() / 4);
            drawCodes(g2d);
        }

        // 初始状态
        else if (currentNodes != null && !currentNodes.isEmpty()) {
            drawCurrentNodes(g2d);
            g2d.setColor(Color.BLUE);
            g2d.setFont(new Font("宋体", Font.PLAIN, 14));
            g2d.drawString("请点击'开始构建'按钮开始构建过程", getWidth() / 2 - 120, 120);
        }

        // 没有任何内容
        else {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("宋体", Font.BOLD, 16));
            g2d.drawString("请输入权重并开始构建", getWidth() / 2 - 100, getHeight() / 2);
        }

        // 绘制节点集合（圆形排列）
        if (currentNodes != null && !currentNodes.isEmpty()) {
            drawCurrentNodes(g2d);
        }
    }

    private void drawForest(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 14));
        g2d.drawString("当前森林中的树:", 20, 110);

        int treeSpacing = Math.max(getWidth() / (forest.size() + 1), 150);
        int startY = 150;

        for (int i = 0; i < forest.size(); i++) {
            HuffmanNode treeRoot = forest.get(i);
            int centerX = treeSpacing * (i + 1);

            // 绘制单棵树
            drawSingleTree(g2d, treeRoot, centerX, startY, Math.min(treeSpacing / 3, 100));

            // 在树下方显示权重
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("宋体", Font.PLAIN, 12));
            g2d.drawString("权重: " + treeRoot.weight, centerX - 20, startY + 200);
        }

        // 绘制操作提示
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.PLAIN, 12));
        g2d.drawString("提示: 使用'上一步'和'下一步'按钮控制构建过程", 20, startY + 230);
    }

    private void drawSingleTree(Graphics2D g2d, HuffmanNode node, int x, int y, int hGap) {
        int radius = 20;

        // 绘制左子树
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + 60;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawSingleTree(g2d, node.left, childX, childY, hGap / 2);
        }

        // 绘制右子树
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + 60;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawSingleTree(g2d, node.right, childX, childY, hGap / 2);
        }

        // 绘制当前节点
        if (node.left == null && node.right == null) {
            g2d.setColor(Color.GREEN); // 叶子节点
        } else {
            g2d.setColor(Color.ORANGE); // 内部节点
        }

        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 绘制节点权重
        String weightStr = String.valueOf(node.weight);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(weightStr);
        g2d.setColor(Color.BLACK);
        g2d.drawString(weightStr, x - textWidth / 2, y + 5);

        // 如果是内部节点，显示合并信息
        if (node.left != null && node.right != null) {
            g2d.setFont(new Font("宋体", Font.PLAIN, 10));
            g2d.setColor(Color.RED);
            g2d.drawString(node.left.weight + "+" + node.right.weight, x - 15, y - radius - 5);
        }
    }

    private void drawCurrentNodes(Graphics2D g2d) {
        if (currentNodes == null || currentNodes.isEmpty()) {
            return;
        }

        int startY = getHeight() - 150;

        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 14));
        g2d.drawString("当前节点集合:", 20, startY - 20);

        int startX = 50;
        int nodeSpacing = 60;
        int maxNodesPerRow = Math.max(1, (getWidth() - 100) / nodeSpacing);

        for (int i = 0; i < currentNodes.size(); i++) {
            HuffmanNode node = currentNodes.get(i);
            int row = i / maxNodesPerRow;
            int col = i % maxNodesPerRow;
            int x = startX + col * nodeSpacing;
            int y = startY + row * 60;

            // 绘制节点
            if (node.left == null && node.right == null) {
                g2d.setColor(Color.GREEN); // 叶子节点
            } else {
                g2d.setColor(Color.ORANGE); // 内部节点
            }

            g2d.fillOval(x - 15, y - 15, 30, 30);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x - 15, y - 15, 30, 30);

            // 绘制权重
            String weightStr = String.valueOf(node.weight);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(weightStr);
            g2d.setColor(Color.BLACK);
            g2d.drawString(weightStr, x - textWidth / 2, y + 5);
        }
    }

    private void drawHuffmanTree(Graphics2D g2d, HuffmanNode node, int x, int y, int hGap) {
        int radius = 25;

        // 绘制左子树
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            g2d.drawString("0", (x + childX) / 2, (y + childY) / 2);
            drawHuffmanTree(g2d, node.left, childX, childY, hGap / 2);
        }

        // 绘制右子树
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + 80;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            g2d.drawString("1", (x + childX) / 2, (y + childY) / 2);
            drawHuffmanTree(g2d, node.right, childX, childY, hGap / 2);
        }

        // 绘制当前节点
        if (node.left == null && node.right == null) {
            g2d.setColor(Color.GREEN); // 叶子节点
        } else {
            g2d.setColor(Color.ORANGE); // 内部节点
        }

        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 绘制节点权重
        String weightStr = String.valueOf(node.weight);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(weightStr);
        g2d.setColor(Color.BLACK);
        g2d.drawString(weightStr, x - textWidth / 2, y + 5);
    }

    private void drawCodes(Graphics2D g2d) {
        if (huffmanCodes == null || huffmanCodes.isEmpty()) return;

        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 14));
        g2d.drawString("哈夫曼编码:", 20, getHeight() - 120);

        int yPos = getHeight() - 100;
        for (HuffmanCode hc : huffmanCodes) {
            g2d.drawString("权重 " + hc.weight + ": " + hc.code, 20, yPos);
            yPos += 20;
            if (yPos > getHeight() - 20) break; // 防止超出面板
        }
    }

    // 哈夫曼树节点类 - 修复序列化问题
    private static class HuffmanNode implements Serializable {
        private static final long serialVersionUID = 1L;
        int weight;
        HuffmanNode left;
        HuffmanNode right;

        HuffmanNode(int weight) {
            this.weight = weight;
        }

        @Override
        public String toString() {
            return String.valueOf(weight);
        }
    }

    // 构建步骤类 - 实现序列化
    private static class ConstructionStep implements Serializable {
        private static final long serialVersionUID = 1L;
        java.util.List<HuffmanNode> nodes;
        java.util.List<HuffmanNode> forest;
        String description;

        ConstructionStep(java.util.List<HuffmanNode> nodes, java.util.List<HuffmanNode> forest, String description) {
            this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
            this.forest = forest != null ? new ArrayList<>(forest) : new ArrayList<>();
            this.description = description != null ? description : "";
        }
    }

    // 哈夫曼编码类 - 实现序列化
    public static class HuffmanCode implements Serializable {
        private static final long serialVersionUID = 1L;
        int weight;
        String code;

        public HuffmanCode(int weight, String code) {
            this.weight = weight;
            this.code = code != null ? code : "";
        }
    }
}