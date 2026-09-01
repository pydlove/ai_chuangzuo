package com.aichuangzuo.user.modules.skill.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.LlmJsonParser;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.skill.analyze.config.service.SkillAnalyzeConfigService;
import com.aichuangzuo.user.modules.skill.analyze.service.SkillAnalyzeDailyLimiter;
import com.aichuangzuo.shared.enums.error.SkillErrorCode;
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
        JsonNode root = parseJson(aiResp);

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
        try {
            return LlmJsonParser.parseLenient(lenientObjectMapper, raw);
        } catch (Exception e) {
            log.warn("AI 风格分析结果解析失败 resp={}", abbreviate(raw, 2000));
            throw new BusinessException(SkillErrorCode.SKILL_ANALYZE_FAILED);
        }
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
