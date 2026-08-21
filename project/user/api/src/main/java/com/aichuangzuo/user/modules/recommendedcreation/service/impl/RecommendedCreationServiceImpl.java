package com.aichuangzuo.user.modules.recommendedcreation.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationSubmitRequest;
import com.aichuangzuo.user.modules.generation.service.GenerationTaskService;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.UpdateSessionRequest;
import com.aichuangzuo.user.modules.recommendedcreation.entity.RecommendedCreationSession;
import com.aichuangzuo.user.modules.recommendedcreation.enums.RecommendedCreationErrorCode;
import com.aichuangzuo.user.modules.recommendedcreation.mapper.RecommendedCreationSessionMapper;
import com.aichuangzuo.user.modules.recommendedcreation.service.RecommendedCreationService;
import com.aichuangzuo.user.modules.recommendedcreation.vo.AngleOptionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.RecommendedCreationSessionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.TopicOptionVO;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanAiService;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import com.aichuangzuo.user.modules.selfmedia.vo.SelfMediaPlanVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendedCreationServiceImpl implements RecommendedCreationService {

    private static final String PROMPT_TOPICS = "recommend_creation_topics_v1";
    private static final String PROMPT_ANGLES = "recommend_creation_angles_v1";

    private final SelfMediaPlanAiService aiService;
    private final SelfMediaPlanService planService;
    private final GenerationTaskService generationTaskService;
    private final RecommendedCreationSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public RecommendedCreationSessionVO getSession(Long userId) {
        RecommendedCreationSession session = sessionMapper.selectByUserId(userId);
        if (session == null) {
            return null;
        }
        return toVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TopicOptionVO> generateTopics(Long userId) {
        SelfMediaPlanVO plan = requirePlan(userId);
        Map<String, Object> vars = planVars(plan);
        JsonNode root = aiService.callPrompt(PROMPT_TOPICS, vars);
        List<TopicOptionVO> topics = parseTopics(resolveArray(root, "topics"));
        if (topics.isEmpty()) {
            log.warn("[小爱推荐创作] AI 选题返回为空，userId={}", userId);
            throw new BusinessException(RecommendedCreationErrorCode.AI_RESPONSE_INVALID);
        }

        RecommendedCreationSession session = sessionMapper.selectByUserId(userId);
        if (session == null) {
            session = newSession(userId);
        }
        session.setCurrentStep(1);
        session.setTopicsJson(toJson(topics));
        session.setSelectedTopicJson(null);
        session.setAnglesJson(null);
        session.setSelectedAnglesJson(null);
        save(session);
        return topics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AngleOptionVO> generateAngles(Long userId, String topicId) {
        RecommendedCreationSession session = requireSession(userId);
        TopicOptionVO topic = findTopic(session, topicId);
        session.setSelectedTopicJson(toJson(topic));

        SelfMediaPlanVO plan = requirePlan(userId);
        Map<String, Object> vars = planVars(plan);
        vars.put("topicTitle", topic.getTitle());

        JsonNode root = aiService.callPrompt(PROMPT_ANGLES, vars);
        List<AngleOptionVO> angles = parseAngles(resolveArray(root, "angles"));
        if (angles.isEmpty()) {
            log.warn("[小爱推荐创作] AI 观点返回为空，userId={}", userId);
            throw new BusinessException(RecommendedCreationErrorCode.AI_RESPONSE_INVALID);
        }

        session.setCurrentStep(2);
        session.setAnglesJson(toJson(angles));
        session.setSelectedAnglesJson(null);
        save(session);
        return angles;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSession(Long userId, UpdateSessionRequest request) {
        RecommendedCreationSession session = requireSession(userId);
        session.setCurrentStep(request.getCurrentStep());
        if (request.getWordCount() != null) {
            session.setWordCount(request.getWordCount());
        }
        if (request.getPrompt() != null) {
            session.setPrompt(request.getPrompt());
        }
        if (request.getTemplate() != null) {
            session.setTemplate(request.getTemplate());
        }
        if (request.getSelectedAngles() != null) {
            session.setSelectedAnglesJson(toJson(request.getSelectedAngles()));
        }
        save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerationTaskVO submitGeneration(Long userId) {
        RecommendedCreationSession session = requireSession(userId);
        TopicOptionVO topic = fromJson(session.getSelectedTopicJson(), TopicOptionVO.class);
        List<AngleOptionVO> angles = fromJsonList(session.getSelectedAnglesJson(), AngleOptionVO.class);
        if (topic == null || angles == null || angles.isEmpty()) {
            throw new BusinessException(RecommendedCreationErrorCode.SESSION_INCOMPLETE);
        }
        if (session.getWordCount() == null || StringUtils.isBlank(session.getPrompt()) || StringUtils.isBlank(session.getTemplate())) {
            throw new BusinessException(RecommendedCreationErrorCode.SESSION_INCOMPLETE);
        }

        StringBuilder desc = new StringBuilder();
        desc.append("选题：").append(topic.getTitle()).append("\n");
        desc.append("观点：");
        for (int i = 0; i < angles.size(); i++) {
            desc.append(angles.get(i).getText());
            if (i < angles.size() - 1) {
                desc.append("；");
            }
        }
        desc.append("\n创作要求：").append(session.getPrompt());

        GenerationSubmitRequest req = new GenerationSubmitRequest();
        req.setTitle(topic.getTitle());
        req.setDescription(desc.toString());
        req.setPlatform(resolvePlatform(session.getTemplate()));
        req.setWordCount(session.getWordCount());
        req.setTemplate(session.getTemplate());

        GenerationTaskVO task = generationTaskService.submit(req, userId);
        sessionMapper.deleteByIdPhysically(session.getId());
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearSession(Long userId) {
        RecommendedCreationSession session = sessionMapper.selectByUserId(userId);
        if (session != null) {
            sessionMapper.deleteByIdPhysically(session.getId());
        }
    }

    // ---- helpers ----

    private SelfMediaPlanVO requirePlan(Long userId) {
        SelfMediaPlanVO plan = planService.getCurrentPlan(userId);
        if (plan == null) {
            throw new BusinessException(RecommendedCreationErrorCode.SELF_MEDIA_PLAN_REQUIRED);
        }
        return plan;
    }

    private RecommendedCreationSession requireSession(Long userId) {
        RecommendedCreationSession session = sessionMapper.selectByUserId(userId);
        if (session == null) {
            throw new BusinessException(RecommendedCreationErrorCode.SESSION_NOT_FOUND);
        }
        return session;
    }

    private RecommendedCreationSession newSession(Long userId) {
        RecommendedCreationSession s = new RecommendedCreationSession();
        s.setUserId(userId);
        s.setCurrentStep(1);
        s.setStatus("draft");
        s.setTenantId(0L);
        return s;
    }

    private void save(RecommendedCreationSession session) {
        if (session.getId() == null) {
            sessionMapper.insert(session);
        } else {
            sessionMapper.updateById(session);
        }
    }

    private Map<String, Object> planVars(SelfMediaPlanVO plan) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("platform", plan.getPlatformName());
        vars.put("niche", plan.getNicheName());
        vars.put("persona", plan.getPersonaName());
        vars.put("pillars", formatPillars(plan.getPillars()));
        return vars;
    }

    private String formatPillars(List<PillarVO> pillars) {
        if (pillars == null || pillars.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pillars.size(); i++) {
            sb.append(pillars.get(i).getName()).append(" ").append(pillars.get(i).getPercent()).append("%");
            if (i < pillars.size() - 1) {
                sb.append("，");
            }
        }
        return sb.toString();
    }

    private TopicOptionVO findTopic(RecommendedCreationSession session, String topicId) {
        List<TopicOptionVO> topics = fromJsonList(session.getTopicsJson(), TopicOptionVO.class);
        if (topics == null || topics.isEmpty()) {
            throw new BusinessException(RecommendedCreationErrorCode.TOPIC_NOT_FOUND);
        }
        return topics.stream()
                .filter(t -> topicId.equals(t.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(RecommendedCreationErrorCode.TOPIC_NOT_FOUND));
    }

    private JsonNode resolveArray(JsonNode root, String fieldName) {
        if (root == null || root.isMissingNode()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        JsonNode child = root.path(fieldName);
        if (child.isArray()) {
            return child;
        }
        return null;
    }

    private List<TopicOptionVO> parseTopics(JsonNode node) {
        List<TopicOptionVO> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        for (JsonNode n : node) {
            TopicOptionVO vo = new TopicOptionVO();
            vo.setId(n.path("id").asText(""));
            vo.setTitle(n.path("title").asText(""));
            vo.setRisk(n.path("risk").asText("medium"));
            vo.setRiskLabel(n.path("riskLabel").asText(""));
            vo.setCaseCount(n.path("caseCount").asInt(0));
            vo.setRecommendedAngle(n.path("recommendedAngle").asText(""));
            list.add(vo);
        }
        return list;
    }

    private List<AngleOptionVO> parseAngles(JsonNode node) {
        List<AngleOptionVO> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        for (JsonNode n : node) {
            AngleOptionVO vo = new AngleOptionVO();
            vo.setId(n.path("id").asText(""));
            vo.setText(n.path("text").asText(""));
            list.add(vo);
        }
        return list;
    }

    private String resolvePlatform(String template) {
        if (StringUtils.isBlank(template)) {
            return "";
        }
        int idx = template.indexOf('-');
        return idx > 0 ? template.substring(0, idx) : template;
    }

    private RecommendedCreationSessionVO toVO(RecommendedCreationSession session) {
        RecommendedCreationSessionVO vo = new RecommendedCreationSessionVO();
        vo.setCurrentStep(session.getCurrentStep());
        vo.setTopics(fromJsonList(session.getTopicsJson(), TopicOptionVO.class));
        vo.setSelectedTopic(fromJson(session.getSelectedTopicJson(), TopicOptionVO.class));
        vo.setAngles(fromJsonList(session.getAnglesJson(), AngleOptionVO.class));
        vo.setSelectedAngles(fromJsonList(session.getSelectedAnglesJson(), AngleOptionVO.class));
        vo.setWordCount(session.getWordCount());
        vo.setPrompt(session.getPrompt());
        vo.setTemplate(session.getTemplate());
        return vo;
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @SneakyThrows
    private <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return objectMapper.readValue(json, clazz);
    }

    @SneakyThrows
    private <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }
}
