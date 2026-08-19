package com.aichuangzuo.shared.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    public static String extractJsonObject(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
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

    public static JsonNode parseLenient(ObjectMapper mapper, String raw) throws JsonProcessingException {
        String cleaned = stripCodeFence(raw);
        try {
            return mapper.readTree(cleaned);
        } catch (Exception ignored1) {
            String extracted = extractJsonObject(cleaned);
            if (extracted != null && !extracted.equals(cleaned)) {
                try {
                    return mapper.readTree(extracted);
                } catch (Exception ignored2) {
                    // continue
                }
            }
            String fixed = fixUnescapedQuotesInField(cleaned, "prompt");
            if (!fixed.equals(cleaned)) {
                try {
                    return mapper.readTree(fixed);
                } catch (Exception ignored3) {
                    // continue
                }
            }
            throw new JsonProcessingException("无法解析 LLM 返回的 JSON") {};
        }
    }
}
