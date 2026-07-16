package com.aichuangzuo.user.modules.style.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.style.enums.StyleErrorCode;
import com.aichuangzuo.user.modules.style.service.StyleAnalyzeAiService;
import com.aichuangzuo.user.modules.style.service.StyleAnalyzeService;
import com.aichuangzuo.user.modules.style.vo.StyleAnalyzeVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * AI 风格分析服务实现。
 *
 * <p>prompt 模板与降级策略见 docs/superpowers/specs/2026-07-16-style-analyze-ai-design.md 第 4、5 节。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StyleAnalyzeServiceImpl implements StyleAnalyzeService {

    private static final int PROMPT_MAX_LENGTH = 1000;
    private static final int EXCERPT1_MAX = 120;
    private static final int EXCERPT2_MAX = 80;
    private static final List<String> REQUIRED_MARKERS = List.of("【语气】", "【词汇】", "【句式】", "【结构】");

    private static final String SYSTEM_MESSAGE =
            "你是一位资深的中文文体分析师，擅长拆解中文自媒体文章的写作风格，并把风格特征提炼成可直接指导 AI 写作的提示词。";

    /** 用户消息模板：%s 为参考文章正文。 */
    private static final String USER_PROMPT_TEMPLATE = """
            请分析以下参考文章的写作风格，完成两件事：

            【文章正文】
            %s

            【任务】
            1. 从【语气】【词汇】【句式】【结构】四个维度拆解风格特征。每条特征必须具体、可模仿，禁止空泛形容（不要写"语言优美"，要写"多用15字以内短句，段间留白多"这类可执行描述）。
            2. 从原文中逐字摘录 2 个最能代表该风格的片段。

            【输出 JSON 结构】
            {"excerpt1":"原文中最能代表风格的连续片段，不超过120字，必须逐字摘自原文","excerpt2":"另一个代表性片段，不超过80字，必须逐字摘自原文，且不与excerpt1重复","prompt":"不超过800字的风格提示词"}

            其中 prompt 字段严格使用以下模板：
            你是一位中文写手，请模仿以下参考文章的写作风格：

            【语气】（人称视角、情感温度、与读者的距离感，1-2句）
            【词汇】（书面/口语倾向、网络用语与语气词的使用习惯，1-2句）
            【句式】（句子长短与节奏、标点习惯、常用修辞，1-2句）
            【结构】（开头方式、段落组织、结尾处理，1-2句）

            请在生成新内容时严格遵循以上风格特征。

            最终输出要求（覆盖以上所有说明，必须严格遵守）：
              1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。
              2. 不要用 ```json 或任何代码围栏包裹。
              3. 第一个字符必须是 {，最后一个字符必须是 }。
              4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。
            """;

    private final StyleAnalyzeAiService aiService;
    private final ObjectMapper objectMapper;

    @Override
    public StyleAnalyzeVO analyze(String text) {
        String aiResp = aiService.call(SYSTEM_MESSAGE, String.format(USER_PROMPT_TEMPLATE, text));
        JsonNode root = parseJson(stripCodeFence(aiResp));

        String prompt = root.path("prompt").asText("").trim();
        validatePrompt(prompt);

        StyleAnalyzeVO vo = new StyleAnalyzeVO();
        vo.setPrompt(prompt);
        vo.setExcerpt1(resolveExcerpt(root.path("excerpt1").asText(""), text, true));
        vo.setExcerpt2(resolveExcerpt(root.path("excerpt2").asText(""), text, false));
        return vo;
    }

    private JsonNode parseJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("AI 风格分析结果解析失败 resp={}", abbreviate(raw));
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
        }
    }

    /** prompt 非空、≤1000 字、含四个维度标记，缺一不可。 */
    private void validatePrompt(String prompt) {
        boolean valid = !prompt.isEmpty()
                && prompt.length() <= PROMPT_MAX_LENGTH
                && REQUIRED_MARKERS.stream().allMatch(prompt::contains);
        if (!valid) {
            log.warn("AI 风格分析 prompt 校验失败 length={}", prompt.length());
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
        }
    }

    /**
     * 摘录必须逐字摘自原文（防模型编造）；未命中时降级：
     * excerpt1 → 首个长度 > 20 字段落截取 120 字；excerpt2 → 最长句截取 80 字。
     */
    private String resolveExcerpt(String excerpt, String text, boolean firstParagraph) {
        String candidate = excerpt == null ? "" : excerpt.trim();
        if (!candidate.isEmpty() && text.contains(candidate)) {
            return candidate;
        }
        if (firstParagraph) {
            String first = Arrays.stream(text.split("\\n\\s*\\n"))
                    .map(String::trim)
                    .filter(p -> p.length() > 20)
                    .findFirst()
                    .orElse("");
            return first.length() <= EXCERPT1_MAX ? first : first.substring(0, EXCERPT1_MAX);
        }
        return Arrays.stream(text.split("[。！？\\n]"))
                .map(String::trim)
                .filter(s -> s.length() > 10)
                .max(Comparator.comparingInt(String::length))
                .map(s -> s.length() <= EXCERPT2_MAX ? s : s.substring(0, EXCERPT2_MAX))
                .orElse("");
    }

    /** 防御：模型偶有 ```json 围栏输出，剥掉再解析。 */
    private static String stripCodeFence(String text) {
        if (text == null) {
            return "";
        }
        String s = text.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.strip();
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }
}
