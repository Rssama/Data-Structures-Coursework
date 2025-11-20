package org.GUI;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据结构可视化模拟器 - 主类 (修改版)
 * 更新说明：
 * 1. 修改了保存/加载逻辑，使用 TXT 明文格式代替二进制序列化。
 * 2. 移除了 StructureSaveData 类，直接处理文本流。
 */
public class DataStructureVisualizer extends JFrame {
    private JTextArea statusArea;
    private JTabbedPane tabbedPane;
    private Map<String, JPanel> panels;

    // 当前活动面板的引用
    private JPanel currentActivePanel;
    private String currentPanelName;

    public JPanel getPanel(String panelKey) {
        return panels.get(panelKey);
    }

    public DataStructureVisualizer() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("数据结构可视化模拟器 - 增强版 (文本存储)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null); // 窗口居中

        panels = new HashMap<>();
        createMainInterface();
        setVisible(true);
    }

    private void createMainInterface() {
        // 创建菜单栏
        createMenuBar();

        // 使用选项卡面板
        tabbedPane = new JTabbedPane();

        // 创建各个面板
        LinkedListPanel linkedListPanel = new LinkedListPanel();
        StackPanel stackPanel = new StackPanel();
        BinaryTreePanel binaryTreePanel = new BinaryTreePanel();
        BSTPanel bstPanel = new BSTPanel();
        HuffmanTreePanel huffmanTreePanel = new HuffmanTreePanel();
        AVLPanel avlPanel = new AVLPanel();

        // 保存面板引用
        panels.put("LinkedList", linkedListPanel);
        panels.put("Stack", stackPanel);
        panels.put("BinaryTree", binaryTreePanel);
        panels.put("BST", bstPanel);
        panels.put("HuffmanTree", huffmanTreePanel);
        panels.put("AVLTree", avlPanel);

        // 添加数据结构面板
        tabbedPane.addTab("链表结构", createTabPanel(linkedListPanel,
                "线性表的链式存储结构\n支持批量添加、插入、删除节点操作"));

        tabbedPane.addTab("栈结构", createTabPanel(stackPanel,
                "后进先出(LIFO)数据结构\n支持批量入栈、出栈、查看栈顶操作"));

        tabbedPane.addTab("二叉树构建", createTabPanel(binaryTreePanel,
                "二叉树构建与遍历\n支持批量添加(层序)和三种遍历方式的动画演示"));

        tabbedPane.addTab("二叉搜索树", createTabPanel(bstPanel,
                "带动画查找的二叉搜索树\n支持批量构建、动画查找、删除节点操作"));

        tabbedPane.addTab("哈夫曼树", createTabPanel(huffmanTreePanel,
                "动态构建哈夫曼树\n支持批量输入权重，手动控制构建过程"));

        tabbedPane.addTab("AVL平衡树", createTabPanel(avlPanel,
                "自平衡二叉搜索树\n支持批量添加，展示插入删除时的旋转平衡操作"));

        // 设置选项卡变化监听器
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                String title = tabbedPane.getTitleAt(selectedIndex);
                currentPanelName = getPanelKeyFromTitle(title);
                currentActivePanel = panels.get(currentPanelName);
                logStatus("切换到: " + title);
            }
        });

        // 初始化当前面板
        currentPanelName = "LinkedList";
        currentActivePanel = linkedListPanel;

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(createStatusPanel(), BorderLayout.SOUTH);

        add(mainPanel);

        // 初始状态消息
        logStatus("=== 数据结构可视化模拟器已启动 ===");
        logStatus("已启用 TXT 明文保存/读取功能");
        logStatus("提示：保存文件将包含结构类型和节点数据");
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem saveItem = new JMenuItem("保存结构到TXT");
        JMenuItem loadItem = new JMenuItem("从TXT加载结构");
        JMenuItem exitItem = new JMenuItem("退出");

        saveItem.addActionListener(this::saveCurrentStructureToText);
        loadItem.addActionListener(this::loadStructureFromText);
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem helpItem = new JMenuItem("使用说明");
        JMenuItem aboutItem = new JMenuItem("关于");

        helpItem.addActionListener(e -> showHelpDialog());
        aboutItem.addActionListener(e -> showAboutDialog());

        helpMenu.add(helpItem);
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * 修改后的保存方法：保存为 TXT 明文
     */
    private void saveCurrentStructureToText(ActionEvent e) {
        if (currentActivePanel == null) {
            logStatus("错误: 没有活动的面板可以保存");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存数据结构 (TXT)");
        fileChooser.setSelectedFile(new File(currentPanelName + "_data.txt"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件 (*.txt)", "txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // 1. 获取数据列表
                List<Integer> dataList = extractDataFromCurrentPanel();

                if (dataList == null) {
                    logStatus("错误: 无法获取当前结构的数据");
                    return;
                }

                // 2. 写入文件
                // 第一行：类型标识
                writer.println(currentPanelName);
                // 第二行：逗号分隔的数据
                String dataStr = dataList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                writer.println(dataStr);

                logStatus("数据结构已保存到: " + file.getName() + " (节点数: " + dataList.size() + ")");

            } catch (IOException ex) {
                logStatus("保存失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 辅助方法：从当前面板的状态对象中提取整数列表
     */
    private List<Integer> extractDataFromCurrentPanel() {
        switch (currentPanelName) {
            case "LinkedList":
                return ((LinkedListPanel) currentActivePanel).getCurrentState().nodeValues;
            case "Stack":
                return ((StackPanel) currentActivePanel).getCurrentState().stackElements;
            case "BinaryTree":
                return ((BinaryTreePanel) currentActivePanel).getCurrentState().nodeValues;
            case "BST":
                return ((BSTPanel) currentActivePanel).getCurrentState().nodeValues;
            case "HuffmanTree":
                // 哈夫曼树只需保存权重即可重建
                return ((HuffmanTreePanel) currentActivePanel).getCurrentState().weights;
            case "AVLTree":
                return ((AVLPanel) currentActivePanel).getCurrentState().nodeValues;
            default:
                return null;
        }
    }

    /**
     * 修改后的加载方法：从 TXT 明文加载
     */
    private void loadStructureFromText(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("加载数据结构 (TXT)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件 (*.txt)", "txt"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try (Scanner scanner = new Scanner(file)) {
                if (!scanner.hasNextLine()) {
                    throw new IOException("文件为空");
                }

                // 1. 读取类型
                String type = scanner.nextLine().trim();

                // 2. 读取数据
                String dataLine = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
                List<Integer> dataList = new ArrayList<>();

                if (!dataLine.isEmpty()) {
                    String[] parts = dataLine.split(",");
                    for (String part : parts) {
                        try {
                            dataList.add(Integer.parseInt(part.trim()));
                        } catch (NumberFormatException ignored) {
                            // 忽略非数字格式
                        }
                    }
                }

                // 3. 切换面板并恢复
                if (panels.containsKey(type)) {
                    switchToPanel(type);
                    restoreDataToPanel(type, dataList);
                    logStatus("已从 " + file.getName() + " 加载结构: " + type + " (节点数: " + dataList.size() + ")");
                } else {
                    logStatus("错误: 未知的数据结构类型 '" + type + "'");
                    JOptionPane.showMessageDialog(this, "未知的文件类型: " + type, "加载错误", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                logStatus("加载失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 辅助方法：将数据列表恢复到指定类型的面板
     */
    private void restoreDataToPanel(String type, List<Integer> data) {
        JPanel panel = panels.get(type);
        switch (type) {
            case "LinkedList":
                ((LinkedListPanel) panel).restoreFromState(new LinkedListPanel.LinkedListState(data));
                break;
            case "Stack":
                ((StackPanel) panel).restoreFromState(new StackPanel.StackState(data));
                break;
            case "BinaryTree":
                ((BinaryTreePanel) panel).restoreFromState(new BinaryTreePanel.BinaryTreeState(data));
                break;
            case "BST":
                ((BSTPanel) panel).restoreFromState(new BSTPanel.BSTState(data));
                break;
            case "HuffmanTree":
                // 哈夫曼树加载时，只传入权重，buildingCompleted 设为 false 以便重新构建
                ((HuffmanTreePanel) panel).restoreFromState(
                        new HuffmanTreePanel.HuffmanTreeState(data, null, false));
                break;
            case "AVLTree":
                ((AVLPanel) panel).restoreFromState(new AVLPanel.AVLTreeState(data));
                break;
        }
    }

    // ================== 界面辅助方法 ==================

    public void switchToPanel(String panelKey) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String title = tabbedPane.getTitleAt(i);
            if (panelKey.equals(getPanelKeyFromTitle(title))) {
                tabbedPane.setSelectedIndex(i);
                currentPanelName = panelKey;
                currentActivePanel = panels.get(panelKey);
                logStatus("切换到: " + title);
                break;
            }
        }
    }

    private String getPanelKeyFromTitle(String title) {
        switch (title) {
            case "链表结构": return "LinkedList";
            case "栈结构": return "Stack";
            case "二叉树构建": return "BinaryTree";
            case "二叉搜索树": return "BST";
            case "哈夫曼树": return "HuffmanTree";
            case "AVL平衡树": return "AVLTree";
            default: return "LinkedList";
        }
    }

    private JPanel createTabPanel(JPanel contentPanel, String description) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea descArea = new JTextArea(description);
        descArea.setEditable(false);
        descArea.setFont(new Font("宋体", Font.PLAIN, 12));
        descArea.setBackground(new Color(240, 245, 255));
        descArea.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.add(descArea, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("操作日志"));
        statusPanel.setPreferredSize(new Dimension(0, 100));

        statusArea = new JTextArea(4, 50);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("宋体", Font.PLAIN, 12));
        statusArea.setBackground(new Color(250, 250, 250));

        JScrollPane scrollPane = new JScrollPane(statusArea);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("清空日志");
        JButton helpButton = new JButton("使用说明");
        JButton exportButton = new JButton("导出日志");

        clearButton.addActionListener(e -> statusArea.setText(""));
        helpButton.addActionListener(e -> showHelpDialog());
        exportButton.addActionListener(e -> exportLog());

        buttonPanel.add(clearButton);
        buttonPanel.add(helpButton);
        buttonPanel.add(exportButton);

        statusPanel.add(scrollPane, BorderLayout.CENTER);
        statusPanel.add(buttonPanel, BorderLayout.SOUTH);

        return statusPanel;
    }

    private void exportLog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出操作日志");
        fileChooser.setSelectedFile(new File("datastructure_log.txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.write(statusArea.getText());
                logStatus("操作日志已导出到: " + file.getName());
            } catch (IOException ex) {
                logStatus("导出日志失败: " + ex.getMessage());
            }
        }
    }

    private void showHelpDialog() {
        String helpMessage =
                "数据结构可视化模拟器 - 使用说明 (V2.2)\n\n" +
                        "通用功能:\n" +
                        "  • 批量添加: 在各面板支持输入逗号分隔的数值(如 5,3,7,1)一次性添加\n" +
                        "  • 文件保存: 菜单栏'保存'可生成TXT明文文件\n" +
                        "  • 文件加载: 菜单栏'加载'可读取TXT文件并自动恢复结构\n\n" +
                        "链表结构:\n" +
                        "  • 批量添加、插入、删除、查找、清空\n\n" +
                        "栈结构:\n" +
                        "  • 批量入栈、出栈、查看栈顶\n\n" +
                        "二叉树构建:\n" +
                        "  • 批量添加(层序)、三种遍历动画\n\n" +
                        "二叉搜索树(BST):\n" +
                        "  • 批量构建、动画查找、转为链表\n\n" +
                        "哈夫曼树:\n" +
                        "  • 批量输入权重，可视化构建过程\n\n" +
                        "AVL平衡树:\n" +
                        "  • 批量插入，自动平衡旋转演示";

        JTextArea helpArea = new JTextArea(helpMessage);
        helpArea.setEditable(false);
        helpArea.setFont(new Font("宋体", Font.PLAIN, 12));
        helpArea.setBackground(new Color(240, 240, 240));
        JScrollPane scrollPane = new JScrollPane(helpArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "使用说明", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAboutDialog() {
        String aboutMessage =
                "数据结构可视化模拟器 v2.2\n\n" +
                        "更新日志:\n" +
                        "• 新增: 所有面板支持批量数据输入\n" +
                        "• 优化: 数据保存格式改为 TXT 明文\n" +
                        "  (格式: 第一行类型，第二行逗号分隔数据)\n" +
                        "• 功能: 支持BST转链表\n\n" +
                        "开发技术:\n" +
                        "• Java Swing\n" +
                        "• IO Stream (Text Processing)\n\n" +
                        "© 2024 数据结构课程设计项目";
        JOptionPane.showMessageDialog(this, aboutMessage, "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    private void logStatus(String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        statusArea.append("[" + timestamp + "] " + message + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new DataStructureVisualizer();
        });
    }
}