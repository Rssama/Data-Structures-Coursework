package org.GUI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 服务类 - 适配本地 Ollama (DeepSeek)
 */
public class LLMService {
    // ================= 配置区域 =================
    // Ollama 的 OpenAI 兼容接口
    private static final String API_URL = "http://localhost:11434/v1/chat/completions";
    private static final String API_KEY = "ollama"; // Ollama 本地不需要真实 Key，随便填

    // 🔴 请确认此处名称与您 'ollama list' 中的名称一致
    // 常见名称: "deepseek-r1:1.5b", "deepseek-coder:1.3b", "qwen:1.8b"
    private static final String MODEL_NAME = "deepseek-r1:7b";
    // ===========================================

    public interface LLMCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public static void sendRequest(String userMessage, LLMCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setDoOutput(true);

                // 针对小模型的精简 System Prompt
                String systemPrompt = "你是专门为数据结构可视化系统服务的指令生成器。你的任务只有一个：将用户的自然语言请求转换为严格的指令格式。\n\n" +

                        "=== 绝对规则 ===\n" +
                        "1. 输出必须且只能是：目标:动作:数据\n" +
                        "2. 不要包含任何其他字符：不要中括号[]、不要引号\"\"、不要圆括号()、不要星号**、不要反引号`\n" +
                        "3. 不要解释、不要思考、不要道歉、不要说多余的话\n" +
                        "4. 不要输出任何markdown格式\n" +
                        "5. 如果无法理解，直接输出：ERROR:无法理解\n\n" +

                        "=== 指令格式 ===\n" +
                        "格式：目标:动作:数据\n" +
                        "目标必须是以下之一（不区分大小写）：\n" +
                        "  LINKEDLIST, STACK, BST, BINARYTREE, AVL, HUFFMAN\n" +
                        "动作必须是以下之一（不区分大小写）：\n" +
                        "  BATCH_ADD, ADD, DELETE, SEARCH, CLEAR\n\n" +

                        "=== 数据结构特殊格式 ===\n" +
                        "【链表LINKEDLIST】\n" +
                        "  插入：LINKEDLIST:INSERT:位置:值   示例：在链表第0位插入10 → LINKEDLIST:INSERT:0:10\n" +
                        "  删除：LINKEDLIST:DELETE:位置      示例：删除链表第2个节点 → LINKEDLIST:DELETE:2\n" +
                        "  批量添加：LINKEDLIST:BATCH_ADD:值,值,值  示例：链表添加5,3,7 → LINKEDLIST:BATCH_ADD:5,3,7\n" +
                        "  清空：LINKEDLIST:CLEAR:\n\n" +

                        "【栈STACK】\n" +
                        "  批量入栈：STACK:BATCH_ADD:值,值,值  示例：入栈1,2,3 → STACK:BATCH_ADD:1,2,3\n" +
                        "  出栈：STACK:DELETE:         示例：出栈 → STACK:DELETE:\n" +
                        "  清空：STACK:CLEAR:\n\n" +

                        "【二叉搜索树BST】\n" +
                        "  批量添加：BST:BATCH_ADD:值,值,值  示例：建立BST 5,3,7 → BST:BATCH_ADD:5,3,7\n" +
                        "  插入：BST:ADD:值            示例：插入9 → BST:ADD:9\n" +
                        "  删除：BST:DELETE:值         示例：删除5 → BST:DELETE:5\n" +
                        "  查找：BST:SEARCH:值         示例：查找7 → BST:SEARCH:7\n" +
                        "  清空：BST:CLEAR:\n\n" +

                        "【普通二叉树BINARYTREE】\n" +
                        "  批量添加：BINARYTREE:BATCH_ADD:值,值,值  示例：建立二叉树1,2,3 → BINARYTREE:BATCH_ADD:1,2,3\n" +
                        "  清空：BINARYTREE:CLEAR:\n\n" +

                        "【AVL平衡树】\n" +
                        "  批量添加：AVL:BATCH_ADD:值,值,值  示例：建立AVL树5,3,7 → AVL:BATCH_ADD:5,3,7\n" +
                        "  插入：AVL:ADD:值            示例：插入9 → AVL:ADD:9\n" +
                        "  查找：AVL:SEARCH:值         示例：查找7 → AVL:SEARCH:7\n" +
                        "  清空：AVL:CLEAR:\n\n" +

                        "【哈夫曼树HUFFMAN】\n" +
                        "  构建：HUFFMAN:BATCH_ADD:值,值,值  示例：构建哈夫曼树5,3,7 → HUFFMAN:BATCH_ADD:5,3,7\n" +
                        "  清空：HUFFMAN:CLEAR:\n\n" +

                        "=== 关键示例 ===\n" +
                        "1. '建立一个包含5,3,7的BST' → BST:BATCH_ADD:5,3,7\n" +
                        "2. '在链表头部插入10' → LINKEDLIST:INSERT:0:10\n" +
                        "3. '删除链表的第二个节点' → LINKEDLIST:DELETE:1\n" +
                        "4. '栈里添加1,2,3' → STACK:BATCH_ADD:1,2,3\n" +
                        "5. '清空二叉树' → BINARYTREE:CLEAR:\n" +
                        "6. '查找BST中的5' → BST:SEARCH:5\n" +
                        "7. '删除BST中的7' → BST:DELETE:7\n" +
                        "8. '构建哈夫曼树2,4,6,8' → HUFFMAN:BATCH_ADD:2,4,6,8\n" +
                        "9. '建立AVL树10,5,15' → AVL:BATCH_ADD:10,5,15\n\n" +

                        "=== 重要提醒 ===\n" +
                        "1. 所有值都是整数，用逗号分隔，不要有空格\n" +
                        "2. 链表位置从0开始计数\n" +
                        "3. 如果数据为空，保持冒号，如：BINARYTREE:CLEAR:\n" +
                        "4. 这是唯一的输出格式，绝对不要输出其他内容！\n";
                // 构建 JSON Body
                String jsonBody = String.format(
                        "{\"model\": \"%s\", \"messages\": [" +
                                "{\"role\": \"system\", \"content\": \"%s\"}," +
                                "{\"role\": \"user\", \"content\": \"%s\"}" +
                                "], \"stream\": false, \"temperature\": 0.1}", // 低温度降低幻觉
                        MODEL_NAME,
                        escapeJson(systemPrompt),
                        escapeJson(userMessage)
                );

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        // 提取内容
                        String rawContent = extractContentFromJSON(response.toString());

                        // 🔴 关键：清洗 DeepSeek R1 的 <think> 标签
                        String cleanContent = removeThinkTags(rawContent);

                        // 再次清洗可能存在的 Markdown 代码块符号
                        cleanContent = cleanContent.replace("```", "").trim();

                        callback.onResponse(cleanContent);
                    }
                } else {
                    callback.onError("Ollama 连接失败 (Code: " + responseCode + ")。请确认 Ollama 已运行。");
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("网络错误: " + e.getMessage());
            }
        }).start();
    }

    // 去除 <think>...</think> 内容
    private static String removeThinkTags(String content) {
        // 匹配 <think>...</think> (包括换行)
        Pattern pattern = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.replaceAll("").trim();
    }

    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String extractContentFromJSON(String json) {
        try {
            // 简单解析，适应 Ollama 返回的 OpenAI 格式
            String marker = "\"content\":\"";
            int startIndex = json.indexOf(marker);
            if (startIndex == -1) return json; // 没找到，直接返回原文方便调试
            startIndex += marker.length();

            // 寻找结束引号，注意处理转义引号
            int endIndex = startIndex;
            while (endIndex < json.length()) {
                endIndex = json.indexOf("\"", endIndex);
                if (endIndex == -1) break;
                if (json.charAt(endIndex - 1) != '\\') {
                    break; // 找到未转义的结束引号
                }
                endIndex++; // 跳过转义引号
            }

            if (endIndex == -1) return json;

            String content = json.substring(startIndex, endIndex);
            // 处理 JSON 转义字符
            return content.replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        } catch (Exception e) {
            return json;
        }
    }
}