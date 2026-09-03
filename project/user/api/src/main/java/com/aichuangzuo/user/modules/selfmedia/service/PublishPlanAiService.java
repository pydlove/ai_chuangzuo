package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlanPublishGuide;
import com.aichuangzuo.shared.enums.error.SelfMediaPlanErrorCode;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanMapper;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanPublishGuideMapper;
import com.aichuangzuo.user.modules.selfmedia.util.SelfMediaPlanHashUtil;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import com.aichuangzuo.user.modules.selfmedia.vo.PublishPlanGuideVO;
import com.aichuangzuo.user.modules.selfmedia.vo.SelfMediaPlanVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishPlanAiService {

    private static final String PROMPT_CODE = "publish_plan_guide_v1";

    private final SelfMediaPlanService planService;
    private final SelfMediaPlanAiService selfMediaPlanAiService;
    private final SelfMediaPlanMapper planMapper;
    private final SelfMediaPlanPublishGuideMapper publishGuideMapper;
    private final ObjectMapper objectMapper;

    public PublishPlanGuideVO generatePlan(Long userId, String mainPlatform) {
        if (StringUtils.isBlank(mainPlatform)) {
            throw new BusinessException(SelfMediaPlanErrorCode.PUBLISH_PLAN_PARAM_INVALID);
        }
        SelfMediaPlanVO plan = planService.getCurrentPlan(userId);
        if (plan == null) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_NOT_FOUND);
        }
        SelfMediaPlan planEntity = planMapper.selectByUserId(userId);
        if (planEntity == null) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_NOT_FOUND);
        }

        PublishPlanGuideVO cached = getCachedPlan(planEntity, mainPlatform.trim());
        if (cached != null) {
            log.info("[发布计划] 命中缓存 userId={}, mainPlatform={}", userId, mainPlatform);
            return cached;
        }

        String platformName = StringUtils.defaultString(mainPlatform, plan.getPlatformName());
        String nicheName = StringUtils.defaultString(plan.getNicheName(), "");
        String personaName = StringUtils.defaultString(plan.getPersonaName(), "");
        String contentPillars = buildPillarsText(plan.getPillars());

        Map<String, Object> variables = Map.of(
                "platformName", platformName,
                "nicheName", nicheName,
                "personaName", personaName,
                "contentPillars", contentPillars,
                "mainPlatform", mainPlatform.trim()
        );

        try {
            log.info("[发布计划] 调用 AI 生成 userId={}, mainPlatform={}", userId, mainPlatform);
            JsonNode result = selfMediaPlanAiService.callPrompt(PROMPT_CODE, variables);
            PublishPlanGuideVO vo = parseResult(result);
            saveCachedPlan(planEntity, mainPlatform.trim(), vo);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("发布计划 AI 生成失败 userId={}", userId, e);
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        }
    }

    /**
     * 只读查询缓存的发布计划，不触发 AI 生成。
     *
     * @return 命中缓存返回 {@link PublishPlanGuideVO}，未命中或缓存过期返回 null
     */
    public PublishPlanGuideVO getCachedPlan(Long userId, String mainPlatform) {
        if (StringUtils.isBlank(mainPlatform)) {
            return null;
        }
        SelfMediaPlan plan = planMapper.selectByUserId(userId);
        if (plan == null) {
            return null;
        }
        return getCachedPlan(plan, mainPlatform.trim());
    }

    private PublishPlanGuideVO getCachedPlan(SelfMediaPlan plan, String mainPlatform) {
        String planContentHash = SelfMediaPlanHashUtil.computePlanContentHash(objectMapper, plan);
        SelfMediaPlanPublishGuide guide = publishGuideMapper.selectByUserPlatformAndHash(
                plan.getUserId(), mainPlatform, planContentHash);
        if (guide == null) {
            return null;
        }
        try {
            PublishPlanGuideVO vo = new PublishPlanGuideVO();
            vo.setMainPlatform(objectMapper.readValue(guide.getMainPlatformJson(), PublishPlanGuideVO.MainPlatformPlan.class));
            vo.setColdStart(objectMapper.readValue(guide.getColdStartJson(), PublishPlanGuideVO.ColdStartPlan.class));
            vo.setReposts(objectMapper.readValue(guide.getRepostsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PublishPlanGuideVO.RepostPlan.class)));
            return vo;
        } catch (JsonProcessingException e) {
            log.warn("[发布计划] 缓存反序列化失败 userId={}", plan.getUserId(), e);
            return null;
        }
    }

    private void saveCachedPlan(SelfMediaPlan plan, String mainPlatform, PublishPlanGuideVO vo) {
        try {
            String planContentHash = SelfMediaPlanHashUtil.computePlanContentHash(objectMapper, plan);
            SelfMediaPlanPublishGuide guide = publishGuideMapper.selectByUserPlatformAndHash(
                    plan.getUserId(), mainPlatform, planContentHash);
            boolean isNew = guide == null;
            if (isNew) {
                guide = new SelfMediaPlanPublishGuide();
                guide.setUserId(plan.getUserId());
                guide.setPlanId(plan.getId());
                guide.setMainPlatform(mainPlatform);
                guide.setPlanContentHash(planContentHash);
                guide.setTenantId(plan.getTenantId());
            }
            guide.setPlanUpdatedAt(plan.getUpdatedAt());
            guide.setMainPlatformJson(objectMapper.writeValueAsString(vo.getMainPlatform()));
            guide.setColdStartJson(objectMapper.writeValueAsString(vo.getColdStart()));
            guide.setRepostsJson(objectMapper.writeValueAsString(vo.getReposts()));
            if (isNew) {
                publishGuideMapper.insert(guide);
            } else {
                publishGuideMapper.updateById(guide);
            }
        } catch (JsonProcessingException e) {
            log.warn("[发布计划] 缓存序列化失败 userId={}", plan.getUserId(), e);
        }
    }

    private String buildPillarsText(List<PillarVO> pillars) {
        if (pillars == null || pillars.isEmpty()) {
            return "暂无";
        }
        return pillars.stream()
                .filter(p -> StringUtils.isNotBlank(p.getName()))
                .map(p -> p.getName() + (p.getPercent() != null ? " " + p.getPercent() + "%" : ""))
                .collect(Collectors.joining("，"));
    }

    private PublishPlanGuideVO parseResult(JsonNode result) {
        PublishPlanGuideVO vo = new PublishPlanGuideVO();
        if (result == null) {
            return vo;
        }
        JsonNode mainNode = result.path("mainPlatform");
        PublishPlanGuideVO.MainPlatformPlan main = new PublishPlanGuideVO.MainPlatformPlan();
        main.setPlatform(textOrDefault(mainNode.path("platform"), ""));
        main.setPublishTime(textOrDefault(mainNode.path("publishTime"), ""));
        main.setReason(textOrDefault(mainNode.path("reason"), ""));
        vo.setMainPlatform(main);

        JsonNode coldStartNode = result.path("coldStart");
        PublishPlanGuideVO.ColdStartPlan coldStart = new PublishPlanGuideVO.ColdStartPlan();
        coldStart.setImmediateActions(parseStringList(coldStartNode.path("immediateActions")));
        coldStart.setDuration(textOrDefault(coldStartNode.path("duration"), ""));
        coldStart.setSharingTips(textOrDefault(coldStartNode.path("sharingTips"), ""));
        vo.setColdStart(coldStart);

        List<PublishPlanGuideVO.RepostPlan> reposts = new ArrayList<>();
        JsonNode repostsNode = result.path("reposts");
        if (repostsNode.isArray()) {
            for (JsonNode item : repostsNode) {
                PublishPlanGuideVO.RepostPlan r = new PublishPlanGuideVO.RepostPlan();
                r.setPlatform(textOrDefault(item.path("platform"), ""));
                r.setPublishTime(textOrDefault(item.path("publishTime"), ""));
                r.setTitle(textOrDefault(item.path("title"), ""));
                r.setTags(parseStringList(item.path("tags")));
                r.setImageSuggestions(textOrDefault(item.path("imageSuggestions"), ""));
                r.setTips(textOrDefault(item.path("tips"), ""));
                if (StringUtils.isNotBlank(r.getPlatform())) {
                    reposts.add(r);
                }
            }
        }
        vo.setReposts(reposts);
        return vo;
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && StringUtils.isNotBlank(item.asText())) {
                    list.add(item.asText().trim());
                }
            }
        }
        return list;
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        return node.isMissingNode() || !node.isTextual() || StringUtils.isBlank(node.asText())
                ? defaultValue
                : node.asText().trim();
    }
}
