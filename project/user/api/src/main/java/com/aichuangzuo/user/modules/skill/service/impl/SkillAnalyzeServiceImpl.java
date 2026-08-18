package com.aichuangzuo.user.modules.skill.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.skill.analyze.config.service.SkillAnalyzeConfigService;
import com.aichuangzuo.user.modules.skill.analyze.service.SkillAnalyzeDailyLimiter;
import com.aichuangzuo.user.modules.skill.enums.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.service.SkillAnalyzeAiService;
import com.aichuangzuo.user.modules.skill.service.SkillAnalyzeService;
import com.aichuangzuo.user.modules.skill.vo.SkillAnalyzeVO;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * AI 风格分析服务实现。
 *
 * <p>prompt 模板与降级策略见 docs/superpowers/specs/2026-07-16-skill-analyze-ai-design.md 第 4、5 节。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillAnalyzeServiceImpl implements SkillAnalyzeService {

    private static final int PROMPT_MAX_LENGTH = 1200;
    private static final int TEXT_MAX_LENGTH = 1000;
    private static final int EXCERPT1_MAX = 120;
    private static final int EXCERPT2_MAX = 80;
    private static final int DESCRIPTION_MAX = 100;
    private static final String BENEFIT_CODE_SKILL_LEARN_ANALYZE = "skill_learn_analyze";
    private static final List<String> REQUIRED_MARKERS = List.of("【语气】", "【词汇】", "【句式】", "【结构】");

    private final SkillAnalyzeAiService aiService;
    private final SkillAnalyzeConfigService skillAnalyzeConfigService;
    private final SkillAnalyzeDailyLimiter skillAnalyzeDailyLimiter;
    private final BenefitService benefitService;
    private final AiPromptRenderService aiPromptRenderService;
    private final ObjectMapper objectMapper;
    private final ObjectMapper lenientObjectMapper = createLenientObjectMapper();

    private static ObjectMapper createLenientObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        return mapper;
    }

    @Override
    public SkillAnalyzeVO analyze(Long userId, String text) {
        BenefitCheckVO benefitCheck = benefitService.check(userId, BENEFIT_CODE_SKILL_LEARN_ANALYZE);
        if (!Boolean.TRUE.equals(benefitCheck.getAllowed())) {
            throw new BusinessException(SkillErrorCode.SKILL_LEARN_QUOTA_EXCEEDED);
        }

        int dailyLimit = skillAnalyzeConfigService.getDailyAttemptLimit();
        skillAnalyzeDailyLimiter.checkAndIncrement(userId, dailyLimit);

        // 统一截断：超过 1000 字只取前 1000 字学习
        if (text != null && text.length() > TEXT_MAX_LENGTH) {
            text = text.substring(0, TEXT_MAX_LENGTH);
        }

        AiPromptRendered rendered = aiPromptRenderService.render("skill_analyze_v1", Map.of("text", text));
        String aiResp = aiService.call(rendered.systemRole(), rendered.userPrompt());
        JsonNode root = parseJson(stripCodeFence(aiResp));

        String prompt = root.path("prompt").asText("").trim();
        validatePrompt(prompt);

        String description = root.path("description").asText("").trim();
        if (description.length() > DESCRIPTION_MAX) {
            description = description.substring(0, DESCRIPTION_MAX);
        }

        SkillAnalyzeVO vo = new SkillAnalyzeVO();
        vo.setPrompt(prompt);
        vo.setDescription(description);
        vo.setExcerpt1(resolveExcerpt(root.path("excerpt1").asText(""), text, true));
        vo.setExcerpt2(resolveExcerpt(root.path("excerpt2").asText(""), text, false));
        return vo;
    }

    private JsonNode parseJson(String raw) {
        String cleaned = stripCodeFence(raw);
        try {
            return lenientObjectMapper.readTree(cleaned);
        } catch (Exception e) {
            String extracted = extractJsonObject(cleaned);
            if (extracted != null && !extracted.equals(cleaned)) {
                try {
                    return lenientObjectMapper.readTree(extracted);
                } catch (Exception ignored) {
                    // 继续尝试修复 prompt 内未转义引号
                }
            }
            String fixed = fixUnescapedQuotesInPrompt(cleaned);
            if (!fixed.equals(cleaned)) {
                try {
                    return lenientObjectMapper.readTree(fixed);
                } catch (Exception ignored) {
                    // 继续走失败分支
                }
            }
            log.warn("AI 风格分析结果解析失败 resp={}", abbreviate(raw, 2000));
            throw new BusinessException(SkillErrorCode.SKILL_ANALYZE_FAILED);
        }
    }

    /**
     * 修复 prompt 字段内部未转义的英文双引号。
     * 模型常把示例词语写成 "xxx" 而非 \"xxx\"，导致整个 JSON 非法。
     */
    private static String fixUnescapedQuotesInPrompt(String text) {
        if (text == null) {
            return text;
        }
        String promptKey = "\"prompt\"";
        int keyIdx = text.indexOf(promptKey);
        if (keyIdx < 0) {
            return text;
        }
        int valueStart = text.indexOf('"', keyIdx + promptKey.length());
        if (valueStart < 0) {
            return text;
        }
        valueStart++; // 跳过 opening quote

        // 从后往前找 prompt 字段的结束引号：第一个前面不是反斜杠的 "
        int valueEnd = -1;
        for (int i = text.length() - 1; i >= valueStart; i--) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                // 再确认它后面是合法的 JSON 分隔符（跳过空白）
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

    /** 从文本中截取出最外层 JSON 对象（兼容模型在 JSON 前后加说明的情况）。 */
    private static String extractJsonObject(String text) {
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

    /** prompt 非空、≤1200 字、含四个维度标记，缺一不可。 */
    private void validatePrompt(String prompt) {
        boolean valid = !prompt.isEmpty()
                && prompt.length() <= PROMPT_MAX_LENGTH
                && REQUIRED_MARKERS.stream().allMatch(prompt::contains);
        if (!valid) {
            log.warn("AI 风格分析 prompt 校验失败 length={}", prompt.length());
            throw new BusinessException(SkillErrorCode.SKILL_ANALYZE_FAILED);
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
            } else {
                s = s.substring(3);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.strip();
    }

    private static String abbreviate(String s, int maxLength) {
        if (s == null) {
            return "null";
        }
        return s.length() <= maxLength ? s : s.substring(0, maxLength) + "...";
    }

    private static String abbreviate(String s) {
        return abbreviate(s, 200);
    }
}
