package com.aichuangzuo.admin.modules.generation.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 提示词模板占位符渲染：仅支持 {{name}} 形式，从 var 中取值替换。
 *
 * <p>实现极简：单层替换，未命中保留原样，不抛错（避免 AI 错怪模板）。
 */
@Service
public class PromptTemplateRenderService {

    /** 渲染模板，把 {{key}} 替换为 vars 对应的值。vars 为 null 时返回原模板。 */
    public String render(String template, Map<String, Object> vars) {
        if (template == null || template.isEmpty()) return "";
        if (vars == null || vars.isEmpty()) return template;

        String result = template;
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            if (entry.getKey() == null) continue;
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
