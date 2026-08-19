package com.aichuangzuo.user.modules.selfmedia.service.impl;

import com.aichuangzuo.shared.entity.Platform;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.platform.mapper.PlatformMapper;
import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import com.aichuangzuo.user.modules.selfmedia.dto.request.*;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.aichuangzuo.user.modules.selfmedia.enums.SelfMediaPlanErrorCode;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanMapper;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfMediaPlanServiceImpl implements SelfMediaPlanService {

    private final SelfMediaPlanAiService aiService;
    private final SelfMediaPlanMapper planMapper;
    private final PlatformMapper platformMapper;
    private final ObjectMapper objectMapper;

    @Override
    public SelfMediaPlanVO getCurrentPlan(Long userId) {
        SelfMediaPlan plan = planMapper.selectByUserId(userId);
        return plan == null ? null : toVO(plan);
    }

    @Override
    public SelfMediaPlanVO savePlan(Long userId, SavePlanRequest request) {
        validateSave(request);
        SelfMediaPlan existing = planMapper.selectByUserId(userId);
        SelfMediaPlan plan = existing == null ? new SelfMediaPlan() : existing;
        plan.setUserId(userId);
        plan.setPlatformKey(request.getPlatformKey());
        plan.setPlatformName(request.getPlatformName());
        plan.setGoal(request.getGoal());
        plan.setBackground(request.getBackground());
        plan.setHasProduct(Boolean.TRUE.equals(request.getHasProduct()) ? 1 : 0);
        plan.setProductDesc(request.getProductDesc());
        plan.setNicheKey(request.getNicheKey());
        plan.setNicheName(request.getNicheName());
        plan.setPersonaKey(request.getPersonaKey());
        plan.setPersonaName(request.getPersonaName());
        plan.setIsRecommendedByAi(Boolean.TRUE.equals(request.getIsRecommendedByAI()) ? 1 : 0);
        plan.setContentPillarsJson(toJson(request.getPillars()));
        plan.setRecommendationContextJson(toJson(request.getRecommendationContext()));
        plan.setTenantId(0L);
        if (existing == null) {
            planMapper.insert(plan);
        } else {
            planMapper.updateById(plan);
        }
        return getCurrentPlan(userId);
    }

    @Override
    public RecommendPlatformResultVO recommendPlatform(Long userId, RecommendPlatformRequest request) {
        SelfMediaRecommendationContext ctx = request.getContext();
        List<Platform> platforms = platformMapper.selectList(
                new LambdaQueryWrapper<Platform>()
                        .eq(Platform::getStatus, 1)
                        .orderByAsc(Platform::getSortOrder));
        String platformsJson = toJson(platforms.stream().map(p -> Map.of(
                "platformKey", p.getPlatformKey(),
                "platformName", p.getPlatformName(),
                "tagline", defaultString(p.getTagline()),
                "contentForm", jsonListToString(p.getContentFormJson()),
                "monetization", jsonListToString(p.getMonetizationJson()),
                "bestFor", defaultString(p.getBestFor())
        )).collect(Collectors.toList()));

        Map<String, Object> vars = contextVars(ctx);
        vars.put("platformsJson", platformsJson);
        JsonNode root = aiService.callPrompt("self_media_recommend_platform_v1", vars);
        String platformKey = root.path("platformKey").asText("");
        Platform chosen = platforms.stream()
                .filter(p -> p.getPlatformKey().equals(platformKey))
                .findFirst()
                .orElse(platforms.isEmpty() ? null : platforms.get(0));
        if (chosen == null) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        }
        RecommendPlatformResultVO vo = new RecommendPlatformResultVO();
        vo.setPlatformKey(chosen.getPlatformKey());
        vo.setPlatformName(chosen.getPlatformName());
        vo.setReason(root.path("reason").asText(""));
        return vo;
    }

    @Override
    public List<GoalOptionVO> recommendGoals(Long userId, RecommendGoalsRequest request) {
        Platform platform = requirePlatform(request.getPlatformKey());
        Map<String, Object> vars = platformVars(platform);
        vars.put("background", defaultString(request.getBackground()));
        vars.putAll(contextVars(request.getContext()));
        JsonNode root = aiService.callPrompt("self_media_recommend_goals_v1", vars);
        return parseGoals(root.path("goals"));
    }

    @Override
    public List<NicheOptionVO> recommendNiches(Long userId, RecommendNichesRequest request) {
        Platform platform = requirePlatform(request.getPlatformKey());
        Map<String, Object> vars = platformVars(platform);
        vars.put("goal", defaultString(request.getGoal()));
        vars.put("background", defaultString(request.getBackground()));
        vars.put("hasProduct", Boolean.TRUE.equals(request.getHasProduct()) ? "有" : "没有");
        vars.put("productDesc", defaultString(request.getProductDesc()));
        vars.putAll(contextVars(request.getContext()));
        JsonNode root = aiService.callPrompt("self_media_recommend_niches_v1", vars);
        return parseNiches(root.path("niches"));
    }

    @Override
    public RecommendPersonasResultVO recommendPersonas(Long userId, RecommendPersonasRequest request) {
        Platform platform = requirePlatform(request.getPlatformKey());
        Map<String, Object> vars = platformVars(platform);
        vars.put("goal", defaultString(request.getGoal()));
        vars.put("background", defaultString(request.getBackground()));
        vars.put("nicheKey", defaultString(request.getNicheKey()));
        vars.put("nicheName", defaultString(request.getNicheName()));
        vars.putAll(contextVars(request.getContext()));
        JsonNode root = aiService.callPrompt("self_media_recommend_personas_v1", vars);
        RecommendPersonasResultVO vo = new RecommendPersonasResultVO();
        vo.setPersonas(parsePersonas(root.path("personas")));
        vo.setDefaultPillars(parsePillars(root.path("defaultPillars")));
        return vo;
    }

    // ---------- private helpers ----------

    private void validateSave(SavePlanRequest request) {
        if (StringUtils.isBlank(request.getPlatformKey())) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_PLATFORM_REQUIRED);
        }
        if (StringUtils.isBlank(request.getGoal())) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_GOAL_REQUIRED);
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

    private Map<String, Object> contextVars(SelfMediaRecommendationContext ctx) {
        Map<String, Object> vars = new LinkedHashMap<>();
        if (ctx == null) {
            vars.put("workType", "");
            vars.put("timePerWeek", "");
            vars.put("incomeGoal", "");
            vars.put("breakEvenPeriod", "");
            vars.put("contentType", "");
            vars.put("audience", "");
            vars.put("identity", "");
            vars.put("onCamera", "");
            vars.put("note", "");
            return vars;
        }
        vars.put("workType", defaultString(ctx.getWorkType()));
        vars.put("timePerWeek", defaultString(ctx.getTimePerWeek()));
        vars.put("incomeGoal", defaultString(ctx.getIncomeGoal()));
        vars.put("breakEvenPeriod", defaultString(ctx.getBreakEvenPeriod()));
        vars.put("contentType", defaultString(ctx.getContentType()));
        vars.put("audience", defaultString(ctx.getAudience()));
        vars.put("identity", defaultString(ctx.getIdentity()));
        vars.put("onCamera", defaultString(ctx.getOnCamera()));
        vars.put("note", defaultString(ctx.getNote()));
        return vars;
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
        vo.setGoal(plan.getGoal());
        vo.setBackground(plan.getBackground());
        vo.setHasProduct(Integer.valueOf(1).equals(plan.getHasProduct()));
        vo.setProductDesc(plan.getProductDesc());
        vo.setNicheKey(plan.getNicheKey());
        vo.setNicheName(plan.getNicheName());
        vo.setPersonaKey(plan.getPersonaKey());
        vo.setPersonaName(plan.getPersonaName());
        vo.setIsRecommendedByAI(Integer.valueOf(1).equals(plan.getIsRecommendedByAi()));
        vo.setPillars(parsePillarsJson(plan.getContentPillarsJson()));
        vo.setRecommendationContext(parseContextJson(plan.getRecommendationContextJson()));
        return vo;
    }

    private List<PillarVO> parsePillarsJson(String json) {
        if (StringUtils.isBlank(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PillarVO>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private SelfMediaRecommendationContext parseContextJson(String json) {
        if (StringUtils.isBlank(json)) return new SelfMediaRecommendationContext();
        try {
            return objectMapper.readValue(json, SelfMediaRecommendationContext.class);
        } catch (Exception e) {
            return new SelfMediaRecommendationContext();
        }
    }

    private List<GoalOptionVO> parseGoals(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    GoalOptionVO vo = new GoalOptionVO();
                    vo.setKey(n.path("key").asText(""));
                    vo.setName(n.path("name").asText(""));
                    vo.setDescription(n.path("description").asText(""));
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
