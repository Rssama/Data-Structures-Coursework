package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;

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
            String input = inputField.getText();
            String[] parts = input.split(",");
            java.util.List<Integer> weights = new ArrayList<>();

            for (String part : parts) {
                weights.add(Integer.parseInt(part.trim()));
            }

            if (weights.size() < 2) {
                log("至少需要2个权重值");
                return;
            }

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
            Collections.sort(currentNodes);

            // 记录初始状态
            constructionSteps.add(new ConstructionStep(
                    new ArrayList<>(currentNodes),
                    new ArrayList<>(forest),
                    "初始状态: " + getNodeWeights(currentNodes)
            ));

            // 构建哈夫曼树
            buildHuffmanTreeSteps();

            currentStep = 0;
            isBuilding = true;

            log("哈夫曼树构建准备完成");
            log("共有 " + constructionSteps.size() + " 个构建步骤");
            log("点击'下一步'开始逐步构建");

            updateDisplay();

        } catch (NumberFormatException ex) {
            log("请输入有效的数字，用逗号分隔");
        }
    }

    private void buildHuffmanTreeSteps() {
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>(currentNodes);
        java.util.List<HuffmanNode> currentForest = new ArrayList<>(forest);

        while (queue.size() > 1) {
            // 取出两个最小的节点
            HuffmanNode left = queue.poll();
            HuffmanNode right = queue.poll();

            // 从森林中移除这两个节点
            currentForest.remove(left);
            currentForest.remove(right);

            // 创建新节点
            HuffmanNode parent = new HuffmanNode(left.weight + right.weight);
            parent.left = left;
            parent.right = right;

            // 记录合并前的状态
            java.util.List<HuffmanNode> currentState = new ArrayList<>(queue);
            currentState.add(left);
            currentState.add(right);
            Collections.sort(currentState);

            constructionSteps.add(new ConstructionStep(
                    new ArrayList<>(currentState),
                    new ArrayList<>(currentForest),
                    "合并节点: " + left.weight + " + " + right.weight + " = " + parent.weight
            ));

            // 添加新节点到队列和森林
            queue.offer(parent);
            currentForest.add(parent);

            // 记录合并后的状态
            currentState = new ArrayList<>(queue);
            Collections.sort(currentState);
            constructionSteps.add(new ConstructionStep(
                    new ArrayList<>(currentState),
                    new ArrayList<>(currentForest),
                    "添加新节点: " + parent.weight
            ));
        }

        root = queue.poll();
        constructionSteps.add(new ConstructionStep(
                Collections.singletonList(root),
                Collections.singletonList(root),
                "构建完成! 根节点权重: " + root.weight
        ));

        // 生成哈夫曼编码
        generateHuffmanCodes();
    }

    private void nextStep() {
        if (!isBuilding || constructionSteps == null) {
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
            }
        } else {
            log("已经是最后一步了");
        }
    }

    private void prevStep() {
        if (!isBuilding || constructionSteps == null) {
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
        if (!isBuilding || constructionSteps == null) {
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
        generateCodes(root, "", huffmanCodes);
        Collections.sort(huffmanCodes, (a, b) -> Integer.compare(a.weight, b.weight));
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
        if (huffmanCodes == null) return;

        log("=== 哈夫曼编码 ===");
        for (HuffmanCode hc : huffmanCodes) {
            log("权重 " + hc.weight + ": " + hc.code);
        }
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void updateDisplay() {
        repaint();
    }

    private String getNodeWeights(java.util.List<HuffmanNode> nodes) {
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

        if (constructionSteps != null && currentStep < constructionSteps.size()) {
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

        int treeSpacing = getWidth() / (forest.size() + 1);
        int startY = 150;

        for (int i = 0; i < forest.size(); i++) {
            HuffmanNode treeRoot = forest.get(i);
            int centerX = treeSpacing * (i + 1);

            // 绘制单棵树
            drawSingleTree(g2d, treeRoot, centerX, startY, treeSpacing / 3);

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

        // 计算树的高度
        int treeHeight = calculateTreeHeight(node);
        int vGap = 60; // 垂直间距

        // 绘制左子树
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + vGap;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y + radius, childX, childY - radius);
            drawSingleTree(g2d, node.left, childX, childY, hGap / 2);
        }

        // 绘制右子树
        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + vGap;
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

    private int calculateTreeHeight(HuffmanNode node) {
        if (node == null) return 0;
        return 1 + Math.max(calculateTreeHeight(node.left), calculateTreeHeight(node.right));
    }

    private void drawCurrentNodes(Graphics2D g2d) {
        int startY = getHeight() - 150;

        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 14));
        g2d.drawString("当前节点集合:", 20, startY - 20);

        int startX = 50;
        int nodeSpacing = 60;

        for (int i = 0; i < currentNodes.size(); i++) {
            HuffmanNode node = currentNodes.get(i);
            int x = startX + i * nodeSpacing;
            int y = startY;

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
        if (huffmanCodes == null) return;

        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("宋体", Font.BOLD, 14));
        g2d.drawString("哈夫曼编码:", 20, getHeight() - 120);

        int yPos = getHeight() - 100;
        for (HuffmanCode hc : huffmanCodes) {
            g2d.drawString("权重 " + hc.weight + ": " + hc.code, 20, yPos);
            yPos += 20;
        }
    }

    // 哈夫曼树节点类
    private static class HuffmanNode implements Comparable<HuffmanNode> {
        int weight;
        HuffmanNode left;
        HuffmanNode right;

        HuffmanNode(int weight) {
            this.weight = weight;
        }

        @Override
        public int compareTo(HuffmanNode other) {
            return Integer.compare(this.weight, other.weight);
        }

        @Override
        public String toString() {
            return String.valueOf(weight);
        }
    }

    // 构建步骤类
    private static class ConstructionStep {
        java.util.List<HuffmanNode> nodes;
        java.util.List<HuffmanNode> forest;
        String description;

        ConstructionStep(java.util.List<HuffmanNode> nodes, java.util.List<HuffmanNode> forest, String description) {
            this.nodes = nodes;
            this.forest = forest;
            this.description = description;
        }
    }

    // 哈夫曼编码类
    private static class HuffmanCode {
        int weight;
        String code;

        HuffmanCode(int weight, String code) {
            this.weight = weight;
            this.code = code;
        }
    }
}