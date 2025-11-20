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
 * 数据结构可视化模拟器 - 主类 (Ollama AI 增强版)
 */
public class DataStructureVisualizer extends JFrame {
    private JTextArea statusArea;
    private JTabbedPane tabbedPane;
    private Map<String, JPanel> panels;
    private JPanel currentActivePanel;
    private String currentPanelName;

    // AI 组件
    private JTextArea chatHistoryArea;
    private JTextField chatInputField;
    private JButton sendButton;

    public JPanel getPanel(String panelKey) {
        return panels.get(panelKey);
    }

    public DataStructureVisualizer() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("数据结构可视化 - AI增强版 (本地Ollama)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        panels = new HashMap<>();
        createMainInterface();
        setVisible(true);
    }

    private void createMainInterface() {
        createMenuBar();

        JPanel leftPanel = new JPanel(new BorderLayout());
        tabbedPane = new JTabbedPane();
        initializePanels();

        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                String title = tabbedPane.getTitleAt(selectedIndex);
                currentPanelName = getPanelKeyFromTitle(title);
                currentActivePanel = panels.get(currentPanelName);
                logStatus("切换到: " + title);
            }
        });

        leftPanel.add(tabbedPane, BorderLayout.CENTER);
        leftPanel.add(createStatusPanel(), BorderLayout.SOUTH);

        // 创建右侧 AI 聊天区
        JPanel rightPanel = createAIChatPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(1000);
        splitPane.setResizeWeight(0.75);

        add(splitPane);

        currentPanelName = "LinkedList";
        currentActivePanel = panels.get("LinkedList");

        logStatus("=== 本地 AI 服务已连接 ===");
        logStatus("提示: 请确保 Ollama 已运行且已拉取 deepseek 模型");
    }

    private void initializePanels() {
        LinkedListPanel linkedListPanel = new LinkedListPanel();
        StackPanel stackPanel = new StackPanel();
        BinaryTreePanel binaryTreePanel = new BinaryTreePanel();
        BSTPanel bstPanel = new BSTPanel();
        HuffmanTreePanel huffmanTreePanel = new HuffmanTreePanel();
        AVLPanel avlPanel = new AVLPanel();

        panels.put("LinkedList", linkedListPanel);
        panels.put("Stack", stackPanel);
        panels.put("BinaryTree", binaryTreePanel);
        panels.put("BST", bstPanel);
        panels.put("HuffmanTree", huffmanTreePanel);
        panels.put("AVLTree", avlPanel);

        tabbedPane.addTab("链表结构", createTabPanel(linkedListPanel, "线性表链式存储"));
        tabbedPane.addTab("栈结构", createTabPanel(stackPanel, "LIFO栈结构"));
        tabbedPane.addTab("二叉树构建", createTabPanel(binaryTreePanel, "普通二叉树构建"));
        tabbedPane.addTab("二叉搜索树", createTabPanel(bstPanel, "BST查找与构建"));
        tabbedPane.addTab("哈夫曼树", createTabPanel(huffmanTreePanel, "哈夫曼编码树"));
        tabbedPane.addTab("AVL平衡树", createTabPanel(avlPanel, "AVL自平衡树"));
    }

    // ================== AI 聊天逻辑 ==================

    private JPanel createAIChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("DeepSeek 助手 (1.7B)"));

        chatHistoryArea = new JTextArea();
        chatHistoryArea.setEditable(false);
        chatHistoryArea.setLineWrap(true);
        chatHistoryArea.setWrapStyleWord(true);
        chatHistoryArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        chatInputField = new JTextField();
        chatInputField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        sendButton = new JButton("发送");

        inputPanel.add(chatInputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        sendButton.addActionListener(e -> handleSendMessage());
        chatInputField.addActionListener(e -> handleSendMessage());

        panel.add(new JScrollPane(chatHistoryArea), BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        appendChat("System", "你好！我是本地 DeepSeek 助手。\n请输入指令，例如：\n“帮我创建一个包含10,5,8的BST”\n“清空栈”");

        return panel;
    }

    private void handleSendMessage() {
        String text = chatInputField.getText().trim();
        if (text.isEmpty()) return;

        appendChat("You", text);
        chatInputField.setText("");
        sendButton.setEnabled(false);
        logStatus("正在思考...");

        LLMService.sendRequest(text, new LLMService.LLMCallback() {
            @Override
            public void onResponse(String response) {
                SwingUtilities.invokeLater(() -> {
                    appendChat("AI", response); // 显示原始回复以便调试
                    executeDSL(response);       // 执行指令
                    sendButton.setEnabled(true);
                    logStatus("AI 响应完成");
                });
            }

            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    appendChat("System", "错误: " + error);
                    sendButton.setEnabled(true);
                });
            }
        });
    }

    private void appendChat(String role, String message) {
        chatHistoryArea.append(role + ": " + message + "\n\n");
        chatHistoryArea.setCaretPosition(chatHistoryArea.getDocument().getLength());
    }

    /**
     * 解析 DSL (增强版容错)
     */
    private void executeDSL(String responseText) {
        try {
            // 1.7B 模型可能会有多余的标点或换行，尝试提取最像指令的一行
            String commandLine = "";
            String[] lines = responseText.split("\n");
            for (String line : lines) {
                line = line.trim();
                // 寻找包含两个冒号的行，且全大写字母开头
                if (line.contains(":") && line.split(":").length >= 2) {
                    commandLine = line;
                    break;
                }
            }

            if (commandLine.isEmpty()) {
                // 如果没找到明显的一行，尝试直接解析全文
                commandLine = responseText.trim();
            }

            // 去掉可能存在的末尾句号
            if (commandLine.endsWith(".")) commandLine = commandLine.substring(0, commandLine.length() - 1);

            String[] parts = commandLine.split(":");
            if (parts.length < 2) {
                logStatus("AI返回的格式无法识别，请重试");
                return;
            }

            String target = parts[0].trim().toUpperCase();
            String action = parts[1].trim().toUpperCase();
            String data = parts.length > 2 ? parts[2].trim() : "";

            // 映射面板
            String panelKey = mapTargetToPanelKey(target);
            if (panelKey == null) {
                logStatus("未知目标结构: " + target);
                return;
            }

            switchToPanel(panelKey);
            performUiAction(currentActivePanel, action, data);

        } catch (Exception e) {
            logStatus("执行指令失败: " + e.getMessage());
        }
    }

    private String mapTargetToPanelKey(String target) {
        // 增加一些别名容错
        if (target.contains("LINKED")) return "LinkedList";
        if (target.contains("STACK")) return "Stack";
        if (target.contains("BST")) return "BST";
        if (target.contains("BINARY")) return "BinaryTree";
        if (target.contains("AVL")) return "AVLTree";
        if (target.contains("HUFFMAN")) return "HuffmanTree";
        return null;
    }

    private void performUiAction(JPanel panel, String action, String data) {
        JTextField valueField = findComponent(panel, JTextField.class);

        // 如果数据字段不为空，先填入数据
        if (valueField != null && !data.isEmpty() && !data.equals("NULL")) {
            valueField.setText(data);
        }

        String buttonText = mapActionToButtonText(action, panel);
        JButton targetButton = findButtonByText(panel, buttonText);

        if (targetButton != null) {
            logStatus("自动点击: [" + buttonText + "] 参数: " + data);
            targetButton.doClick();
        } else {
            logStatus("未找到按钮: " + buttonText + " (动作: " + action + ")");
        }
    }

    private String mapActionToButtonText(String action, JPanel panel) {
        switch (action) {
            case "BATCH_ADD": return "批量添加";
            case "BATCH": return "批量添加"; // 容错
            case "ADD":
                if (panel instanceof StackPanel) return "入栈"; // 栈特殊处理
                return "添加节点";
            case "PUSH": return "入栈";
            case "DELETE":
                if (panel instanceof StackPanel) return "出栈";
                return "删除节点";
            case "POP": return "出栈";
            case "SEARCH":
                // 优先匹配动画查找
                if (findButtonByText(panel, "动画查找") != null) return "动画查找";
                return "查找节点";
            case "CLEAR":
                if (panel instanceof StackPanel) return "清空栈";
                if (panel instanceof LinkedListPanel) return "清空链表";
                return "清空树";
            default: return action;
        }
    }

    // 递归查找组件
    private <T extends Component> T findComponent(Container container, Class<T> clazz) {
        for (Component comp : container.getComponents()) {
            if (clazz.isInstance(comp)) {
                // 简单的启发式：找比较宽的输入框作为主输入框
                if (comp instanceof JTextField && ((JTextField)comp).getColumns() > 5) {
                    return clazz.cast(comp);
                }
            } else if (comp instanceof Container) {
                T result = findComponent((Container) comp, clazz);
                if (result != null) return result;
            }
        }
        return null;
    }

    private JButton findButtonByText(Container container, String partialText) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                if (btn.getText() != null && btn.getText().contains(partialText)) {
                    return btn;
                }
            } else if (comp instanceof Container) {
                JButton result = findButtonByText((Container) comp, partialText);
                if (result != null) return result;
            }
        }
        return null;
    }

    // ================== 基础功能保持不变 ==================

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
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

        JMenu helpMenu = new JMenu("帮助");
        JMenuItem helpItem = new JMenuItem("使用说明");
        helpMenu.add(helpItem);
        helpItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "请输入自然语言控制数据结构\n确保Ollama服务已开启"));

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void saveCurrentStructureToText(ActionEvent e) {
        if (currentActivePanel == null) return;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(currentPanelName + "_data.txt"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                List<Integer> data = extractDataFromCurrentPanel();
                if (data != null) {
                    writer.println(currentPanelName);
                    writer.println(data.stream().map(String::valueOf).collect(Collectors.joining(",")));
                    logStatus("保存成功");
                }
            } catch (Exception ex) { logStatus("保存失败: " + ex.getMessage()); }
        }
    }

    private void loadStructureFromText(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (Scanner scanner = new Scanner(fileChooser.getSelectedFile())) {
                if (scanner.hasNextLine()) {
                    String type = scanner.nextLine().trim();
                    String dataLine = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
                    if (panels.containsKey(type)) {
                        switchToPanel(type);
                        List<Integer> dataList = new ArrayList<>();
                        if (!dataLine.isEmpty()) {
                            for (String s : dataLine.split(",")) {
                                try { dataList.add(Integer.parseInt(s.trim())); } catch(Exception ignored){}
                            }
                        }
                        restoreDataToPanel(type, dataList);
                        logStatus("加载成功: " + type);
                    }
                }
            } catch (Exception ex) { logStatus("加载失败: " + ex.getMessage()); }
        }
    }

    private List<Integer> extractDataFromCurrentPanel() {
        switch (currentPanelName) {
            case "LinkedList": return ((LinkedListPanel) currentActivePanel).getCurrentState().nodeValues;
            case "Stack": return ((StackPanel) currentActivePanel).getCurrentState().stackElements;
            case "BinaryTree": return ((BinaryTreePanel) currentActivePanel).getCurrentState().nodeValues;
            case "BST": return ((BSTPanel) currentActivePanel).getCurrentState().nodeValues;
            case "HuffmanTree": return ((HuffmanTreePanel) currentActivePanel).getCurrentState().weights;
            case "AVLTree": return ((AVLPanel) currentActivePanel).getCurrentState().nodeValues;
            default: return null;
        }
    }

    private void restoreDataToPanel(String type, List<Integer> data) {
        JPanel panel = panels.get(type);
        switch (type) {
            case "LinkedList": ((LinkedListPanel) panel).restoreFromState(new LinkedListPanel.LinkedListState(data)); break;
            case "Stack": ((StackPanel) panel).restoreFromState(new StackPanel.StackState(data)); break;
            case "BinaryTree": ((BinaryTreePanel) panel).restoreFromState(new BinaryTreePanel.BinaryTreeState(data)); break;
            case "BST": ((BSTPanel) panel).restoreFromState(new BSTPanel.BSTState(data)); break;
            case "HuffmanTree": ((HuffmanTreePanel) panel).restoreFromState(new HuffmanTreePanel.HuffmanTreeState(data, null, false)); break;
            case "AVLTree": ((AVLPanel) panel).restoreFromState(new AVLPanel.AVLTreeState(data)); break;
        }
    }

    public void switchToPanel(String panelKey) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String title = tabbedPane.getTitleAt(i);
            if (panelKey.equals(getPanelKeyFromTitle(title))) {
                tabbedPane.setSelectedIndex(i);
                currentPanelName = panelKey;
                currentActivePanel = panels.get(panelKey);
                break;
            }
        }
    }

    private String getPanelKeyFromTitle(String title) {
        if (title.contains("链表")) return "LinkedList";
        if (title.contains("栈")) return "Stack";
        if (title.contains("二叉搜索树")) return "BST";
        if (title.contains("二叉树")) return "BinaryTree";
        if (title.contains("哈夫曼")) return "HuffmanTree";
        if (title.contains("AVL")) return "AVLTree";
        return "LinkedList";
    }

    private JPanel createTabPanel(JPanel contentPanel, String description) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea descArea = new JTextArea(description);
        descArea.setEditable(false);
        descArea.setBackground(new Color(240, 245, 255));
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
        statusPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        return statusPanel;
    }

    private void logStatus(String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        statusArea.append("[" + timestamp + "] " + message + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(DataStructureVisualizer::new);
    }
}