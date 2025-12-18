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
 * 数据结构可视化模拟器 - 主类 (Ollama DeepSeek 1.5B 适配版)
 * 新增功能：支持直接输入DSL指令执行，无需经过AI模型
 * 修改说明：修复链表删除问题，统一使用位置(索引)删除
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
    private JCheckBox directDSLCheckBox; // 新增：DSL直接执行复选框

    public JPanel getPanel(String panelKey) {
        return panels.get(panelKey);
    }

    public DataStructureVisualizer() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("数据结构可视化 - AI增强版 (支持直接DSL指令)");
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

        logStatus("=== 系统初始化完成 ===");
        logStatus("提示1: 勾选'直接DSL'可直接输入指令（格式: 目标:动作:数据）");
        logStatus("提示2: 不勾选则使用AI解析自然语言");
        logStatus("注意: 链表删除使用位置(索引)而不是值，例如 LINKEDLIST:DELETE:0 删除第一个节点");
    }

    // ================== AI 聊天逻辑 (增强版：支持直接DSL) ==================

    private JPanel createAIChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("DeepSeek 指令台 / DSL直接执行"));

        // 创建顶部控制面板
        JPanel topControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 新增：直接DSL执行复选框
        directDSLCheckBox = new JCheckBox("直接DSL（跳过AI）");
        directDSLCheckBox.setToolTipText("勾选后，输入框将直接执行DSL指令，格式: 目标:动作:数据");
        directDSLCheckBox.setSelected(false);
        directDSLCheckBox.addActionListener(e -> {
            boolean selected = directDSLCheckBox.isSelected();
            chatInputField.setToolTipText(selected ?
                    "输入DSL指令，格式: 目标:动作:数据 (如: BST:BATCH_ADD:5,3,7)" :
                    "输入自然语言指令 (如: 建立BST 5,3,7)");
            sendButton.setText(selected ? "执行DSL" : "发送AI");
            logStatus(selected ? "切换到直接DSL模式" : "切换到AI自然语言模式");
        });

        JButton helpButton = new JButton("DSL语法帮助");
        helpButton.addActionListener(e -> showDSLHelp());

        topControlPanel.add(directDSLCheckBox);
        topControlPanel.add(helpButton);

        chatHistoryArea = new JTextArea();
        chatHistoryArea.setEditable(false);
        chatHistoryArea.setLineWrap(true);
        chatHistoryArea.setWrapStyleWord(true);
        chatHistoryArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        chatInputField = new JTextField();
        chatInputField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        chatInputField.setToolTipText("输入自然语言指令 (如: 建立BST 5,3,7)");

        sendButton = new JButton("发送AI");

        // 新增：DSL指令历史下拉框
        JComboBox<String> dslHistoryCombo = new JComboBox<>();
        dslHistoryCombo.setEditable(false);
        dslHistoryCombo.setToolTipText("选择历史DSL指令");
        dslHistoryCombo.addActionListener(e -> {
            String selected = (String) dslHistoryCombo.getSelectedItem();
            if (selected != null && !selected.isEmpty()) {
                chatInputField.setText(selected);
                chatInputField.requestFocus();
            }
        });

        JPanel inputBottomPanel = new JPanel(new BorderLayout());
        inputBottomPanel.add(sendButton, BorderLayout.EAST);
        inputBottomPanel.add(new JLabel("历史: "), BorderLayout.WEST);
        inputBottomPanel.add(dslHistoryCombo, BorderLayout.CENTER);

        inputPanel.add(chatInputField, BorderLayout.CENTER);
        inputPanel.add(inputBottomPanel, BorderLayout.SOUTH);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 绑定事件
        sendButton.addActionListener(e -> handleUserInput(dslHistoryCombo));
        chatInputField.addActionListener(e -> handleUserInput(dslHistoryCombo));

        panel.add(topControlPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(chatHistoryArea), BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        appendChat("System", "系统就绪。支持两种模式:\n" +
                "1. AI模式(默认): 输入自然语言，如'建立BST 5,3,7'\n" +
                "2. DSL模式(勾选复选框): 直接输入DSL指令，格式'目标:动作:数据'\n" +
                "示例: BST:BATCH_ADD:5,3,7 或 LINKEDLIST:DELETE:0 (删除第一个节点)");

        return panel;
    }

    private void showDSLHelp() {
        String helpText = "DSL (Domain Specific Language) 指令格式:\n\n" +
                "基本格式: [目标]:[动作]:[数据]\n\n" +
                "目标 (不区分大小写):\n" +
                "  LINKEDLIST  - 链表 (重要: 删除使用位置索引，不是值)\n" +
                "  STACK       - 栈\n" +
                "  BST         - 二叉搜索树\n" +
                "  BINARYTREE  - 普通二叉树\n" +
                "  AVL         - AVL平衡树\n" +
                "  HUFFMAN     - 哈夫曼树\n\n" +
                "动作 (不区分大小写):\n" +
                "  BATCH_ADD   - 批量添加 (数据用逗号分隔)\n" +
                "  ADD         - 添加单个节点\n" +
                "  DELETE      - 删除节点 (链表使用位置索引，其他使用值)\n" +
                "  SEARCH      - 查找节点\n" +
                "  CLEAR       - 清空结构\n\n" +
                "数据:\n" +
                "  数值或逗号分隔的数值列表，如: 5,3,7 或 10\n" +
                "  链表删除: 使用位置索引，如 0 (第一个节点)\n\n" +
                "示例指令:\n" +
                "  BST:BATCH_ADD:5,3,7,2,4\n" +
                "  LINKEDLIST:DELETE:0        (删除第一个节点)\n" +
                "  LINKEDLIST:DELETE:2        (删除第三个节点)\n" +
                "  STACK:BATCH_ADD:1,2,3\n" +
                "  BINARYTREE:CLEAR:\n" +
                "  AVL:SEARCH:25";

        JTextArea textArea = new JTextArea(helpText);
        textArea.setEditable(false);
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "DSL指令语法帮助", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleUserInput(JComboBox<String> dslHistoryCombo) {
        String text = chatInputField.getText().trim();
        if (text.isEmpty()) return;

        appendChat("You", text);
        chatInputField.setText("");

        // 保存到历史记录（如果是DSL格式或勾选了DSL模式）
        if (directDSLCheckBox.isSelected() || text.contains(":")) {
            addToDSLHistory(dslHistoryCombo, text);
        }

        if (directDSLCheckBox.isSelected()) {
            // 直接DSL模式
            executeDirectDSL(text);
        } else {
            // AI模式
            sendButton.setEnabled(false);
            logStatus("AI 正在解析指令...");

            LLMService.sendRequest(text, new LLMService.LLMCallback() {
                @Override
                public void onResponse(String response) {
                    SwingUtilities.invokeLater(() -> {
                        appendChat("AI", response);
                        executeDSL(response);
                        sendButton.setEnabled(true);
                        logStatus("AI指令执行完毕");
                    });
                }

                @Override
                public void onError(String error) {
                    SwingUtilities.invokeLater(() -> {
                        appendChat("System", "AI错误: " + error);
                        sendButton.setEnabled(true);
                        // 尝试将输入作为DSL直接执行（如果格式正确）
                        if (text.contains(":")) {
                            appendChat("System", "尝试将输入作为DSL指令执行...");
                            executeDirectDSL(text);
                        }
                    });
                }
            });
        }
    }

    /**
     * 直接执行DSL指令（不经过AI解析）
     */
    private void executeDirectDSL(String dslText) {
        try {
            // 清理输入
            String cleanText = dslText.replace("**", "")
                    .replace("`", "")
                    .replace("DSL:", "")
                    .replace("dsl:", "")
                    .trim();

            // 简单的DSL格式验证
            if (!cleanText.contains(":")) {
                logStatus("错误: DSL指令必须包含冒号(:)，格式: 目标:动作:数据");
                appendChat("System", "DSL格式错误: 缺少冒号分隔符");
                return;
            }

            // 分割指令
            String[] parts = cleanText.split(":", 3); // 最多分成3部分
            if (parts.length < 2) {
                logStatus("错误: DSL指令格式不正确，至少需要目标和动作");
                appendChat("System", "DSL格式错误: 指令格式应为'目标:动作:数据'");
                return;
            }

            String target = parts[0].trim().toUpperCase();
            String action = parts[1].trim().toUpperCase();
            String data = parts.length > 2 ? parts[2].trim() : "";

            // 执行指令
            executeDSLCommand(target, action, data);
            appendChat("System", "✓ DSL指令执行成功: " + cleanText);

        } catch (Exception e) {
            logStatus("DSL执行异常: " + e.getMessage());
            appendChat("System", "✗ DSL执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行DSL指令的核心方法
     */
    private void executeDSLCommand(String target, String action, String data) {
        // 映射面板
        String panelKey = mapTargetToPanelKey(target);
        if (panelKey == null) {
            logStatus("未知的数据结构目标: " + target);
            appendChat("System", "错误: 未知的目标 '" + target + "'");
            return;
        }

        // 切换到对应面板
        switchToPanel(panelKey);

        // 对于链表删除的特殊处理
        if (panelKey.equals("LinkedList") && action.contains("DELETE")) {
            handleLinkedListDelete(data);
            return;
        }

        // 执行UI操作
        performUiAction(currentActivePanel, action, data);

        logStatus("执行DSL: [" + target + ":" + action + "] 数据: " + (data.isEmpty() ? "空" : data));
    }

    /**
     * 处理链表删除的特殊情况
     */
    private void handleLinkedListDelete(String data) {
        try {
            if (data.isEmpty()) {
                logStatus("错误: 链表删除需要指定位置索引");
                appendChat("System", "错误: 链表删除需要指定位置索引，例如 LINKEDLIST:DELETE:0");
                return;
            }

            int index = Integer.parseInt(data);

            // 直接调用LinkedListPanel的删除方法
            if (currentActivePanel instanceof LinkedListPanel) {
                LinkedListPanel panel = (LinkedListPanel) currentActivePanel;
                panel.deleteByIndex(index);
                logStatus("执行链表删除: 位置 " + index);
            } else {
                logStatus("错误: 当前面板不是链表面板");
            }
        } catch (NumberFormatException e) {
            logStatus("错误: 链表删除的位置必须是整数");
            appendChat("System", "错误: 链表删除的位置必须是整数，例如 LINKEDLIST:DELETE:0");
        }
    }

    /**
     * 添加到DSL历史记录
     */
    private void addToDSLHistory(JComboBox<String> combo, String dsl) {
        // 检查是否已存在
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).equals(dsl)) {
                combo.removeItemAt(i);
                break;
            }
        }

        // 添加到首位
        combo.insertItemAt(dsl, 0);
        combo.setSelectedIndex(0);

        // 限制历史记录数量
        if (combo.getItemCount() > 20) {
            combo.removeItemAt(combo.getItemCount() - 1);
        }
    }

    /**
     * 解析 AI 返回的 DSL (针对 1.5B 模型深度优化)
     */
    private void executeDSL(String responseText) {
        try {
            // 1. 预处理：DeepSeek 1.5B 有时会输出 Markdown 加粗或反引号
            String cleanText = responseText.replace("**", "")
                    .replace("`", "")
                    .trim();

            // 2. 提取指令行：寻找包含两个冒号的行
            String commandLine = "";
            String[] lines = cleanText.split("\n");
            for (String line : lines) {
                line = line.trim();
                // 格式检查：必须包含冒号，且不包含过多的自然语言
                if (line.contains(":") && line.split(":").length >= 2) {
                    // 简单的启发式：如果一行以大写字母开头，且包含冒号，大概率是指令
                    if (Character.isUpperCase(line.charAt(0))) {
                        commandLine = line;
                        break;
                    }
                }
            }

            if (commandLine.isEmpty()) {
                // 如果没找到明显的多行中的一行，尝试直接使用全文
                commandLine = cleanText;
            }

            // 去掉末尾可能的句号
            if (commandLine.endsWith(".")) commandLine = commandLine.substring(0, commandLine.length() - 1);

            // 3. 分割指令
            String[] parts = commandLine.split(":");
            if (parts.length < 2) {
                logStatus("无法识别 AI 指令: " + commandLine);
                return;
            }

            // 4. 归一化 Target (移除下划线，解决 BINARY_TREE 问题)
            String target = parts[0].trim().toUpperCase().replace("_", "").replace(" ", "");
            String action = parts[1].trim().toUpperCase();
            String data = parts.length > 2 ? parts[2].trim() : "";

            // 5. 执行指令
            executeDSLCommand(target, action, data);

        } catch (Exception e) {
            logStatus("指令解析异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 增强的目标映射，处理 AI 可能的模糊输出
     */
    private String mapTargetToPanelKey(String target) {
        if (target.contains("LINKED")) return "LinkedList";
        if (target.contains("STACK")) return "Stack";
        if (target.contains("BST")) return "BST"; // BST 必须在 BinaryTree 之前判断
        if (target.contains("BINARY") || target.contains("TREE")) return "BinaryTree"; // 处理 BINARYTREE 或 NORMAL_TREE
        if (target.contains("AVL")) return "AVLTree";
        if (target.contains("HUFFMAN")) return "HuffmanTree";
        return null;
    }

    /**
     * 执行 UI 操作
     */
    private void performUiAction(JPanel panel, String action, String data) {
        // 1. 填入数据 (如果数据存在且不是 NULL)
        JTextField valueField = findComponent(panel, JTextField.class);
        if (valueField != null && !data.isEmpty() && !data.equalsIgnoreCase("NULL")) {
            valueField.setText(data);
        }

        // 2. 查找按钮
        String buttonText = mapActionToButtonText(action, panel);
        JButton targetButton = findButtonByText(panel, buttonText);

        // 3. 执行点击
        if (targetButton != null) {
            logStatus("自动执行: [" + buttonText + "] 参数: " + data);
            targetButton.doClick();
        } else {
            // 降级策略：如果找不到 "批量添加"，尝试找 "添加节点"
            if (buttonText.equals("批量添加")) {
                JButton fallbackBtn = findButtonByText(panel, "添加节点");
                if (fallbackBtn != null) {
                    logStatus("降级执行: [添加节点] (未找到批量按钮)");
                    fallbackBtn.doClick();
                    return;
                }
            }
            logStatus("未找到按钮: " + buttonText + " (Action: " + action + ")");
        }
    }

    /**
     * 关键：将 DSL 动作映射到具体的按钮文本
     */
    private String mapActionToButtonText(String action, JPanel panel) {
        // 归一化动作指令
        if (action.contains("BATCH")) return "批量添加"; // 对应我们在 Panel 中新增的按钮

        // 栈面板特殊处理
        if (panel instanceof StackPanel) {
            if (action.contains("BATCH")) return "批量入栈";
            if (action.contains("ADD") || action.contains("PUSH")) return "入栈";
            if (action.contains("DELETE") || action.contains("POP")) return "出栈";
            if (action.contains("CLEAR")) return "清空栈";
        }

        // 链表面板特殊处理
        if (panel instanceof LinkedListPanel) {
            // 注意：链表删除使用特殊处理，不通过按钮映射
            if (action.contains("CLEAR")) return "清空";
        }

        // 通用映射
        if (action.contains("ADD")) return "添加节点";
        if (action.contains("DELETE")) return "删除(动画)"; // 修改这里，映射到正确的按钮文本
        if (action.contains("SEARCH")) {
            // 优先匹配动画查找 (BST)，其次普通查找
            if (findButtonByText(panel, "动画查找") != null) return "动画查找";
            if (findButtonByText(panel, "查找(动画)") != null) return "查找(动画)";
            return "查找节点";
        }
        if (action.contains("CLEAR")) return "清空树"; // 默认清空树

        return action; // 如果没匹配到，直接用 AI 输出的试试
    }

    // ================== 基础 UI 方法 (保持不变) ==================

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
        helpItem.addActionListener(e -> showUsageManual());

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void showUsageManual() {
        String info = "系统使用说明:\n\n" +
                "=== 两种指令模式 ===\n" +
                "1. AI自然语言模式 (默认):\n" +
                "   • 输入如'建立BST 5,3,7'，由AI解析\n" +
                "   • AI会理解您的意图并生成相应操作\n\n" +
                "2. 直接DSL模式 (勾选复选框):\n" +
                "   • 直接输入DSL指令: 目标:动作:数据\n" +
                "   • 示例: BST:BATCH_ADD:5,3,7\n" +
                "   • 点击'DSL语法帮助'查看完整语法\n\n" +
                "=== 重要说明 ===\n" +
                "• 链表删除使用位置(索引)，不是值！\n" +
                "• 示例: LINKEDLIST:DELETE:0 删除第一个节点\n" +
                "• 索引从0开始计数\n\n" +
                "=== 常见指令示例 ===\n" +
                "• BST:BATCH_ADD:5,3,7,2,4\n" +
                "• LINKEDLIST:DELETE:0 (删除第一个节点)\n" +
                "• LINKEDLIST:DELETE:2 (删除第三个节点)\n" +
                "• STACK:BATCH_ADD:1,2,3\n" +
                "• BINARYTREE:CLEAR:\n" +
                "• AVL:SEARCH:25";
        JOptionPane.showMessageDialog(this, info, "使用说明", JOptionPane.INFORMATION_MESSAGE);
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

    private void appendChat(String role, String message) {
        chatHistoryArea.append(role + ": " + message + "\n\n");
        chatHistoryArea.setCaretPosition(chatHistoryArea.getDocument().getLength());
    }

    // ================== 工具方法 ==================

    // 递归查找组件 (找输入框)
    private <T extends Component> T findComponent(Container container, Class<T> clazz) {
        for (Component comp : container.getComponents()) {
            if (clazz.isInstance(comp)) {
                // 简单的启发式：找比较宽的输入框，通常是值输入框
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

    // 递归查找按钮 (模糊匹配文本)
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

    // ================== 初始化面板 (保持不变) ==================

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

        tabbedPane.addTab("链表结构", createTabPanel(linkedListPanel, "线性表链式存储 - 注意: 删除使用位置(索引)而不是值"));
        tabbedPane.addTab("栈结构", createTabPanel(stackPanel, "LIFO栈结构"));
        tabbedPane.addTab("二叉树构建", createTabPanel(binaryTreePanel, "普通二叉树构建"));
        tabbedPane.addTab("二叉搜索树", createTabPanel(bstPanel, "BST查找与构建"));
        tabbedPane.addTab("哈夫曼树", createTabPanel(huffmanTreePanel, "哈夫曼编码树"));
        tabbedPane.addTab("AVL平衡树", createTabPanel(avlPanel, "AVL自平衡树"));
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(DataStructureVisualizer::new);
    }
}
