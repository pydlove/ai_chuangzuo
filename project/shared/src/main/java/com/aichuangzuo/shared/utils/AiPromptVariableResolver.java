package com.aichuangzuo.shared.utils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 提示词变量解析器：统一处理 {{variableName}} 占位符。
 */
public final class AiPromptVariableResolver {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private AiPromptVariableResolver() {
    }

    /**
     * 从文本中提取所有变量名（去重、保持出现顺序）。
     */
    public static Set<String> extractVariables(String text) {
        Set<String> variables = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return variables;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    /**
     * 替换模板中的变量。缺失的变量替换为空字符串。
     */
    public static String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return "";
        }
        if (variables == null) {
            variables = Map.of();
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = variables.get(name);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : value.toString()));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
