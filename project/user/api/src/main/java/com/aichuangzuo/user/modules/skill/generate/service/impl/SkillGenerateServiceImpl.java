package com.aichuangzuo.user.modules.skill.generate.service.impl;

import com.aichuangzuo.shared.enums.error.SkillErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.LlmJsonParser;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.skill.generate.service.SkillGenerateDailyLimiter;
import com.aichuangzuo.user.modules.skill.generate.service.SkillGenerateService;
import com.aichuangzuo.user.modules.skill.generate.vo.SkillGenerateStatusVO;
import com.aichuangzuo.user.modules.skill.generate.vo.SkillGenerateVO;
import com.aichuangzuo.user.modules.skill.service.SkillAnalyzeAiService;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 小爱帮写服务实现。
 *
 * <p>系统提示词见 c_ai_prompt 表 skill_generate_v1（文档：docs/提示词/skill_generate_v1.txt），
 * 框架参照 self_media_platform_questions_v2.txt。
 */
@Slf4j
@Service
public class SkillGenerateServiceImpl implements SkillGenerateService {

    private static final String BENEFIT_CODE_SKILL_AI_GENERATE = "skill_ai_generate";
    private static final String PROMPT_CODE_SKILL_GENERATE = "skill_generate_v1";
    /** 每日帮写上限。 */
    private static final int DAILY_LIMIT = 4;

    private static final int REQUIREMENT_MAX_LENGTH = 1000;
    private static final int SKILL_NAME_MAX = 20;
    private static final int DESCRIPTION_MAX = 100;
    private static final int SCOPE_TAG_MAX = 3;
    private static final int SCOPE_TAG_LENGTH_MAX = 8;
    private static final int PROMPT_MAX_LENGTH = 1200;

    private final BenefitService benefitService;
    private final SkillGenerateDailyLimiter dailyLimiter;
    private final AiPromptRenderService aiPromptRenderService;
    private final SkillAnalyzeAiService aiService;
    private final ObjectMapper lenientObjectMapper = createLenientObjectMapper();

    public SkillGenerateServiceImpl(BenefitService benefitService,
                                    SkillGenerateDailyLimiter dailyLimiter,
                                    AiPromptRenderService aiPromptRenderService,
                                    SkillAnalyzeAiService aiService) {
        this.benefitService = benefitService;
        this.dailyLimiter = dailyLimiter;
        this.aiPromptRenderService = aiPromptRenderService;
        this.aiService = aiService;
    }

    private static ObjectMapper createLenientObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        return mapper;
    }

    @Override
    public SkillGenerateStatusVO status(Long userId) {
        BenefitCheckVO benefitCheck = benefitService.check(userId, BENEFIT_CODE_SKILL_AI_GENERATE);
        int used = dailyLimiter.currentCount(userId, java.time.LocalDate.now());
        SkillGenerateStatusVO vo = new SkillGenerateStatusVO();
        vo.setAllowed(Boolean.TRUE.equals(benefitCheck.getAllowed()));
        vo.setDailyLimit(DAILY_LIMIT);
        vo.setRemainingToday(Math.max(0, DAILY_LIMIT - used));
        return vo;
    }

    @Override
    public SkillGenerateVO generate(Long userId, String requirement) {
        BenefitCheckVO benefitCheck = benefitService.check(userId, BENEFIT_CODE_SKILL_AI_GENERATE);
        if (!Boolean.TRUE.equals(benefitCheck.getAllowed())) {
            throw new BusinessException(SkillErrorCode.SKILL_GENERATE_PLAN_REQUIRED);
        }

        int attemptCount = dailyLimiter.checkAndIncrement(userId, DAILY_LIMIT);
        int remainingToday = Math.max(0, DAILY_LIMIT - attemptCount);

        String trimmedRequirement = requirement == null ? "" : requirement.trim();
        if (trimmedRequirement.length() > REQUIREMENT_MAX_LENGTH) {
            trimmedRequirement = trimmedRequirement.substring(0, REQUIREMENT_MAX_LENGTH);
        }

        AiPromptRendered rendered = aiPromptRenderService.render(
                PROMPT_CODE_SKILL_GENERATE, Map.of("requirement", trimmedRequirement));
        String aiResp = aiService.call(rendered.systemRole(), rendered.userPrompt());
        SkillGenerateVO vo = parseAndBuild(aiResp);
        vo.setRemainingToday(remainingToday);
        return vo;
    }

    private SkillGenerateVO parseAndBuild(String raw) {
        JsonNode root;
        try {
            root = LlmJsonParser.parseLenient(lenientObjectMapper, raw);
        } catch (Exception e) {
            log.warn("小爱帮写结果解析失败 resp={}", abbreviate(raw, 2000));
            throw new BusinessException(SkillErrorCode.SKILL_GENERATE_FAILED);
        }

        SkillGenerateVO vo = new SkillGenerateVO();
        vo.setSkillName(textOf(root, "skillName"));
        vo.setDescription(textOf(root, "description"));
        vo.setRole(textOf(root, "role"));
        vo.setAudience(textOf(root, "audience"));
        vo.setRequirements(textOf(root, "requirements"));
        vo.setTone(textOf(root, "tone"));
        vo.setRestrictions(textOf(root, "restrictions"));
        vo.setExample(textOf(root, "example"));
        vo.setScopeTags(parseScopeTags(root.path("scopeTags")));

        validate(vo);
        return vo;
    }

    private String textOf(JsonNode root, String field) {
        return root.path(field).asText("").trim();
    }

    private List<String> parseScopeTags(JsonNode node) {
        List<String> tags = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String tag = item.asText("").trim();
                if (!tag.isEmpty()) {
                    tags.add(tag.length() > SCOPE_TAG_LENGTH_MAX ? tag.substring(0, SCOPE_TAG_LENGTH_MAX) : tag);
                }
            }
        }
        return tags.size() > SCOPE_TAG_MAX ? tags.subList(0, SCOPE_TAG_MAX) : tags;
    }

    /** 名称/角色/写作要求非空，拼接后 ≤1200 字，与表单校验口径一致。 */
    private void validate(SkillGenerateVO vo) {
        String name = vo.getSkillName();
        if (name.isEmpty() || name.length() > SKILL_NAME_MAX
                || vo.getRole().isEmpty()
                || vo.getRequirements().isEmpty()
                || vo.getDescription().length() > DESCRIPTION_MAX) {
            log.warn("小爱帮写结果校验失败 nameLength={} roleEmpty={} requirementsEmpty={}",
                    name.length(), vo.getRole().isEmpty(), vo.getRequirements().isEmpty());
            throw new BusinessException(SkillErrorCode.SKILL_GENERATE_FAILED);
        }
        if (buildPrompt(vo).length() > PROMPT_MAX_LENGTH) {
            log.warn("小爱帮写结果拼接提示词超长");
            throw new BusinessException(SkillErrorCode.SKILL_GENERATE_FAILED);
        }
    }

    /** 与前端 buildPromptFromExtra 同一拼接口径，用于长度校验。 */
    private String buildPrompt(SkillGenerateVO vo) {
        StringBuilder sb = new StringBuilder();
        sb.append("- 角色：").append(vo.getRole());
        if (!vo.getAudience().isEmpty()) {
            sb.append("\n\n- 受众：").append(vo.getAudience());
        }
        sb.append("\n\n- 写作要求：\n").append(vo.getRequirements());
        if (!vo.getTone().isEmpty()) {
            sb.append("\n\n- 语气：").append(vo.getTone());
        }
        if (!vo.getRestrictions().isEmpty()) {
            sb.append("\n\n- 禁区：").append(vo.getRestrictions());
        }
        if (!vo.getExample().isEmpty()) {
            sb.append("\n\n- 示例：\n").append(vo.getExample());
        }
        return sb.toString();
    }

    private static String abbreviate(String s, int maxLength) {
        if (s == null) {
            return "null";
        }
        return s.length() <= maxLength ? s : s.substring(0, maxLength) + "...";
    }
}
