package com.aichuangzuo.user.modules.skill.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
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
    private static final List<String> REQUIRED_MARKERS = List.of("【语气】", "【词汇】", "【句式】", "【结构】");

    private static final String SYSTEM_MESSAGE =
            "你是一位资深的中文文体分析师，擅长拆解中文自媒体文章的写作风格，并把风格特征提炼成可直接指导 AI 写作的提示词。";

    /** 用户消息模板：%s 为参考文章正文。 */
    private static final String USER_PROMPT_TEMPLATE = """
            请分析以下参考文章的写作风格，完成两件事：

            【文章正文】
            %s

            【任务】
            1. 从【语气】【词汇】【句式】【结构】四个维度拆解风格特征。每条特征必须具体、可模仿，禁止空泛形容（不要写「语言优美」，要写「多用15字以内短句，段间留白多」这类可执行描述）。
            2. 从原文中逐字摘录 2 个最能代表该风格的片段。

            【输出 JSON 结构】
            {"excerpt1":"原文中最能代表风格的连续片段，不超过120字，必须逐字摘自原文","excerpt2":"另一个代表性片段，不超过80字，必须逐字摘自原文，且不与excerpt1重复","prompt":"不超过1200字的风格提示词"}

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
              5. prompt 字段中若需引用示例词语，必须使用中文直角引号「」，严禁使用英文双引号 "，避免破坏 JSON 格式。
            """;

    private final SkillAnalyzeAiService aiService;
    private final BenefitService benefitService;
    private final ObjectMapper objectMapper;
    private final ObjectMapper lenientObjectMapper = createLenientObjectMapper();

    private static ObjectMapper createLenientObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        return mapper;
    }

    /** 学习我的风格月度额度权益编码（basic=0/pro=1/flagship=2）。 */
    private static final String LEARN_ANALYZE_BENEFIT = "skill_learn_analyze";

    @Override
    public BenefitCheckVO preConsume(Long userId) {
        return benefitService.preConsume(userId, LEARN_ANALYZE_BENEFIT);
    }

    @Override
    public SkillAnalyzeVO analyze(Long userId, String text) {
        // 统一截断：超过 1000 字只取前 1000 字学习
        if (text != null && text.length() > TEXT_MAX_LENGTH) {
            text = text.substring(0, TEXT_MAX_LENGTH);
        }

        String aiResp = aiService.call(SYSTEM_MESSAGE, USER_PROMPT_TEMPLATE.replace("%s", text));
        JsonNode root = parseJson(stripCodeFence(aiResp));

        String prompt = root.path("prompt").asText("").trim();
        validatePrompt(prompt);

        SkillAnalyzeVO vo = new SkillAnalyzeVO();
        vo.setPrompt(prompt);
        vo.setExcerpt1(resolveExcerpt(root.path("excerpt1").asText(""), text, true));
        vo.setExcerpt2(resolveExcerpt(root.path("excerpt2").asText(""), text, false));
        return vo;
    }

    @Override
    public void confirmConsume(Long userId) {
        benefitService.confirmPreConsume(userId, LEARN_ANALYZE_BENEFIT);
    }

    @Override
    public void cancelConsume(Long userId) {
        benefitService.cancelPreConsume(userId, LEARN_ANALYZE_BENEFIT);
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
