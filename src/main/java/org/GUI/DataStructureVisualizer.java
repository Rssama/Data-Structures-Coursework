package org.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据结构可视化模拟器 - 主类
 * 包含链表、栈、二叉树构建、二叉搜索树、哈夫曼树和AVL平衡树
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
        setTitle("数据结构可视化模拟器 - 增强版");
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

        // 创建各个面板并保存引用
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
                "线性表的链式存储结构\n支持添加、插入、删除节点操作"));

        tabbedPane.addTab("栈结构", createTabPanel(stackPanel,
                "后进先出(LIFO)数据结构\n支持入栈、出栈、查看栈顶操作"));

        tabbedPane.addTab("二叉树构建", createTabPanel(binaryTreePanel,
                "二叉树构建与遍历\n支持层序构建和三种遍历方式的动画演示"));

        tabbedPane.addTab("二叉搜索树", createTabPanel(bstPanel,
                "带动画查找的二叉搜索树\n支持构建、动画查找、删除节点操作"));

        tabbedPane.addTab("哈夫曼树", createTabPanel(huffmanTreePanel,
                "动态构建哈夫曼树\n手动控制构建过程，展示每一步的合并操作"));

        tabbedPane.addTab("AVL平衡树", createTabPanel(avlPanel,
                "自平衡二叉搜索树\n展示插入删除时的旋转平衡操作"));

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
        logStatus("当前包含: 链表结构、栈结构、二叉树构建、二叉搜索树、哈夫曼树、AVL平衡树");
        logStatus("使用文件菜单可以保存和加载数据结构状态");
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem saveItem = new JMenuItem("保存当前结构");
        JMenuItem loadItem = new JMenuItem("加载结构");
        JMenuItem exitItem = new JMenuItem("退出");

        saveItem.addActionListener(this::saveCurrentStructure);
        loadItem.addActionListener(this::loadStructure);
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

    private void saveCurrentStructure(ActionEvent e) {
        if (currentActivePanel == null) {
            logStatus("错误: 没有活动的面板可以保存");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存数据结构");
        fileChooser.setSelectedFile(new File(currentPanelName + "_data.dat"));

        // 添加文件过滤器
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "数据结构文件 (*.dat)", "dat"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            // 确保文件扩展名
            if (!file.getName().toLowerCase().endsWith(".dat")) {
                file = new File(file.getAbsolutePath() + ".dat");
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(file)))) {

                // 根据当前面板类型获取状态
                Object state = null;
                switch (currentPanelName) {
                    case "LinkedList":
                        state = ((LinkedListPanel) currentActivePanel).getCurrentState();
                        break;
                    case "Stack":
                        state = ((StackPanel) currentActivePanel).getCurrentState();
                        break;
                    case "BinaryTree":
                        state = ((BinaryTreePanel) currentActivePanel).getCurrentState();
                        break;
                    case "BST":
                        state = ((BSTPanel) currentActivePanel).getCurrentState();
                        break;
                    case "HuffmanTree":
                        state = ((HuffmanTreePanel) currentActivePanel).getCurrentState();
                        break;
                    case "AVLTree":
                        state = ((AVLPanel) currentActivePanel).getCurrentState();
                        break;
                }

                if (state != null) {
                    StructureSaveData saveData = new StructureSaveData();
                    saveData.setPanelType(currentPanelName);
                    saveData.setTimestamp(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    saveData.setState(state);

                    oos.writeObject(saveData);
                    logStatus("数据结构已保存到: " + file.getName());
                } else {
                    logStatus("错误: 无法获取当前数据结构的状态");
                }

            } catch (IOException ex) {
                logStatus("保存失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                        "保存失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                logStatus("保存过程中发生错误: " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                        "保存过程中发生错误: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadStructure(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("加载数据结构");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "数据结构文件 (*.dat)", "dat"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(file)))) {

                StructureSaveData saveData = (StructureSaveData) ois.readObject();
                String panelType = saveData.getPanelType();
                Object state = saveData.getState();

                // 切换到对应的面板
                switchToPanel(panelType);

                // 根据面板类型恢复状态
                switch (panelType) {
                    case "LinkedList":
                        if (state instanceof LinkedListPanel.LinkedListState) {
                            ((LinkedListPanel) currentActivePanel).restoreFromState((LinkedListPanel.LinkedListState) state);
                        }
                        break;
                    case "Stack":
                        if (state instanceof StackPanel.StackState) {
                            ((StackPanel) currentActivePanel).restoreFromState((StackPanel.StackState) state);
                        }
                        break;
                    case "BinaryTree":
                        if (state instanceof BinaryTreePanel.BinaryTreeState) {
                            ((BinaryTreePanel) currentActivePanel).restoreFromState((BinaryTreePanel.BinaryTreeState) state);
                        }
                        break;
                    case "BST":
                        if (state instanceof BSTPanel.BSTState) {
                            ((BSTPanel) currentActivePanel).restoreFromState((BSTPanel.BSTState) state);
                        }
                        break;
                    case "HuffmanTree":
                        if (state instanceof HuffmanTreePanel.HuffmanTreeState) {
                            ((HuffmanTreePanel) currentActivePanel).restoreFromState((HuffmanTreePanel.HuffmanTreeState) state);
                        }
                        break;
                    case "AVLTree":
                        if (state instanceof AVLPanel.AVLTreeState) {
                            ((AVLPanel) currentActivePanel).restoreFromState((AVLPanel.AVLTreeState) state);
                        }
                        break;
                }

                logStatus("数据结构已从 " + file.getName() + " 加载 (保存时间: " + saveData.getTimestamp() + ")");

            } catch (IOException | ClassNotFoundException ex) {
                logStatus("加载失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                        "加载失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                logStatus("加载过程中发生错误: " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                        "加载过程中发生错误: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================== 辅助方法 ==================

    /**
     * 切换到指定面板 - 修改为public方法
     */
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

        // 描述面板
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

        // 状态文本区域
        statusArea = new JTextArea(4, 50);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("宋体", Font.PLAIN, 12));
        statusArea.setBackground(new Color(250, 250, 250));

        JScrollPane scrollPane = new JScrollPane(statusArea);

        // 按钮面板
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
                "数据结构可视化模拟器 - 使用说明\n\n" +
                        "链表结构:\n" +
                        "  • 添加节点: 输入值，点击添加节点按钮\n" +
                        "  • 插入节点: 输入值和位置，在指定位置插入\n" +
                        "  • 删除节点: 输入位置，删除指定节点\n" +
                        "  • 清空链表: 移除所有节点\n\n" +
                        "栈结构:\n" +
                        "  • 入栈操作: 输入值，推入栈顶\n" +
                        "  • 出栈操作: 从栈顶弹出元素\n" +
                        "  • 查看栈顶: 显示栈顶元素值\n" +
                        "  • 清空栈: 移除所有栈元素\n\n" +
                        "二叉树构建:\n" +
                        "  • 添加节点: 输入值，按层序构建二叉树\n" +
                        "  • 层序构建: 输入多个值(逗号分隔)，一次性构建完全二叉树\n" +
                        "  • 遍历操作: 支持先序、中序、后序遍历，带动画效果\n" +
                        "  • 颜色标识: 黄色-当前节点，橙色-已访问，绿色-叶子节点，青色-内部节点\n\n" +
                        "二叉搜索树(动画查找):\n" +
                        "  • 添加节点: 输入值构建二叉搜索树\n" +
                        "  • 动画查找: 输入值，显示从根节点到目标的完整查找路径\n" +
                        "  • 删除节点: 输入值从树中删除节点\n" +
                        "  • 清空树: 移除所有节点\n" +
                        "  • 转为普通二叉树: 将BST转换为普通二叉树\n" +
                        "  • 转为链表: 将BST转换为链表\n\n" +
                        "普通二叉树:\n" +
                        "  • 转为BST: 将普通二叉树转换为二叉搜索树\n\n" +
                        "链表结构:\n" +
                        "  • 转为BST: 将链表转换为二叉搜索树\n\n" +
                        "哈夫曼树(动态构建):\n" +
                        "  • 开始构建: 输入权重值(逗号分隔)，准备构建过程\n" +
                        "  • 上一步/下一步: 手动控制构建过程，查看每一步的合并操作\n" +
                        "  • 直接完成: 直接显示最终构建结果\n" +
                        "  • 重置: 重新开始构建过程\n\n" +
                        "AVL平衡树:\n" +
                        "  • 插入节点: 自动进行平衡操作\n" +
                        "  • 删除节点: 自动进行平衡操作\n" +
                        "  • 查找节点: 高亮显示查找路径\n" +
                        "  • 平衡显示: 显示每个节点的平衡因子\n" +
                        "  • 颜色标识: 红色-需要平衡，黄色-当前操作节点\n\n" +
                        "文件操作:\n" +
                        "  • 保存结构: 将当前数据结构保存到文件\n" +
                        "  • 加载结构: 从文件加载之前保存的数据结构\n" +
                        "  • 导出日志: 将操作日志导出为文本文件\n\n" +
                        "增强功能:\n" +
                        "  • 输入验证: 全面的输入验证和错误处理\n" +
                        "  • 遍历动画: 增强的变色效果显示遍历顺序\n" +
                        "  • 数据结构转换: BST与普通二叉树、链表之间的相互转换\n" +
                        "  • 健壮性: 异常处理和边界条件检查";

        JTextArea helpArea = new JTextArea(helpMessage);
        helpArea.setEditable(false);
        helpArea.setFont(new Font("宋体", Font.PLAIN, 12));
        helpArea.setBackground(new Color(240, 240, 240));

        JScrollPane scrollPane = new JScrollPane(helpArea);
        scrollPane.setPreferredSize(new Dimension(500, 500));

        JOptionPane.showMessageDialog(this, scrollPane, "使用说明", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAboutDialog() {
        String aboutMessage =
                "数据结构可视化模拟器 v2.0\n\n" +
                        "功能特点:\n" +
                        "• 六种数据结构可视化: 链表、栈、二叉树、BST、哈夫曼树、AVL树\n" +
                        "• 数据结构转换: BST与普通二叉树、链表之间的相互转换\n" +
                        "• 动画演示: 遍历过程、查找路径、平衡操作\n" +
                        "• 数据持久化: 支持保存和加载数据结构状态\n" +
                        "• 健壮性设计: 全面的输入验证和异常处理\n" +
                        "• 用户友好: 直观的界面和详细的操作反馈\n\n" +
                        "开发技术:\n" +
                        "• Java Swing GUI框架\n" +
                        "• 面向对象设计\n" +
                        "• 序列化技术\n" +
                        "• 自定义绘图\n\n" +
                        "© 2024 数据结构课程设计项目";

        JOptionPane.showMessageDialog(this, aboutMessage, "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    private void logStatus(String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        statusArea.append("[" + timestamp + "] " + message + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 启动应用
        SwingUtilities.invokeLater(() -> {
            new DataStructureVisualizer();
        });
    }
}

/**
 * 数据结构保存数据类 - 用于序列化
 */
class StructureSaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String panelType;
    private String timestamp;
    private Object state; // 保存面板的状态对象

    public StructureSaveData() {
        // 不再使用dataMap，直接使用state对象
    }

    // Getter和Setter方法
    public String getPanelType() { return panelType; }
    public void setPanelType(String panelType) { this.panelType = panelType; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Object getState() { return state; }
    public void setState(Object state) { this.state = state; }

    // 为了向后兼容，保留这些方法但标记为过时
    @Deprecated
    public void setData(String key, Object value) { }

    @Deprecated
    public Object getData(String key) { return null; }
}