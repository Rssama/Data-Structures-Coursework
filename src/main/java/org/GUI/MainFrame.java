package org.GUI;

// src/main/java/org/example/MainFrame.java


import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public MainFrame() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("数据结构可视化模拟器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 创建卡片布局
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 创建不同数据结构的可视化面板
        LinkedListPanel linkedListPanel = new LinkedListPanel();
        StackPanel stackPanel = new StackPanel();
        BinaryTreePanel binaryTreePanel = new BinaryTreePanel();

        mainPanel.add(linkedListPanel, "LinkedList");
        mainPanel.add(stackPanel, "Stack");
        mainPanel.add(binaryTreePanel, "BinaryTree");

        // 创建工具栏
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        String[] structures = {"链表", "栈", "二叉树"};
        for (String structure : structures) {
            JButton button = new JButton(structure);
            button.addActionListener(e -> switchPanel(structure));
            toolBar.add(button);
        }

        return toolBar;
    }

    private void switchPanel(String structure) {
        switch (structure) {
            case "链表":
                cardLayout.show(mainPanel, "LinkedList");
                break;
            case "栈":
                cardLayout.show(mainPanel, "Stack");
                break;
            case "二叉树":
                cardLayout.show(mainPanel, "BinaryTree");
                break;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
