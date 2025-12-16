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
    private static final String MODEL_NAME = "deepseek-r1:1.5b";
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
                String systemPrompt = "你是一个指令生成器。用户输入自然语言，你只输出格式指令。\n" +
                        "格式: [目标]:[动作]:[数据]\n" +
                        "目标: LINKEDLIST, STACK, BST, AVL, HUFFMAN, BINARYTREE\n" +
                        "动作: BATCH_ADD, ADD, DELETE, SEARCH, CLEAR\n" +
                        "例子:\n" +
                        "\"建树5,3\" -> BST:BATCH_ADD:5,3\n" +
                        "\"删5\" -> BST:DELETE:5\n" +
                        "禁止输出思考过程，禁止输出Markdown，禁止废话。";

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