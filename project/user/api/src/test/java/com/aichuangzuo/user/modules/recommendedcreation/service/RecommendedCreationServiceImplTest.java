package com.aichuangzuo.user.modules.recommendedcreation.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationSubmitRequest;
import com.aichuangzuo.user.modules.generation.service.GenerationTaskService;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.UpdateSessionRequest;
import com.aichuangzuo.user.modules.recommendedcreation.entity.RecommendedCreationSession;
import com.aichuangzuo.user.modules.recommendedcreation.entity.RecommendedCreationTopicHistory;
import com.aichuangzuo.shared.enums.error.RecommendedCreationErrorCode;
import com.aichuangzuo.user.modules.recommendedcreation.mapper.RecommendedCreationSessionMapper;
import com.aichuangzuo.user.modules.recommendedcreation.mapper.RecommendedCreationTopicHistoryMapper;
import com.aichuangzuo.user.modules.recommendedcreation.service.impl.RecommendedCreationServiceImpl;
import com.aichuangzuo.user.modules.recommendedcreation.vo.AngleOptionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.TopicOptionVO;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanAiService;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import com.aichuangzuo.user.modules.selfmedia.vo.SelfMediaPlanVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RecommendedCreationServiceImplTest {

    private final SelfMediaPlanAiService aiService = mock(SelfMediaPlanAiService.class);
    private final SelfMediaPlanService planService = mock(SelfMediaPlanService.class);
    private final GenerationTaskService generationTaskService = mock(GenerationTaskService.class);
    private final RecommendedCreationSessionMapper sessionMapper = mock(RecommendedCreationSessionMapper.class);
    private final RecommendedCreationTopicHistoryMapper topicHistoryMapper = mock(RecommendedCreationTopicHistoryMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RecommendedCreationServiceImpl service() {
        return new RecommendedCreationServiceImpl(aiService, planService, generationTaskService, sessionMapper, topicHistoryMapper, objectMapper);
    }

    private SelfMediaPlanVO mockPlan() {
        SelfMediaPlanVO plan = new SelfMediaPlanVO();
        plan.setPlatformName("小红书");
        plan.setNicheName("35+ 职场转型");
        plan.setPersonaName("实战记录者");
        PillarVO p = new PillarVO();
        p.setName("干货复盘");
        p.setPercent(60);
        plan.setPillars(List.of(p));
        return plan;
    }

    @Test
    void getSession_shouldReturnNullWhenNoSession() {
        when(sessionMapper.selectByUserId(1L)).thenReturn(null);
        assertNull(service().getSession(1L));
    }

    @Test
    void generateTopics_shouldInsertSessionWhenNotExists() throws Exception {
        when(planService.getCurrentPlan(1L)).thenReturn(mockPlan());
        when(topicHistoryMapper.selectTitlesByUserIdSince(eq(1L), any(LocalDateTime.class))).thenReturn(List.of());
        JsonNode root = objectMapper.readTree("{\"topics\":[{\"id\":\"t1\",\"title\":\"选题1\",\"risk\":\"low\",\"riskLabel\":\"低\",\"caseCount\":5,\"recommendedAngle\":\"角度\"}]}");
        when(aiService.callPrompt(any(), any())).thenReturn(root);
        when(sessionMapper.selectByUserId(1L)).thenReturn(null);

        List<TopicOptionVO> topics = service().generateTopics(1L);

        assertEquals(1, topics.size());
        assertEquals("t1", topics.get(0).getId());
        ArgumentCaptor<RecommendedCreationSession> captor = ArgumentCaptor.forClass(RecommendedCreationSession.class);
        verify(sessionMapper).insert(captor.capture());
        RecommendedCreationSession inserted = captor.getValue();
        assertEquals(1, inserted.getCurrentStep());
        assertTrue(inserted.getTopicsJson().contains("选题1"));
    }

    @Test
    void generateTopics_shouldPassRecentTitlesToAiAndSaveHistory() throws Exception {
        when(planService.getCurrentPlan(1L)).thenReturn(mockPlan());
        when(topicHistoryMapper.selectTitlesByUserIdSince(eq(1L), any(LocalDateTime.class))).thenReturn(List.of("旧选题1", "旧选题2"));
        JsonNode root = objectMapper.readTree("{\"topics\":[{\"id\":\"t1\",\"title\":\"新选题1\",\"risk\":\"low\",\"riskLabel\":\"低\",\"caseCount\":5,\"recommendedAngle\":\"角度\"}]}");
        when(aiService.callPrompt(any(), any())).thenReturn(root);
        RecommendedCreationSession session = new RecommendedCreationSession();
        session.setId(100L);
        session.setUserId(1L);
        session.setTenantId(0L);
        when(sessionMapper.selectByUserId(1L)).thenReturn(session);

        List<TopicOptionVO> topics = service().generateTopics(1L);

        assertEquals(1, topics.size());
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aiService).callPrompt(eq("recommend_creation_topics_v1"), varsCaptor.capture());
        assertEquals("[\"旧选题1\",\"旧选题2\"]", varsCaptor.getValue().get("recentlyRecommendedTitles"));

        ArgumentCaptor<RecommendedCreationTopicHistory> historyCaptor = ArgumentCaptor.forClass(RecommendedCreationTopicHistory.class);
        verify(topicHistoryMapper).insert(historyCaptor.capture());
        RecommendedCreationTopicHistory history = historyCaptor.getValue();
        assertEquals("新选题1", history.getTitle());
        assertEquals(100L, history.getSessionId());
        assertEquals(1L, history.getUserId());
    }

    @Test
    void generateTopics_shouldThrowWhenAiReturnsEmpty() throws Exception {
        when(planService.getCurrentPlan(1L)).thenReturn(mockPlan());
        when(topicHistoryMapper.selectTitlesByUserIdSince(eq(1L), any(LocalDateTime.class))).thenReturn(List.of());
        when(aiService.callPrompt(any(), any())).thenReturn(objectMapper.readTree("{\"topics\":[]}"));
        when(sessionMapper.selectByUserId(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service().generateTopics(1L));
        assertEquals(RecommendedCreationErrorCode.AI_RESPONSE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void generateAngles_shouldSaveSelectedTopicAndAngles() throws Exception {
        RecommendedCreationSession session = new RecommendedCreationSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setTopicsJson(objectMapper.writeValueAsString(List.of(new TopicOptionVO() {{ setId("t1"); setTitle("选题1"); }})));
        when(sessionMapper.selectByUserId(1L)).thenReturn(session);
        when(planService.getCurrentPlan(1L)).thenReturn(mockPlan());
        JsonNode root = objectMapper.readTree("{\"angles\":[{\"id\":\"a1\",\"text\":\"观点1\"}]}");
        when(aiService.callPrompt(any(), any())).thenReturn(root);

        List<AngleOptionVO> angles = service().generateAngles(1L, "t1");

        assertEquals(1, angles.size());
        assertEquals(2, session.getCurrentStep());
        assertTrue(session.getSelectedTopicJson().contains("选题1"));
        assertTrue(session.getAnglesJson().contains("观点1"));
        verify(sessionMapper).updateById(session);
    }

    @Test
    void updateSession_shouldUpdateWordCountAndPrompt() {
        RecommendedCreationSession session = new RecommendedCreationSession();
        session.setId(1L);
        session.setUserId(1L);
        when(sessionMapper.selectByUserId(1L)).thenReturn(session);

        UpdateSessionRequest req = new UpdateSessionRequest();
        req.setCurrentStep(3);
        req.setWordCount(1500);

        service().updateSession(1L, req);

        assertEquals(3, session.getCurrentStep());
        assertEquals(1500, session.getWordCount());
        verify(sessionMapper).updateById(session);
    }

    @Test
    void submitGeneration_shouldCallGenerationAndMarkCompleted() throws Exception {
        RecommendedCreationSession session = new RecommendedCreationSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setCurrentStep(5);
        session.setSelectedTopicJson(objectMapper.writeValueAsString(new TopicOptionVO() {{ setId("t1"); setTitle("选题1"); }}));
        session.setSelectedAnglesJson(objectMapper.writeValueAsString(List.of(new AngleOptionVO() {{ setId("a1"); setText("观点1"); }})));
        session.setWordCount(1500);
        session.setPrompt("提示词");
        session.setTemplate("xiaohongshu-default");
        when(sessionMapper.selectByUserId(1L)).thenReturn(session);
        when(generationTaskService.submit(any(GenerationSubmitRequest.class), eq(1L))).thenReturn(new GenerationTaskVO());

        service().submitGeneration(1L);

        ArgumentCaptor<GenerationSubmitRequest> captor = ArgumentCaptor.forClass(GenerationSubmitRequest.class);
        verify(generationTaskService).submit(captor.capture(), eq(1L));
        assertEquals("选题1", captor.getValue().getTitle());
        assertEquals("xiaohongshu", captor.getValue().getPlatform());
        assertEquals(1500, captor.getValue().getWordCount());
        assertEquals("xiaohongshu-default", captor.getValue().getTemplate());
        assertEquals("completed", session.getStatus());
        verify(sessionMapper).updateById(session);
    }

    @Test
    void submitGeneration_shouldThrowWhenSessionIncomplete() {
        RecommendedCreationSession session = new RecommendedCreationSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setCurrentStep(2);
        when(sessionMapper.selectByUserId(1L)).thenReturn(session);

        BusinessException ex = assertThrows(BusinessException.class, () -> service().submitGeneration(1L));
        assertEquals(RecommendedCreationErrorCode.SESSION_INCOMPLETE.getCode(), ex.getCode());
    }

    @Test
    void clearSession_shouldDeleteWhenExists() {
        RecommendedCreationSession session = new RecommendedCreationSession();
        session.setId(1L);
        when(sessionMapper.selectByUserId(1L)).thenReturn(session);

        service().clearSession(1L);

        verify(sessionMapper).deleteByIdPhysically(1L);
    }
}
