package org.GUI;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;

public class HuffmanTreePanel extends JPanel {
    private HuffmanNode root;
    private JTextField inputField;
    private JTextArea logArea;
    private java.util.List<HuffmanNode> constructionSteps;

    public HuffmanTreePanel() {
        constructionSteps = new ArrayList<>();
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
        inputField.setText("5,3,8,2,1,6"); // 默认值

        JButton buildButton = new JButton("构建哈夫曼树");
        JButton stepButton = new JButton("下一步");
        JButton resetButton = new JButton("重置");

        buildButton.addActionListener(e -> buildHuffmanTree());
        stepButton.addActionListener(e -> nextStep());
        resetButton.addActionListener(e -> reset());

        panel.add(new JLabel("输入权重(逗号分隔):"));
        panel.add(inputField);
        panel.add(buildButton);
        panel.add(stepButton);
        panel.add(resetButton);

        return panel;
    }

    private void buildHuffmanTree() {
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

            // 构建哈夫曼树
            root = buildHuffmanTree(weights);
            constructionSteps.clear();

            // 记录构建步骤
            recordConstructionSteps(new ArrayList<>(weights));

            log("哈夫曼树构建完成");
            log("权重: " + weights);
            repaint();

        } catch (NumberFormatException ex) {
            log("请输入有效的数字，用逗号分隔");
        }
    }

    private HuffmanNode buildHuffmanTree(java.util.List<Integer> weights) {
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>();

        // 创建叶子节点
        for (int weight : weights) {
            queue.offer(new HuffmanNode(weight));
        }

        // 构建哈夫曼树
        while (queue.size() > 1) {
            HuffmanNode left = queue.poll();
            HuffmanNode right = queue.poll();
            HuffmanNode parent = new HuffmanNode(left.weight + right.weight);
            parent.left = left;
            parent.right = right;
            queue.offer(parent);
        }

        return queue.poll();
    }

    private void recordConstructionSteps(java.util.List<Integer> weights) {
        // 这里简化处理，实际应该记录每一步的合并过程
        constructionSteps.add(new HuffmanNode(0)); // 占位
    }

    private void nextStep() {
        // 逐步显示构建过程
        log("下一步构建...");
        repaint();
    }

    private void reset() {
        root = null;
        constructionSteps.clear();
        repaint();
        log("已重置");
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
            drawHuffmanTree(g2d, root, getWidth() / 2, 50, getWidth() / 4);
            drawCodes(g2d);
        } else {
            g2d.setColor(Color.RED);
            g2d.drawString("请先构建哈夫曼树", getWidth() / 2 - 40, getHeight() / 2);
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
        g2d.drawString(weightStr, x - textWidth / 2, y + 5);
    }

    private void drawCodes(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.drawString("哈夫曼编码:", 20, getHeight() - 80);

        Map<Integer, String> codes = new HashMap<>();
        generateCodes(root, "", codes);

        int yPos = getHeight() - 60;
        for (Map.Entry<Integer, String> entry : codes.entrySet()) {
            g2d.drawString("权重 " + entry.getKey() + ": " + entry.getValue(), 20, yPos);
            yPos += 20;
        }
    }

    private void generateCodes(HuffmanNode node, String code, Map<Integer, String> codes) {
        if (node == null) return;

        if (node.left == null && node.right == null) {
            codes.put(node.weight, code.isEmpty() ? "0" : code);
            return;
        }

        generateCodes(node.left, code + "0", codes);
        generateCodes(node.right, code + "1", codes);
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
    }
}