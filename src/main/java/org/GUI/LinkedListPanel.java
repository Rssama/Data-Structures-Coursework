package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 修改说明：删除了转为BST的功能
 */
public class LinkedListPanel extends JPanel {
    private List<Node> nodes;
    private JTextField valueField;
    private JTextField indexField;
    private JTextArea logArea;

    public LinkedListPanel() {
        nodes = new ArrayList<>();
        initializePanel();
    }

    public static class LinkedListState implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Integer> nodeValues;

        public LinkedListState(List<Integer> values) {
            this.nodeValues = new ArrayList<>(values);
        }
    }

    public LinkedListState getCurrentState() {
        List<Integer> values = new ArrayList<>();
        for (Node node : nodes) {
            values.add(node.value);
        }
        return new LinkedListState(values);
    }

    public void restoreFromState(LinkedListState state) {
        if (state == null || state.nodeValues == null) {
            nodes.clear();
            repaint();
            return;
        }

        nodes.clear();
        for (Integer value : state.nodeValues) {
            nodes.add(new Node(value));
        }
        repaint();
        log("从保存状态恢复链表，节点数: " + nodes.size());
    }

    private void initializePanel() {
        setLayout(new BorderLayout());
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

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
        JButton searchButton = new JButton("查找节点");
        JButton clearButton = new JButton("清空链表");
        // 修改：移除转为BST按钮

        addButton.addActionListener(this::addNode);
        insertButton.addActionListener(this::insertNode);
        deleteButton.addActionListener(this::deleteNode);
        searchButton.addActionListener(this::searchNode);
        clearButton.addActionListener(e -> clearList());

        panel.add(new JLabel("值:"));
        panel.add(valueField);
        panel.add(new JLabel("位置:"));
        panel.add(indexField);
        panel.add(addButton);
        panel.add(insertButton);
        panel.add(deleteButton);
        panel.add(searchButton);
        panel.add(clearButton);

        return panel;
    }

    private void addNode(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            if (value < -9999 || value > 9999) {
                log("错误: 数值范围应在 -9999 到 9999 之间");
                return;
            }
            nodes.add(new Node(value));
            valueField.setText("");
            repaint();
            log("添加节点: " + value + " (当前节点数: " + nodes.size() + ")");
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
        }
    }

    private void insertNode(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            int index = Integer.parseInt(indexField.getText().trim());

            if (value < -9999 || value > 9999) {
                log("错误: 数值范围应在 -9999 到 9999 之间");
                return;
            }

            if (index >= 0 && index <= nodes.size()) {
                nodes.add(index, new Node(value));
                repaint();
                log("在位置 " + index + " 插入节点: " + value + " (当前节点数: " + nodes.size() + ")");
            } else {
                log("错误: 位置超出范围: 0 - " + nodes.size());
            }
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的数字");
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
        }
    }

    private void deleteNode(ActionEvent e) {
        try {
            int index = Integer.parseInt(indexField.getText().trim());
            int value = Integer.parseInt(valueField.getText().trim());

            if (index < 0 || index >= nodes.size()) {
                log("错误: 位置超出范围: 0 - " + (nodes.size() - 1));
                return;
            }

            Node nodeToDelete = nodes.get(index);
            if (nodeToDelete.value == value) {
                Node removed = nodes.remove(index);
                repaint();
                log("删除成功: 位置 " + index + " 的节点 " + removed.value + " 已被删除");
            } else {
                log("删除失败: 位置 " + index + " 的节点值是 " + nodeToDelete.value +
                        "，与输入值 " + value + " 不匹配");
            }
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的数字");
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
        }
    }

    private void searchNode(ActionEvent e) {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            boolean found = false;
            int foundIndex = -1;

            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).value == value) {
                    found = true;
                    foundIndex = i;
                    break;
                }
            }

            if (found) {
                log("查找成功: 节点 " + value + " 位于位置 " + foundIndex);
                repaint();
            } else {
                log("查找失败: 未找到值为 " + value + " 的节点");
            }
        } catch (NumberFormatException ex) {
            log("错误: 请输入有效的整数");
        } catch (Exception ex) {
            log("系统错误: " + ex.getMessage());
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
            g2d.setFont(new Font("宋体", Font.BOLD, 16));
            g2d.drawString("链表为空", getWidth() / 2 - 30, getHeight() / 2);
            return;
        }

        int startX = 100;
        int startY = getHeight() / 2;
        int nodeWidth = 60;
        int nodeHeight = 40;
        int spacing = 80;

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("宋体", Font.BOLD, 14));
        g2d.drawString("head", startX - 40, startY - 20);
        g2d.drawLine(startX - 20, startY - 10, startX, startY);

        for (int i = 0; i < nodes.size(); i++) {
            int x = startX + i * (nodeWidth + spacing);
            Node node = nodes.get(i);

            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(x, startY - nodeHeight / 2, nodeWidth, nodeHeight);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, startY - nodeHeight / 2, nodeWidth, nodeHeight);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("宋体", Font.BOLD, 14));
            g2d.drawString(String.valueOf(node.value), x + nodeWidth / 2 - 5, startY);

            g2d.drawLine(x + nodeWidth / 2, startY - nodeHeight / 2,
                    x + nodeWidth / 2, startY + nodeHeight / 2);

            if (i < nodes.size() - 1) {
                int nextX = x + nodeWidth + spacing;
                drawArrow(g2d, x + nodeWidth, startY, nextX, startY);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("宋体", Font.PLAIN, 12));
                g2d.drawString("null", x + nodeWidth + 10, startY);
            }

            g2d.setColor(Color.BLUE);
            g2d.setFont(new Font("宋体", Font.BOLD, 12));
            g2d.drawString("[" + i + "]", x + nodeWidth / 2 - 5, startY - 30);
        }

        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("宋体", Font.PLAIN, 12));
        g2d.drawString("删除操作说明: 需要同时输入位置和值，且位置上的节点值必须与输入值匹配",
                20, getHeight() - 30);
    }

    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.setColor(Color.BLACK);
        g2d.drawLine(x1, y1, x2, y2);

        int arrowSize = 8;
        double angle = Math.atan2(y2 - y1, x2 - x1);

        int x3 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int y3 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
        int x4 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int y4 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));

        g2d.drawLine(x2, y2, x3, y3);
        g2d.drawLine(x2, y2, x4, y4);
    }

    private static class Node implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;

        Node(int value) {
            this.value = value;
        }
    }
}