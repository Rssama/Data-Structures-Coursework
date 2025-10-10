package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class LinkedListPanel extends JPanel {
    private List<Node> nodes;
    private JTextField valueField;
    private JTextField indexField;
    private JTextArea logArea;

    public LinkedListPanel() {
        nodes = new ArrayList<>();
        initializePanel();
    }

    private void initializePanel() {
        setLayout(new BorderLayout());

        // 控制面板
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        // 日志区域
        logArea = new JTextArea(5, 30);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        valueField = new JTextField(10);
        indexField = new JTextField(5);

        JButton addButton = new JButton("添加节点");
        JButton insertButton = new JButton("插入节点");
        JButton deleteButton = new JButton("删除节点");
        JButton clearButton = new JButton("清空链表");

        addButton.addActionListener(this::addNode);
        insertButton.addActionListener(this::insertNode);
        deleteButton.addActionListener(this::deleteNode);
        clearButton.addActionListener(e -> clearList());

        panel.add(new JLabel("值:"));
        panel.add(valueField);
        panel.add(new JLabel("位置:"));
        panel.add(indexField);
        panel.add(addButton);
        panel.add(insertButton);
        panel.add(deleteButton);
        panel.add(clearButton);

        return panel;
    }

    private void addNode(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText());
            nodes.add(new Node(value));
            valueField.setText("");
            repaint();
            log("添加节点: " + value);
        } catch (NumberFormatException ex) {
            log("请输入有效的数字");
        }
    }

    private void insertNode(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText());
            int index = Integer.parseInt(indexField.getText());

            if (index >= 0 && index <= nodes.size()) {
                nodes.add(index, new Node(value));
                repaint();
                log("在位置 " + index + " 插入节点: " + value);
            } else {
                log("位置超出范围: 0 - " + nodes.size());
            }
        } catch (NumberFormatException ex) {
            log("请输入有效的数字");
        }
    }

    private void deleteNode(ActionEvent e) {
        try {
            int index = Integer.parseInt(indexField.getText());
            if (index >= 0 && index < nodes.size()) {
                Node removed = nodes.remove(index);
                repaint();
                log("删除位置 " + index + " 的节点: " + removed.value);
            } else {
                log("位置超出范围: 0 - " + (nodes.size() - 1));
            }
        } catch (NumberFormatException ex) {
            log("请输入有效的数字");
        }
    }

    private void clearList() {
        nodes.clear();
        repaint();
        log("清空链表");
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

        drawLinkedList(g2d);
    }

    private void drawLinkedList(Graphics2D g2d) {
        if (nodes.isEmpty()) {
            g2d.setColor(Color.RED);
            g2d.drawString("链表为空", getWidth() / 2 - 30, getHeight() / 2);
            return;
        }

        int startX = 100;
        int startY = getHeight() / 2;
        int nodeWidth = 60;
        int nodeHeight = 40;
        int spacing = 80;

        // 绘制头指针
        g2d.setColor(Color.BLACK);
        g2d.drawString("head", startX - 40, startY - 20);
        g2d.drawLine(startX - 20, startY - 10, startX, startY);

        for (int i = 0; i < nodes.size(); i++) {
            int x = startX + i * (nodeWidth + spacing);
            Node node = nodes.get(i);

            // 绘制节点矩形
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(x, startY - nodeHeight / 2, nodeWidth, nodeHeight);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, startY - nodeHeight / 2, nodeWidth, nodeHeight);

            // 绘制数据
            g2d.drawString(String.valueOf(node.value), x + nodeWidth / 2 - 5, startY);

            // 绘制指针区域分隔线
            g2d.drawLine(x + nodeWidth / 2, startY - nodeHeight / 2,
                    x + nodeWidth / 2, startY + nodeHeight / 2);

            // 绘制指针箭头（如果不是最后一个节点）
            if (i < nodes.size() - 1) {
                int nextX = x + nodeWidth + spacing;
                drawArrow(g2d, x + nodeWidth, startY, nextX, startY);
            } else {
                // 最后一个节点的指针为null
                g2d.drawString("null", x + nodeWidth + 10, startY);
            }

            // 显示位置索引
            g2d.setColor(Color.BLUE);
            g2d.drawString("[" + i + "]", x + nodeWidth / 2 - 5, startY - 30);
        }
    }

    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.setColor(Color.BLACK);
        g2d.drawLine(x1, y1, x2, y2);

        // 绘制箭头头部
        int arrowSize = 8;
        double angle = Math.atan2(y2 - y1, x2 - x1);

        int x3 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int y3 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
        int x4 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int y4 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));

        g2d.drawLine(x2, y2, x3, y3);
        g2d.drawLine(x2, y2, x4, y4);
    }

    // 内部节点类
    private static class Node {
        int value;

        Node(int value) {
            this.value = value;
        }
    }

}