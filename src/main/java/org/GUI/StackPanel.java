package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.Stack;

public class StackPanel extends JPanel {
    private Stack<Integer> stack;
    private JTextField valueField;
    private JTextArea logArea;

    public StackPanel() {
        stack = new Stack<>();
        initializePanel();
    }

    // 序列化状态类
    public static class StackState implements Serializable {
        private static final long serialVersionUID = 1L;
        public java.util.List<Integer> stackElements;

        public StackState(java.util.List<Integer> elements) {
            this.stackElements = new java.util.ArrayList<>(elements);
        }
    }

    // 获取当前状态
    public StackState getCurrentState() {
        return new StackState(stack);
    }

    // 从状态恢复
    public void restoreFromState(StackState state) {
        if (state == null) return;

        stack.clear();
        for (Integer value : state.stackElements) {
            stack.push(value);
        }
        repaint();
        log("从保存状态恢复栈，元素数: " + stack.size());
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

        JButton pushButton = new JButton("入栈");
        JButton popButton = new JButton("出栈");
        JButton peekButton = new JButton("查看栈顶");
        JButton clearButton = new JButton("清空栈");

        pushButton.addActionListener(this::pushOperation);
        popButton.addActionListener(e -> popOperation());
        peekButton.addActionListener(e -> peekOperation());
        clearButton.addActionListener(e -> clearStack());

        panel.add(new JLabel("值:"));
        panel.add(valueField);
        panel.add(pushButton);
        panel.add(popButton);
        panel.add(peekButton);
        panel.add(clearButton);

        return panel;
    }

    private void pushOperation(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText());
            stack.push(value);
            valueField.setText("");
            repaint();
            log("入栈: " + value);
        } catch (NumberFormatException ex) {
            log("请输入有效的数字");
        }
    }

    private void popOperation() {
        if (!stack.isEmpty()) {
            int value = stack.pop();
            repaint();
            log("出栈: " + value);
        } else {
            log("栈为空，无法出栈");
        }
    }

    private void peekOperation() {
        if (!stack.isEmpty()) {
            log("栈顶元素: " + stack.peek());
        } else {
            log("栈为空");
        }
    }

    private void clearStack() {
        stack.clear();
        repaint();
        log("清空栈");
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

        drawStack(g2d);
    }

    private void drawStack(Graphics2D g2d) {
        int stackBaseX = getWidth() / 2 - 50;
        int stackBaseY = getHeight() - 100;
        int elementWidth = 100;
        int elementHeight = 40;
        int spacing = 5;

        // 绘制栈的轮廓
        g2d.setColor(Color.BLACK);
        g2d.drawRect(stackBaseX, 200, elementWidth, stackBaseY - 200);

        // 绘制栈底标识
        g2d.drawString("栈底", stackBaseX - 40, stackBaseY + 10);

        if (stack.isEmpty()) {
            g2d.setColor(Color.RED);
            g2d.drawString("栈为空", stackBaseX + 30, stackBaseY - 50);
            return;
        }

        // 绘制栈中的元素
        for (int i = 0; i < stack.size(); i++) {
            int value = stack.get(i);
            int y = stackBaseY - (i + 1) * (elementHeight + spacing);

            // 绘制元素矩形
            g2d.setColor(Color.ORANGE);
            g2d.fillRect(stackBaseX, y, elementWidth, elementHeight);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(stackBaseX, y, elementWidth, elementHeight);

            // 绘制元素值
            g2d.drawString(String.valueOf(value), stackBaseX + elementWidth / 2 - 5, y + elementHeight / 2 + 5);

            // 如果是栈顶元素，特殊标记
            if (i == stack.size() - 1) {
                g2d.setColor(Color.RED);
                g2d.drawString("↑ 栈顶", stackBaseX + elementWidth + 10, y + elementHeight / 2);
            }
        }
    }
}