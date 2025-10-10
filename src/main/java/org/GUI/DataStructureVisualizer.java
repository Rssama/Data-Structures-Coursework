package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据结构可视化模拟器 - 主类
 * 使用Swing默认外观，避免复杂的外观设置
 */
public class DataStructureVisualizer extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextArea statusArea;

    public DataStructureVisualizer() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("数据结构可视化模拟器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null); // 窗口居中

        // 创建主界面
        createMainPanel();
        createNavigationPanel();
        createStatusPanel();

        setVisible(true);
    }

    private void createMainPanel() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 直接使用现有的面板类，不添加额外装饰
        mainPanel.add(new LinkedListPanel(), "LINKED_LIST");
        mainPanel.add(new StackPanel(), "STACK");
        mainPanel.add(new BSTPanel(), "BINARY_TREE");

        add(mainPanel, BorderLayout.CENTER);
    }

    private void createNavigationPanel() {
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        navPanel.setBorder(BorderFactory.createTitledBorder("选择数据结构"));

        JButton listButton = new JButton("链表");
        JButton stackButton = new JButton("栈");
        JButton treeButton = new JButton("二叉树");

        // 设置按钮大小一致
        Dimension buttonSize = new Dimension(100, 35);
        listButton.setPreferredSize(buttonSize);
        stackButton.setPreferredSize(buttonSize);
        treeButton.setPreferredSize(buttonSize);

        listButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "LINKED_LIST");
            logStatus("切换到链表可视化");
        });

        stackButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "STACK");
            logStatus("切换到栈可视化");
        });

        treeButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "BINARY_TREE");
            logStatus("切换到二叉树可视化");
        });

        navPanel.add(listButton);
        navPanel.add(stackButton);
        navPanel.add(treeButton);

        // 添加帮助按钮
        JButton helpButton = new JButton("帮助");
        helpButton.setPreferredSize(buttonSize);
        helpButton.addActionListener(e -> showSimpleHelp());
        navPanel.add(helpButton);

        add(navPanel, BorderLayout.NORTH);
    }

    private void createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("操作日志"));

        statusArea = new JTextArea(4, 50);
        statusArea.setEditable(false);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(statusArea);
        statusPanel.add(scrollPane, BorderLayout.CENTER);

        add(statusPanel, BorderLayout.SOUTH);

        // 初始状态消息
        logStatus("应用程序已启动");
        logStatus("请从上方选择要可视化的数据结构");
    }

    private void showSimpleHelp() {
        String helpMessage =
                "数据结构可视化模拟器\n\n" +
                        "使用说明:\n" +
                        "1. 点击上方按钮选择要可视化的数据结构\n" +
                        "2. 在对应的面板中进行操作:\n" +
                        "   - 链表: 添加、插入、删除节点\n" +
                        "   - 栈: 入栈、出栈操作\n" +
                        "   - 二叉树: 构建二叉搜索树\n" +
                        "3. 所有操作结果会实时可视化显示\n" +
                        "4. 操作记录显示在下方日志区域";

        JOptionPane.showMessageDialog(this,
                helpMessage,
                "使用说明",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void logStatus(String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        statusArea.append("[" + timestamp + "] " + message + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        // 最简单的启动方式，不使用任何外观设置
        // 直接在主线程中创建和显示GUI（对于简单应用足够了）
        new DataStructureVisualizer();
    }
}