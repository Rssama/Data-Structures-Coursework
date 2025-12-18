package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.QuadCurve2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 链表操作面板 (算法原理动画版)
 * 重点展示：指针（引用）的断开与重连过程
 * 修改说明：所有删除操作都通过位置（索引）进行，不支持值删除
 */
public class LinkedListPanel extends JPanel {
    private List<Node> nodes;
    private JTextField valueField;
    private JTextField indexField;
    private JTextArea logArea;

    // ================== 动画状态机 ==================
    private enum AnimState {
        IDLE,           // 空闲
        TRAVERSING,     // 1. 寻找插入/删除位置的前驱节点
        CREATE_NODE,    // 2. (插入) 创建新节点，悬浮显示
        LINK_NEXT,      // 3. (插入) 新节点.next 指向 后继节点
        LINK_PREV,      // 4. (插入/删除) 前驱.next 指向 新节点/后继节点 (断开旧连接)
        FINALIZE        // 5. 动画结束，重排布局
    }

    private Timer animationTimer;
    private AnimState currentState = AnimState.IDLE;
    private String currentOperation = ""; // "INSERT" or "DELETE"

    // 动画过程变量
    private int targetIndex = -1;     // 目标操作位置
    private int currentIndex = -1;    // 当前遍历到的位置
    private int pendingValue = 0;     // 待插入的值
    private Node tempNode = null;     // 待插入的临时节点对象

    // 绘图常量
    private final int NODE_WIDTH = 60;
    private final int NODE_HEIGHT = 40;
    private final int SPACING = 80;
    private final int START_X = 80;
    private final int START_Y = 200;

    public LinkedListPanel() {
        nodes = new ArrayList<>();
        initializePanel();
    }

    // ================== 序列化支持 ==================
    public static class LinkedListState implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Integer> nodeValues;
        public LinkedListState(List<Integer> values) {
            this.nodeValues = new ArrayList<>(values);
        }
    }

    public LinkedListState getCurrentState() {
        List<Integer> values = new ArrayList<>();
        for (Node node : nodes) values.add(node.value);
        return new LinkedListState(values);
    }

    public void restoreFromState(LinkedListState state) {
        stopAnimation();
        nodes.clear();
        if (state != null && state.nodeValues != null) {
            for (Integer value : state.nodeValues) nodes.add(new Node(value));
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
        valueField = new JTextField(10);
        indexField = new JTextField(5);

        JButton batchAddButton = new JButton("批量添加");
        JButton insertButton = new JButton("插入(动画)");
        JButton deleteButton = new JButton("删除(动画)"); // 按钮文本保持原样
        JButton clearButton = new JButton("清空");

        batchAddButton.addActionListener(e -> batchAddNodes());
        insertButton.addActionListener(this::startInsertAnim);
        deleteButton.addActionListener(this::startDeleteAnim);
        clearButton.addActionListener(e -> clearList());

        panel.add(new JLabel("值:")); panel.add(valueField);
        panel.add(new JLabel("位置(索引):")); panel.add(indexField);
        panel.add(batchAddButton);
        panel.add(insertButton);
        panel.add(deleteButton);
        panel.add(clearButton);

        return panel;
    }

    // ================== 动画控制逻辑 ==================

    private void startInsertAnim(ActionEvent e) {
        if (currentState != AnimState.IDLE) return;
        try {
            int val = Integer.parseInt(valueField.getText().trim());
            int idx = Integer.parseInt(indexField.getText().trim());
            if (idx < 0 || idx > nodes.size()) { log("索引越界"); return; }

            currentOperation = "INSERT";
            pendingValue = val;
            targetIndex = idx;
            currentIndex = -1;

            // 步骤1: 开始遍历寻找前驱 (如果是插在头部，直接跳到创建节点)
            currentState = (idx == 0) ? AnimState.CREATE_NODE : AnimState.TRAVERSING;

            log("=== 开始插入算法演示 ===");
            if(idx == 0) log("目标是头插法，直接创建节点...");
            else log("寻找前驱节点 (index " + (idx-1) + ")...");

            startTimer();
        } catch (Exception ex) { log("输入错误: " + ex.getMessage()); }
    }

    private void startDeleteAnim(ActionEvent e) {
        if (currentState != AnimState.IDLE) return;
        if (nodes.isEmpty()) { log("链表为空"); return; }
        try {
            int idx = Integer.parseInt(indexField.getText().trim());
            if (idx < 0 || idx >= nodes.size()) { log("索引越界"); return; }

            currentOperation = "DELETE";
            targetIndex = idx;
            currentIndex = -1;

            // 步骤1: 开始遍历 (如果是删头，直接跳到重连)
            currentState = (idx == 0) ? AnimState.LINK_PREV : AnimState.TRAVERSING;

            log("=== 开始删除算法演示 ===");
            if(idx == 0) log("删除头节点，准备移动 Head 指针...");
            else log("寻找待删除节点的前驱 (index " + (idx-1) + ")...");

            startTimer();
        } catch (Exception ex) { log("输入错误: " + ex.getMessage()); }
    }

    private void startTimer() {
        animationTimer = new Timer(1000, e -> stepAnimation()); // 1秒一步，看清过程
        animationTimer.start();
        repaint();
    }

    private void stepAnimation() {
        switch (currentState) {
            case TRAVERSING:
                // 遍历直到找到 targetIndex - 1 (前驱)
                if (currentIndex < targetIndex - 1) {
                    currentIndex++;
                    repaint();
                } else {
                    // 找到前驱了，进入下一阶段
                    if ("INSERT".equals(currentOperation)) {
                        currentState = AnimState.CREATE_NODE;
                        log("已找到前驱节点，准备创建新节点对象...");
                    } else {
                        currentState = AnimState.LINK_PREV; // 删除直接进入指针修改
                        log("已定位前驱节点，准备修改指针跳过目标...");
                    }
                    repaint();
                }
                break;

            case CREATE_NODE:
                // 仅插入模式：创建临时节点
                tempNode = new Node(pendingValue);
                currentState = AnimState.LINK_NEXT;
                log("1. createNode(newNode): 内存中创建新节点 " + pendingValue);
                repaint();
                break;

            case LINK_NEXT:
                // 仅插入模式：newNode.next = prev.next
                currentState = AnimState.LINK_PREV;
                String nextVal = (targetIndex < nodes.size()) ? String.valueOf(nodes.get(targetIndex).value) : "null";
                log("2. newNode.next = current.next: 新节点指向 " + nextVal);
                repaint();
                break;

            case LINK_PREV:
                // 插入：prev.next = newNode
                // 删除：prev.next = current.next.next (跳过 current.next)
                currentState = AnimState.FINALIZE;
                if ("INSERT".equals(currentOperation)) {
                    log("3. prev.next = newNode: 前驱指向新节点 (完成插入)");
                } else {
                    if(targetIndex == 0) log("head = head.next: 头指针后移 (断开旧头节点)");
                    else log("prev.next = target.next: 前驱绕过目标节点 (断开连接)");
                }
                repaint();
                break;

            case FINALIZE:
                // 提交数据变更，重置动画
                commitAction();
                break;
        }
    }

    private void commitAction() {
        if ("INSERT".equals(currentOperation)) {
            nodes.add(targetIndex, new Node(pendingValue));
            log("✓ 插入完成: 位置 " + targetIndex + ", 值 " + pendingValue);
        } else if ("DELETE".equals(currentOperation)) {
            int deletedValue = nodes.get(targetIndex).value;
            nodes.remove(targetIndex);
            log("✓ 删除完成: 位置 " + targetIndex + ", 值 " + deletedValue);
        }
        stopAnimation();
        repaint();
    }

    private void stopAnimation() {
        if (animationTimer != null) animationTimer.stop();
        currentState = AnimState.IDLE;
        tempNode = null;
        currentIndex = -1;
    }

    // ================== 核心绘图逻辑 ==================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(2));

        if (nodes.isEmpty() && currentState == AnimState.IDLE) {
            g2d.drawString("链表为空", getWidth()/2, getHeight()/2);
            return;
        }

        // 绘制 Head 指针
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Consolas", Font.BOLD, 14));
        g2d.drawString("HEAD", START_X - 50, START_Y + NODE_HEIGHT/2 + 5);

        // 如果是删除头节点的动画阶段，HEAD 指针指向第二个节点
        if (currentState == AnimState.LINK_PREV && "DELETE".equals(currentOperation) && targetIndex == 0) {
            // 绘制指向 index 1 的弯曲箭头
            drawCurveArrow(g2d, START_X - 10, START_Y + NODE_HEIGHT/2, START_X + NODE_WIDTH + SPACING, START_Y + NODE_HEIGHT/2, Color.MAGENTA);
        } else {
            // 正常 HEAD 指针
            drawArrow(g2d, START_X - 10, START_Y + NODE_HEIGHT/2, START_X, START_Y + NODE_HEIGHT/2, Color.BLACK);
        }

        // ---------------- 绘制现有链表 ----------------
        for (int i = 0; i < nodes.size(); i++) {
            int x = START_X + i * (NODE_WIDTH + SPACING);
            int y = START_Y;

            // 节点颜色判断
            Color boxColor = Color.WHITE;
            Color borderColor = Color.BLACK;

            // 遍历高亮
            if ((currentState == AnimState.TRAVERSING) && i == currentIndex) {
                boxColor = Color.YELLOW;
            }
            // 删除目标高亮 (变红表示即将移除)
            if ("DELETE".equals(currentOperation) && i == targetIndex && currentState != AnimState.IDLE) {
                boxColor = new Color(255, 200, 200); // 浅红
                borderColor = Color.RED;
            }

            // 绘制节点实体
            drawNode(g2d, x, y, String.valueOf(nodes.get(i).value), boxColor, borderColor);
            g2d.setColor(Color.BLUE);
            g2d.drawString(String.valueOf(i), x + NODE_WIDTH/2 - 4, y - 5); // 索引

            // ---------------- 绘制连接线 (指针) ----------------
            if (i < nodes.size() - 1) {
                int startLinkX = x + NODE_WIDTH;
                int endLinkX = x + NODE_WIDTH + SPACING;
                int linkY = y + NODE_HEIGHT / 2;

                // 特殊逻辑：插入或删除时的指针变化
                boolean drawStandardArrow = true;

                // 场景1：删除时，前驱节点的指针跳过目标
                if ("DELETE".equals(currentOperation) && currentState == AnimState.LINK_PREV && i == targetIndex - 1) {
                    // 绘制一根红色的叉号在旧连接上
                    drawArrow(g2d, startLinkX, linkY, endLinkX, linkY, Color.LIGHT_GRAY);
                    drawCross(g2d, startLinkX + SPACING/2, linkY);

                    // 绘制绿色的新连接线（跳跃）
                    int jumpX = endLinkX + NODE_WIDTH + SPACING; // 跳到下下个节点的开头
                    // 如果是删最后一个，则指向null
                    if (targetIndex == nodes.size() - 1) {
                        // 指向 null 暂时不做特殊处理，只是不画线
                    } else {
                        drawCurveArrow(g2d, startLinkX, linkY, x + (NODE_WIDTH+SPACING)*2, linkY + NODE_HEIGHT/2, Color.GREEN);
                    }
                    drawStandardArrow = false;
                }

                // 场景2：插入时，前驱节点的指针断开，指向下方的新节点
                if ("INSERT".equals(currentOperation) && (currentState == AnimState.LINK_PREV || currentState == AnimState.FINALIZE) && i == targetIndex - 1) {
                    // 旧连接变灰
                    drawArrow(g2d, startLinkX, linkY, endLinkX, linkY, Color.LIGHT_GRAY);
                    // 新连接指向下方的新节点
                    int tempNodeX = x + NODE_WIDTH + SPACING / 2 - NODE_WIDTH / 2;
                    int tempNodeY = y + 80;
                    drawArrow(g2d, x + NODE_WIDTH/2, y + NODE_HEIGHT, tempNodeX + NODE_WIDTH/2, tempNodeY, Color.GREEN);
                    drawStandardArrow = false;
                }

                if (drawStandardArrow) {
                    drawArrow(g2d, startLinkX, linkY, endLinkX, linkY, Color.BLACK);
                }
            } else {
                // 尾节点指向 NULL
                g2d.drawString("null", x + NODE_WIDTH + 5, y + NODE_HEIGHT/2 + 5);
            }
        }

        // ---------------- 绘制"悬浮"的临时节点 (插入算法特有) ----------------
        if ("INSERT".equals(currentOperation) && tempNode != null) {
            // 计算悬浮位置：在前驱和后继之间的下方
            int prevX = START_X + (targetIndex - 1) * (NODE_WIDTH + SPACING);
            if (targetIndex == 0) prevX = START_X - (NODE_WIDTH + SPACING); // 头插处理

            int tempX = prevX + NODE_WIDTH + SPACING / 2 - NODE_WIDTH / 2;
            int tempY = START_Y + 80; // 下移显示

            // 绘制新节点
            drawNode(g2d, tempX, tempY, String.valueOf(tempNode.value), Color.GREEN, Color.BLACK);
            g2d.drawString("New", tempX, tempY + NODE_HEIGHT + 15);

            // 动画步骤 3: 绘制新节点指向后继节点的箭头 (newNode.next = prev.next)
            if (currentState.ordinal() >= AnimState.LINK_NEXT.ordinal()) {
                if (targetIndex < nodes.size()) {
                    // 指向原本在 index 处的节点
                    int targetX = START_X + targetIndex * (NODE_WIDTH + SPACING);
                    drawArrow(g2d, tempX + NODE_WIDTH, tempY + NODE_HEIGHT/2, targetX, START_Y + NODE_HEIGHT - 5, Color.MAGENTA);
                } else {
                    g2d.drawString("null", tempX + NODE_WIDTH + 5, tempY + NODE_HEIGHT/2 + 5);
                }
            }

            // 头插法的特殊指针修正
            if (targetIndex == 0 && currentState == AnimState.LINK_PREV) {
                // Head 指向 NewNode
                g2d.setColor(Color.white); // 擦除旧的 Head 线
                g2d.drawLine(START_X-20, START_Y + NODE_HEIGHT/2, START_X, START_Y + NODE_HEIGHT/2);
                // 画新的
                drawArrow(g2d, START_X-20, START_Y + NODE_HEIGHT/2, tempX, tempY + NODE_HEIGHT/2, Color.GREEN);
            }
        }

        // 绘制图例
        drawLegend(g2d);
    }

    // ================== 绘图辅助方法 ==================

    private void drawNode(Graphics2D g, int x, int y, String text, Color fill, Color border) {
        g.setColor(fill);
        g.fillRect(x, y, NODE_WIDTH, NODE_HEIGHT);
        g.setColor(border);
        g.drawRect(x, y, NODE_WIDTH, NODE_HEIGHT);
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        g.drawString(text, x + (NODE_WIDTH - tw)/2, y + (NODE_HEIGHT + fm.getAscent())/2 - 3);

        // 画 next 区域
        g.drawLine(x + NODE_WIDTH - 15, y, x + NODE_WIDTH - 15, y + NODE_HEIGHT);
    }

    private void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2, Color c) {
        g.setColor(c);
        g.drawLine(x1, y1, x2, y2);
        drawArrowHead(g, x1, y1, x2, y2);
    }

    // 绘制贝塞尔曲线箭头 (用于跳跃连接)
    private void drawCurveArrow(Graphics2D g, int x1, int y1, int x2, int y2, Color c) {
        g.setColor(c);
        QuadCurve2D q = new QuadCurve2D.Float(x1, y1, (x1+x2)/2, y1 - 50, x2, y2);
        g.draw(q);
        // 简化的箭头头，基于结束点方向
        drawArrowHead(g, (int)((x1+x2)/2), y1-50, x2, y2);
    }

    private void drawArrowHead(Graphics2D g, int x1, int y1, int x2, int y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 8;
        int x3 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int y3 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
        int x4 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int y4 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));
        g.drawLine(x2, y2, x3, y3);
        g.drawLine(x2, y2, x4, y4);
    }

    private void drawCross(Graphics2D g, int x, int y) {
        g.setColor(Color.RED);
        g.drawLine(x-5, y-5, x+5, y+5);
        g.drawLine(x+5, y-5, x-5, y+5);
    }

    private void drawLegend(Graphics2D g) {
        int y = getHeight() - 30;
        int x = 20;
        g.setFont(new Font("宋体", Font.PLAIN, 12));

        g.setColor(Color.YELLOW); g.fillRect(x, y, 15, 15); g.setColor(Color.BLACK); g.drawRect(x,y,15,15);
        g.drawString("遍历中", x+20, y+12); x+=80;

        g.setColor(Color.GREEN); g.fillRect(x, y, 15, 15); g.setColor(Color.BLACK); g.drawRect(x,y,15,15);
        g.drawString("新节点/新连接", x+20, y+12); x+=120;

        g.setColor(Color.MAGENTA); g.drawLine(x, y+7, x+15, y+7);
        g.drawString("指向后继", x+20, y+12);
    }

    // ================== 杂项 ==================
    private void batchAddNodes() {
        String input = valueField.getText().trim();
        if(input.isEmpty()) {
            log("错误: 请输入要添加的值");
            return;
        }

        String[] parts = input.split("[,，]");
        int successCount = 0;

        for(String p : parts) {
            try {
                int val = Integer.parseInt(p.trim());
                nodes.add(new Node(val));
                successCount++;
            } catch(Exception e) {
                log("警告: '" + p + "' 不是有效的整数，已跳过");
            }
        }

        valueField.setText("");
        repaint();
        log("批量添加完成: 成功添加 " + successCount + " 个节点");
    }

    private void clearList() {
        stopAnimation();
        nodes.clear();
        repaint();
        log("链表已清空");
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ================== DSL支持方法 ==================

    // 新增方法：通过位置和值插入节点（供DSL调用）
    public void insertByIndex(int index, int value) {
        if (index < 0 || index > nodes.size()) {
            log("索引越界: " + index + " (链表大小: " + nodes.size() + ")");
            return;
        }

        // 设置输入框并触发动画插入
        valueField.setText(String.valueOf(value));
        indexField.setText(String.valueOf(index));
        startInsertAnim(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }

    // 新增方法：批量添加节点（供DSL调用）
    public void batchAdd(String values) {
        if (values == null || values.trim().isEmpty()) {
            log("错误: 批量添加需要提供值列表");
            return;
        }

        // 设置值输入框并触发批量添加
        valueField.setText(values);
        batchAddNodes();
    }

    // 新增方法：通过位置删除节点（供DSL调用）
    public void deleteByIndex(int index) {
        if (index < 0 || index >= nodes.size()) {
            log("索引越界: " + index + " (链表大小: " + nodes.size() + ")");
            return;
        }

        // 设置索引字段并触发动画删除
        indexField.setText(String.valueOf(index));
        startDeleteAnim(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }

    // 新增方法：清空链表（供DSL调用）
    public void clearLinkedList() {
        clearList();
    }

    private static class Node implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;
        Node(int value) { this.value = value; }
    }
}
