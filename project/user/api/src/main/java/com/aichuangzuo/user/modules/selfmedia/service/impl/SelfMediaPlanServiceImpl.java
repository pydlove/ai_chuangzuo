package com.aichuangzuo.user.modules.selfmedia.service.impl;

import com.aichuangzuo.shared.entity.Platform;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.platform.mapper.PlatformMapper;
import com.aichuangzuo.user.modules.selfmedia.dto.QuestionAnswerDTO;
import com.aichuangzuo.user.modules.selfmedia.dto.request.*;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlanNiche;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlanPersona;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlanQuestion;
import com.aichuangzuo.user.modules.selfmedia.enums.SelfMediaPlanErrorCode;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanMapper;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanNicheMapper;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanPersonaMapper;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanQuestionMapper;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanAiService;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfMediaPlanServiceImpl implements SelfMediaPlanService {

    private static final String PROMPT_PLATFORM_QUESTIONS = "self_media_platform_questions_v2";
    private static final String PROMPT_PLATFORM_NICHES = "self_media_platform_niches_v1";
    private static final String PROMPT_PLATFORM_PERSONAS = "self_media_platform_personas_v1";

    private final SelfMediaPlanAiService aiService;
    private final SelfMediaPlanMapper planMapper;
    private final SelfMediaPlanQuestionMapper questionMapper;
    private final SelfMediaPlanNicheMapper nicheMapper;
    private final SelfMediaPlanPersonaMapper personaMapper;
    private final PlatformMapper platformMapper;
    private final ObjectMapper objectMapper;

    @Override
    public SelfMediaPlanVO getCurrentPlan(Long userId) {
        SelfMediaPlan plan = planMapper.selectByUserId(userId);
        return plan == null ? null : toVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SelfMediaPlanVO savePlan(Long userId, SavePlanRequest request) {
        validateSave(request);
        SelfMediaPlan existing = planMapper.selectByUserId(userId);
        SelfMediaPlan plan = existing == null ? new SelfMediaPlan() : existing;
        plan.setUserId(userId);
        plan.setPlatformKey(request.getPlatformKey());
        plan.setPlatformName(request.getPlatformName());
        plan.setNicheKey(request.getNicheKey());
        plan.setNicheName(request.getNicheName());
        plan.setPersonaKey(request.getPersonaKey());
        plan.setPersonaName(request.getPersonaName());
        plan.setContentPillarsJson(toJson(request.getPillars()));
        plan.setAnswersJson(toJson(request.getAnswers()));
        plan.setQuestionPromptCode(PROMPT_PLATFORM_QUESTIONS);
        plan.setTenantId(0L);
        if (existing == null) {
            planMapper.insert(plan);
        } else {
            planMapper.updateById(plan);
        }
        return getCurrentPlan(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<QuestionVO> getOrGeneratePlatformQuestions(Long userId, String platformKey) {
        log.info("[自媒体方案] 开始生成问题，userId={}, platformKey={}", userId, platformKey);
        Platform platform = requirePlatform(platformKey);
        List<SelfMediaPlanQuestion> cached = questionMapper.selectByUserAndPlatform(userId, platformKey);
        if (!cached.isEmpty() && PROMPT_PLATFORM_QUESTIONS.equals(cached.get(0).getPromptCode())) {
            log.info("[自媒体方案] 命中问题缓存，userId={}, platformKey={}, count={}", userId, platformKey, cached.size());
            return cached.stream().map(this::toQuestionVO).toList();
        }

        log.info("[自媒体方案] 调用 AI 生成问题，userId={}, platformKey={}", userId, platformKey);
        Map<String, Object> vars = platformVars(platform);
        JsonNode root = aiService.callPrompt(PROMPT_PLATFORM_QUESTIONS, vars);
        List<QuestionVO> questions = parseQuestions(root.path("questions"));

        // 删除旧问题，保存新问题（物理删除，避免唯一键冲突）
        questionMapper.deleteByUserAndPlatform(userId, platformKey);
        for (QuestionVO q : questions) {
            SelfMediaPlanQuestion entity = new SelfMediaPlanQuestion();
            entity.setUserId(userId);
            entity.setPlatformKey(platformKey);
            entity.setPromptCode(PROMPT_PLATFORM_QUESTIONS);
            entity.setQuestionKey(q.getKey());
            entity.setQuestionText(q.getText());
            entity.setOptionsJson(toJson(q.getOptions()));
            entity.setIsRequired(Boolean.TRUE.equals(q.getIsRequired()) ? 1 : 0);
            entity.setSortOrder(q.getSortOrder() == null ? 0 : q.getSortOrder());
            questionMapper.insert(entity);
        }
        log.info("[自媒体方案] 问题生成完成，userId={}, platformKey={}, count={}", userId, platformKey, questions.size());
        return questions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NicheOptionVO> recommendNiches(Long userId, RecommendNichesRequest request) {
        String platformKey = request.getPlatformKey();
        String hash = answerHash(request.getAnswers());
        log.info("[自媒体方案] 开始推荐赛道，userId={}, platformKey={}, answerHash={}", userId, platformKey, hash);
        Platform platform = requirePlatform(platformKey);
        List<SelfMediaPlanNiche> cached = nicheMapper.selectByUserPlatformAndHash(userId, platformKey, hash);
        if (!cached.isEmpty()) {
            log.info("[自媒体方案] 命中赛道缓存，userId={}, platformKey={}, answerHash={}, count={}",
                    userId, platformKey, hash, cached.size());
            return cached.stream().map(this::toNicheVO).toList();
        }

        log.info("[自媒体方案] 调用 AI 推荐赛道，userId={}, platformKey={}, answerHash={}", userId, platformKey, hash);
        Map<String, Object> vars = platformVars(platform);
        vars.put("questionsAnswersJson", buildQuestionsAnswersJson(userId, platformKey, request.getAnswers()));
        JsonNode root = aiService.callPrompt(PROMPT_PLATFORM_NICHES, vars);
        List<NicheOptionVO> niches = parseNiches(root.path("niches"));

        // 物理删除旧缓存，避免唯一键冲突
        nicheMapper.deleteByUserPlatformAndHash(userId, platformKey, hash);
        for (NicheOptionVO n : niches) {
            SelfMediaPlanNiche entity = new SelfMediaPlanNiche();
            entity.setUserId(userId);
            entity.setPlatformKey(platformKey);
            entity.setAnswerSnapshotHash(hash);
            entity.setAnswerSnapshotJson(toJson(request.getAnswers()));
            entity.setNicheKey(n.getKey());
            entity.setName(n.getName());
            entity.setAudience(n.getAudience());
            entity.setMonetization(n.getMonetization());
            entity.setRiskLabel(n.getRiskLabel());
            entity.setRiskColor(n.getRiskColor());
            entity.setCaseCount(n.getCaseCount());
            entity.setReason(n.getReason());
            nicheMapper.insert(entity);
        }
        log.info("[自媒体方案] 赛道推荐完成，userId={}, platformKey={}, answerHash={}, count={}",
                userId, platformKey, hash, niches.size());
        return niches;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecommendPersonasResultVO recommendPersonas(Long userId, RecommendPersonasRequest request) {
        String platformKey = request.getPlatformKey();
        String nicheKey = request.getNicheKey();
        String hash = answerHash(request.getAnswers());
        log.info("[自媒体方案] 开始推荐人设，userId={}, platformKey={}, nicheKey={}, answerHash={}",
                userId, platformKey, nicheKey, hash);
        Platform platform = requirePlatform(platformKey);
        List<SelfMediaPlanPersona> cached = personaMapper.selectByUserPlatformHashAndNiche(
                userId, platformKey, hash, nicheKey);
        if (!cached.isEmpty()) {
            log.info("[自媒体方案] 命中人设缓存，userId={}, platformKey={}, nicheKey={}, answerHash={}, count={}",
                    userId, platformKey, nicheKey, hash, cached.size());
            RecommendPersonasResultVO vo = new RecommendPersonasResultVO();
            vo.setPersonas(cached.stream().map(this::toPersonaVO).toList());
            vo.setDefaultPillars(parsePillarsJson(cached.get(0).getDefaultPillarsJson()));
            return vo;
        }

        log.info("[自媒体方案] 调用 AI 推荐人设，userId={}, platformKey={}, nicheKey={}, answerHash={}",
                userId, platformKey, nicheKey, hash);
        Map<String, Object> vars = platformVars(platform);
        vars.put("questionsAnswersJson", buildQuestionsAnswersJson(userId, platformKey, request.getAnswers()));
        vars.put("nicheKey", nicheKey);
        String nicheName = findNicheName(userId, platformKey, hash, nicheKey);
        vars.put("nicheName", nicheName);
        JsonNode root = aiService.callPrompt(PROMPT_PLATFORM_PERSONAS, vars);

        List<PersonaOptionVO> personas = parsePersonas(root.path("personas"));
        List<PillarVO> defaultPillars = parsePillars(root.path("defaultPillars"));

        // 物理删除旧缓存，避免唯一键冲突
        personaMapper.deleteByUserPlatformHashAndNiche(userId, platformKey, hash, nicheKey);
        for (PersonaOptionVO p : personas) {
            SelfMediaPlanPersona entity = new SelfMediaPlanPersona();
            entity.setUserId(userId);
            entity.setPlatformKey(platformKey);
            entity.setAnswerSnapshotHash(hash);
            entity.setNicheKey(nicheKey);
            entity.setPersonaKey(p.getKey());
            entity.setName(p.getName());
            entity.setDescription(p.getDesc());
            entity.setDefaultPillarsJson(toJson(defaultPillars));
            personaMapper.insert(entity);
        }

        log.info("[自媒体方案] 人设推荐完成，userId={}, platformKey={}, nicheKey={}, answerHash={}, count={}",
                userId, platformKey, nicheKey, hash, personas.size());
        RecommendPersonasResultVO vo = new RecommendPersonasResultVO();
        vo.setPersonas(personas);
        vo.setDefaultPillars(defaultPillars);
        return vo;
    }

    // ---------- private helpers ----------

    private void validateSave(SavePlanRequest request) {
        if (StringUtils.isBlank(request.getPlatformKey())) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_PLATFORM_REQUIRED);
        }
        if (StringUtils.isBlank(request.getNicheKey())) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_NICHE_REQUIRED);
        }
        if (StringUtils.isBlank(request.getPersonaKey())) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_PERSONA_REQUIRED);
        }
    }

    private Platform requirePlatform(String platformKey) {
        Platform p = platformMapper.selectOne(
                new LambdaQueryWrapper<Platform>()
                        .eq(Platform::getPlatformKey, platformKey)
                        .eq(Platform::getStatus, 1));
        if (p == null) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_PLATFORM_REQUIRED);
        }
        return p;
    }

    private Map<String, Object> platformVars(Platform p) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("platformKey", p.getPlatformKey());
        vars.put("platformName", p.getPlatformName());
        vars.put("platformTagline", defaultString(p.getTagline()));
        vars.put("platformContentForm", jsonListToString(p.getContentFormJson()));
        vars.put("platformMonetization", jsonListToString(p.getMonetizationJson()));
        vars.put("platformBestFor", defaultString(p.getBestFor()));
        return vars;
    }

    private String buildQuestionsAnswersJson(Long userId, String platformKey, List<QuestionAnswerDTO> answers) {
        List<SelfMediaPlanQuestion> questions = questionMapper.selectByUserAndPlatform(userId, platformKey);
        Map<String, String> textMap = questions.stream()
                .collect(Collectors.toMap(SelfMediaPlanQuestion::getQuestionKey, SelfMediaPlanQuestion::getQuestionText, (a, b) -> a));
        List<Map<String, String>> list = answers.stream()
                .sorted(Comparator.comparing(QuestionAnswerDTO::getQuestionKey))
                .map(a -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("questionKey", a.getQuestionKey());
                    m.put("text", textMap.getOrDefault(a.getQuestionKey(), a.getQuestionKey()));
                    m.put("answer", defaultString(a.getAnswer()));
                    return m;
                }).toList();
        return toJson(list);
    }

    private String findNicheName(Long userId, String platformKey, String hash, String nicheKey) {
        List<SelfMediaPlanNiche> list = nicheMapper.selectByUserPlatformAndHash(userId, platformKey, hash);
        return list.stream()
                .filter(n -> nicheKey.equals(n.getNicheKey()))
                .findFirst()
                .map(SelfMediaPlanNiche::getName)
                .orElse("");
    }

    @SneakyThrows
    private String answerHash(List<QuestionAnswerDTO> answers) {
        if (answers == null) return sha256("");
        List<QuestionAnswerDTO> sorted = answers.stream()
                .sorted(Comparator.comparing(QuestionAnswerDTO::getQuestionKey))
                .toList();
        return sha256(toJson(sorted));
    }

    @SneakyThrows
    private String sha256(String input) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private String defaultString(String s) {
        return s == null ? "" : s;
    }

    private String jsonListToString(String json) {
        if (StringUtils.isBlank(json)) return "";
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return String.join("、", list);
        } catch (Exception e) {
            return json;
        }
    }

    @SneakyThrows
    private String toJson(Object value) {
        if (value == null) return null;
        return objectMapper.writeValueAsString(value);
    }

    private SelfMediaPlanVO toVO(SelfMediaPlan plan) {
        SelfMediaPlanVO vo = new SelfMediaPlanVO();
        vo.setPlatformKey(plan.getPlatformKey());
        vo.setPlatformName(plan.getPlatformName());
        vo.setNicheKey(plan.getNicheKey());
        vo.setNicheName(plan.getNicheName());
        vo.setPersonaKey(plan.getPersonaKey());
        vo.setPersonaName(plan.getPersonaName());
        vo.setPillars(parsePillarsJson(plan.getContentPillarsJson()));
        vo.setAnswers(parseAnswersJson(plan.getAnswersJson()));
        return vo;
    }

    private QuestionVO toQuestionVO(SelfMediaPlanQuestion q) {
        QuestionVO vo = new QuestionVO();
        vo.setKey(q.getQuestionKey());
        vo.setText(q.getQuestionText());
        vo.setOptions(parseOptionsJson(q.getOptionsJson()));
        vo.setIsRequired(Integer.valueOf(1).equals(q.getIsRequired()));
        vo.setSortOrder(q.getSortOrder());
        return vo;
    }

    private NicheOptionVO toNicheVO(SelfMediaPlanNiche n) {
        NicheOptionVO vo = new NicheOptionVO();
        vo.setKey(n.getNicheKey());
        vo.setName(n.getName());
        vo.setAudience(n.getAudience());
        vo.setMonetization(n.getMonetization());
        vo.setRiskLabel(n.getRiskLabel());
        vo.setRiskColor(n.getRiskColor());
        vo.setCaseCount(n.getCaseCount());
        vo.setReason(n.getReason());
        return vo;
    }

    private PersonaOptionVO toPersonaVO(SelfMediaPlanPersona p) {
        PersonaOptionVO vo = new PersonaOptionVO();
        vo.setKey(p.getPersonaKey());
        vo.setName(p.getName());
        vo.setDesc(p.getDescription());
        return vo;
    }

    private List<QuestionOptionVO> parseOptionsJson(String json) {
        if (StringUtils.isBlank(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionOptionVO>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<QuestionAnswerDTO> parseAnswersJson(String json) {
        if (StringUtils.isBlank(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionAnswerDTO>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<PillarVO> parsePillarsJson(String json) {
        if (StringUtils.isBlank(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PillarVO>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<QuestionVO> parseQuestions(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    QuestionVO vo = new QuestionVO();
                    vo.setKey(n.path("key").asText(""));
                    vo.setText(n.path("text").asText(""));
                    vo.setOptions(parseQuestionOptions(n.path("options")));
                    vo.setIsRequired(n.path("isRequired").asBoolean(true));
                    vo.setSortOrder(n.path("sortOrder").asInt(0));
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<QuestionOptionVO> parseQuestionOptions(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    QuestionOptionVO vo = new QuestionOptionVO();
                    vo.setKey(n.path("key").asText(""));
                    vo.setLabel(n.path("label").asText(""));
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<NicheOptionVO> parseNiches(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    NicheOptionVO vo = new NicheOptionVO();
                    vo.setKey(n.path("key").asText(""));
                    vo.setName(n.path("name").asText(""));
                    vo.setAudience(n.path("audience").asText(""));
                    vo.setMonetization(n.path("monetization").asText(""));
                    vo.setRiskLabel(n.path("riskLabel").asText(""));
                    vo.setRiskColor(n.path("riskColor").asText(""));
                    vo.setCaseCount(n.path("caseCount").asInt(0));
                    vo.setReason(n.path("reason").asText(""));
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<PersonaOptionVO> parsePersonas(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    PersonaOptionVO vo = new PersonaOptionVO();
                    vo.setKey(n.path("key").asText(""));
                    vo.setName(n.path("name").asText(""));
                    vo.setDesc(n.path("desc").asText(""));
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<PillarVO> parsePillars(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    PillarVO vo = new PillarVO();
                    vo.setName(n.path("name").asText(""));
                    vo.setPercent(n.path("percent").asInt(0));
                    return vo;
                }).collect(Collectors.toList());
    }
}
