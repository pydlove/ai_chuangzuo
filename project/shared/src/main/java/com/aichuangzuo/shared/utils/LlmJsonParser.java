package com.aichuangzuo.shared.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class LlmJsonParser {

    private LlmJsonParser() {}

    public static String stripCodeFence(String text) {
        if (text == null) {
            return "";
        }
        String s = text.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            } else {
                s = s.substring(3);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.strip();
    }

    /**
     * 从文本中提取“最后一个能成功解析”的顶层 JSON 对象或数组。
     *
     * <p>LLM 很容易把 prompt 里的 JSON 示例也复述出来，导致文本里出现多个 JSON 块。
     * 如果简单取“第一个 { 到最后一个 }”，会把示例和最终结果混在一起，得到非法 JSON。
     * 这里按字符串/转义感知地扫描每个顶层对象或数组，逐个尝试解析，返回最后一个成功的节点。
     */
    public static JsonNode extractBestJsonNode(ObjectMapper mapper, String text) {
        if (text == null) {
            return null;
        }
        JsonNode best = null;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '"') {
                // 跳过字符串字面量
                i = skipString(text, i + 1);
                continue;
            }
            if (c == '{' || c == '[') {
                char close = c == '{' ? '}' : ']';
                int end = findMatchingClose(text, i, c, close);
                if (end > i) {
                    String candidate = text.substring(i, end + 1);
                    try {
                        best = mapper.readTree(candidate);
                    } catch (Exception ignored) {
                        // 继续尝试下一个候选
                    }
                    i = end + 1;
                    continue;
                }
            }
            i++;
        }
        return best;
    }

    /**
     * 旧的兜底实现：取第一个 { 到最后一个 } 之间的文本。
     * 保留用于外部仍有简单提取需求的场景，但内部已不再使用。
     */
    public static String extractJsonObject(String text) {
        if (text == null) {
            return null;
        }
        JsonNode node = extractBestJsonNode(new ObjectMapper(), text);
        if (node == null) {
            return null;
        }
        return node.toString();
    }

    private static int skipString(String text, int start) {
        int i = start;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                i += 2;
                continue;
            }
            if (c == '"') {
                return i + 1;
            }
            i++;
        }
        return i;
    }

    private static int findMatchingClose(String text, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        int i = start;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\' && i + 1 < text.length()) {
                    i += 2;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                i++;
                continue;
            }
            if (c == '"') {
                inString = true;
                i++;
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    public static String fixUnescapedQuotesInField(String text, String fieldName) {
        if (text == null || fieldName == null) {
            return text;
        }
        String promptKey = "\"" + fieldName + "\"";
        int keyIdx = text.indexOf(promptKey);
        if (keyIdx < 0) {
            return text;
        }
        int valueStart = text.indexOf('"', keyIdx + promptKey.length());
        if (valueStart < 0) {
            return text;
        }
        valueStart++;
        int valueEnd = -1;
        for (int i = text.length() - 1; i >= valueStart; i--) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                int j = i + 1;
                while (j < text.length() && Character.isWhitespace(text.charAt(j))) {
                    j++;
                }
                if (j >= text.length() || text.charAt(j) == ',' || text.charAt(j) == '}') {
                    valueEnd = i;
                    break;
                }
            }
        }
        if (valueEnd <= valueStart) {
            return text;
        }
        String promptValue = text.substring(valueStart, valueEnd);
        StringBuilder fixed = new StringBuilder();
        for (int i = 0; i < promptValue.length(); i++) {
            char c = promptValue.charAt(i);
            if (c == '"' && (i == 0 || promptValue.charAt(i - 1) != '\\')) {
                fixed.append('\\');
            }
            fixed.append(c);
        }
        return text.substring(0, valueStart) + fixed + text.substring(valueEnd);
    }

    /**
     * 尝试修复 JSON 字符串值内部未转义的双引号。
     *
     * <p>LLM 经常在生成的中文文本里嵌套双引号（如 title/tips 中出现 "他说 "你好""），
     * 导致整段 JSON 无法解析。本方法按字符扫描，利用“内部引号通常成对出现”这一特征，
     * 判断一个双引号是字符串结束还是内部引号开始，并把内部引号转义为 \"。
     */
    public static String escapeUnescapedInternalQuotes(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(text.length() + 16);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c != '"') {
                result.append(c);
                i++;
                continue;
            }

            // 找到一个可能的字符串开始
            result.append('"');
            i++;
            int unclosedInternalQuotes = 0;
            while (i < text.length()) {
                char d = text.charAt(i);
                if (d == '\\' && i + 1 < text.length()) {
                    // 转义序列原样保留
                    result.append(d).append(text.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (d != '"') {
                    result.append(d);
                    i++;
                    continue;
                }

                // 遇到双引号
                if (unclosedInternalQuotes > 0) {
                    // 关闭一个内部引号
                    result.append('\\').append('"');
                    unclosedInternalQuotes--;
                    i++;
                    continue;
                }

                // unclosedInternalQuotes == 0，需要判断是字符串结束还是内部引号开始
                int nextQuoteOrBoundary = findNextQuoteOrStructuralBoundary(text, i + 1);
                boolean isClosingQuote = nextQuoteOrBoundary < 0 || isStructuralBoundary(text.charAt(nextQuoteOrBoundary));
                if (isClosingQuote) {
                    result.append('"');
                    // 跳过字符串结束后的空白，停在下一个有效字符或文本末尾
                    int j = i + 1;
                    while (j < text.length() && Character.isWhitespace(text.charAt(j))) {
                        j++;
                    }
                    i = j;
                    break;
                }
                // 内部引号开始
                result.append('\\').append('"');
                unclosedInternalQuotes++;
                i++;
            }
        }
        return result.toString();
    }

    /**
     * 从指定位置开始查找下一个双引号或 JSON 结构边界字符（: , } ]）。
     * 若先遇到结构边界，返回其下标；若先遇到双引号，返回双引号下标；
     * 跳过转义序列；若到文本末尾都没找到，返回 -1。
     */
    private static int findNextQuoteOrStructuralBoundary(String text, int start) {
        for (int k = start; k < text.length(); k++) {
            char c = text.charAt(k);
            if (c == '\\' && k + 1 < text.length()) {
                k++; // 跳过转义字符
                continue;
            }
            if (c == '"' || c == ':' || c == ',' || c == '}' || c == ']') {
                return k;
            }
        }
        return -1;
    }

    private static boolean isStructuralBoundary(char c) {
        return c == ':' || c == ',' || c == '}' || c == ']';
    }

    public static JsonNode parseLenient(ObjectMapper mapper, String raw) throws JsonProcessingException {
        if (raw == null) {
            throw new JsonProcessingException("无法解析 LLM 返回的 JSON：输入为空") {};
        }

        // 使用一个更宽容的 mapper 副本：允许 trailing comma、未转义控制字符等
        ObjectMapper lenientMapper = mapper.copy()
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());

        String cleaned = stripCodeFence(raw).strip();
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1);
        }

        // 有时 LLM 会把整个 JSON 再包成一层 JSON 字符串："{\"a\":1}"
        if (cleaned.length() >= 2 && cleaned.charAt(0) == '"' && cleaned.charAt(cleaned.length() - 1) == '"') {
            try {
                String unwrapped = lenientMapper.readValue(cleaned, String.class);
                if (unwrapped != null) {
                    JsonNode inner = parseLenient(mapper, unwrapped);
                    if (inner != null) {
                        return inner;
                    }
                }
            } catch (Exception ignored1) {
                // 继续兜底
            }
        }

        try {
            return lenientMapper.readTree(cleaned);
        } catch (Exception ignored2) {
            // 继续兜底
        }

        JsonNode extracted = extractBestJsonNode(lenientMapper, cleaned);
        if (extracted != null) {
            return extracted;
        }

        String fixed = escapeUnescapedInternalQuotes(cleaned);
        if (!fixed.equals(cleaned)) {
            JsonNode fixedExtracted = extractBestJsonNode(lenientMapper, fixed);
            if (fixedExtracted != null) {
                return fixedExtracted;
            }
        }

        fixed = fixUnescapedQuotesInField(cleaned, "prompt");
        if (!fixed.equals(cleaned)) {
            JsonNode fixedExtracted = extractBestJsonNode(lenientMapper, fixed);
            if (fixedExtracted != null) {
                return fixedExtracted;
            }
        }

        throw new JsonProcessingException("无法解析 LLM 返回的 JSON") {};
    }
}
