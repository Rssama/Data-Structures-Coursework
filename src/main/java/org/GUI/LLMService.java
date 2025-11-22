package org.GUI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LLMService {
    // ================= 配置区域 =================
    private static final String API_URL = "http://localhost:11434/v1/chat/completions";
    private static final String API_KEY = "ollama";
    // 🔴 请确保此处模型名称正确
    private static final String MODEL_NAME = "deepseek-r1:14b";
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

                // 🔴 针对 1.5B 模型的强化 Prompt
                // 核心修改：
                // 1. 强调 "不要排序" (Do not sort)
                // 2. 明确 BinaryTree 和 BST 的区别
                String systemPrompt =
                        "你是一个严格的数据结构指令转换器。将用户的自然语言转换为标准指令。\n" +
                                "规则：\n" +
                                "1. 格式必须是: [结构类型]:[操作]:[数据]\n" +
                                "2. 数据必须严格保持用户输入的顺序，**绝对禁止排序**。\n" +
                                "3. 不要输出任何思考过程(<think>...</think>)，不要输出Markdown，只输出指令。\n\n" +
                                "结构类型映射：\n" +
                                "- 普通二叉树/二叉树 -> BINARYTREE\n" +
                                "- 二叉搜索树/BST/排序树 -> BST\n" +
                                "- 平衡树/AVL -> AVLTREE\n" +
                                "- 哈夫曼树 -> HUFFMAN\n" +
                                "- 链表 -> LINKEDLIST\n" +
                                "- 栈 -> STACK\n\n" +
                                "示例：\n" +
                                "用户: '建立二叉树 5,3,7' -> 输出: BINARYTREE:BATCH_ADD:5,3,7\n" +
                                "用户: '建立BST 5,3,7' -> 输出: BST:BATCH_ADD:5,3,7\n" +
                                "用户: '入栈 1,2' -> 输出: STACK:PUSH:1,2\n" +
                                "用户: '删除节点5' -> 输出: BST:DELETE:5";

                String jsonBody = String.format(
                        "{\"model\": \"%s\", \"messages\": [" +
                                "{\"role\": \"system\", \"content\": \"%s\"}," +
                                "{\"role\": \"user\", \"content\": \"%s\"}" +
                                "], \"stream\": false, \"temperature\": 0.0}", // 温度设为0，最大程度保证确定性
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
                        String line;
                        while ((line = br.readLine()) != null) response.append(line);

                        String rawContent = extractContentFromJSON(response.toString());
                        // 清洗 DeepSeek 的思考标签
                        String cleanContent = removeThinkTags(rawContent);
                        // 清洗 Markdown 和可能的加粗符号
                        cleanContent = cleanContent.replace("```", "").replace("**", "").trim();

                        callback.onResponse(cleanContent);
                    }
                } else {
                    callback.onError("API Error: " + responseCode);
                }
            } catch (Exception e) {
                callback.onError("Network Error: " + e.getMessage());
            }
        }).start();
    }

    private static String removeThinkTags(String content) {
        Pattern pattern = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.replaceAll("").trim();
    }

    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String extractContentFromJSON(String json) {
        try {
            String marker = "\"content\":\"";
            int startIndex = json.indexOf(marker);
            if (startIndex == -1) return json;
            startIndex += marker.length();
            int endIndex = startIndex;
            while (endIndex < json.length()) {
                endIndex = json.indexOf("\"", endIndex);
                if (endIndex == -1) break;
                if (json.charAt(endIndex - 1) != '\\') break;
                endIndex++;
            }
            if (endIndex == -1) return json;
            String content = json.substring(startIndex, endIndex);
            return content.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\");
        } catch (Exception e) {
            return json;
        }
    }
}