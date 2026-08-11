package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationSubmitRequest;
import com.aichuangzuo.user.modules.generation.mapper.GenerationActiveModelConfigMapper;
import com.aichuangzuo.user.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.user.modules.generation.mapper.UserPromptTemplateMapper;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationTaskServiceTest {

    @Mock
    private GenerationTaskMapper taskMapper;

    @Mock
    private UserPromptTemplateMapper promptTemplateMapper;

    @Mock
    private GenerationActiveModelConfigMapper activeModelConfigMapper;

    @Mock
    private GenerationBenefitResolver benefitResolver;

    @Mock
    private GenerationRateLimiter rateLimiter;

    @Mock
    private BenefitService benefitService;

    @Mock
    private UserSkillMapper userSkillMapper;

    @Mock
    private SkillMarketMapper skillMarketMapper;

    @Mock
    private UserMembershipMapper userMembershipMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GenerationTaskService service;

    private GenerationSubmitRequest sampleRequest(String skillRef) {
        return sampleRequestWithWordCount(skillRef, 1500);
    }

    private GenerationSubmitRequest sampleRequestWithWordCount(String skillRef, int wordCount) {
        GenerationSubmitRequest req = new GenerationSubmitRequest();
        req.setTitle("t");
        req.setDescription("d");
        req.setPlatform("wechat");
        req.setSkillRef(skillRef);
        req.setWordCount(wordCount);
        return req;
    }

    private void stubCommonFlow(Long userId) {
        when(benefitResolver.ratePerMinute(userId)).thenReturn(5);
        when(activeModelConfigMapper.selectActiveId()).thenReturn(10L);
        when(benefitResolver.retentionDays(userId)).thenReturn(30);
        when(benefitService.getPlanBenefitValue(userId, "generation_word_limit", "500")).thenReturn("1500");
        when(benefitService.getPlanBenefitValue(userId, "queue_max_tasks", "1")).thenReturn("1");
        when(taskMapper.countUserTasks(any(), any())).thenReturn(0L);
        when(userMembershipMapper.selectByUserId(userId)).thenReturn(activeBasicMembership());
        // 唯一已发布模板 id=1，latestPublishedVersion=1（submit 路径需要）
        PromptTemplate tpl = new PromptTemplate();
        tpl.setId(com.aichuangzuo.shared.creative.CreativeTemplateConstants.DEFAULT_TEMPLATE_ID);
        tpl.setTemplateStatus(com.aichuangzuo.shared.creative.TemplateStatus.PUBLISHED.code);
        tpl.setLatestPublishedVersion(1);
        when(promptTemplateMapper.selectPublished()).thenReturn(List.of(tpl));
    }

    private UserMembership activeBasicMembership() {
        UserMembership m = new UserMembership();
        m.setUserId(0L);
        m.setLevel("basic");
        m.setExpiresAt(java.time.LocalDate.now().plusDays(30));
        return m;
    }

    private void stubPreSkillValidation(Long userId) {
        when(benefitResolver.ratePerMinute(userId)).thenReturn(5);
        when(benefitService.getPlanBenefitValue(userId, "generation_word_limit", "500")).thenReturn("1500");
        when(benefitService.getPlanBenefitValue(userId, "queue_max_tasks", "1")).thenReturn("1");
        when(taskMapper.countUserTasks(any(), any())).thenReturn(0L);
    }

    @Test
    void submit_shouldSnapshotUserSkillPromptWhenSkillExists() throws Exception {
        Long userId = 1L;
        UserSkill skill = new UserSkill();
        skill.setSkillName("轻松");
        skill.setPrompt("请用轻松活泼的语气");

        stubCommonFlow(userId);
        when(userSkillMapper.selectOne(any())).thenReturn(skill);

        service.submit(sampleRequest("轻松"), userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        Map<String, Object> parsed = objectMapper.readValue(captor.getValue().getInputParam(), Map.class);
        assertEquals("请用轻松活泼的语气", parsed.get("userSkillPrompt"));
    }

    @Test
    void submit_shouldSnapshotEmptyStringWhenSkillNotFound() throws Exception {
        Long userId = 2L;

        stubCommonFlow(userId);
        when(userSkillMapper.selectOne(any())).thenReturn(null);

        service.submit(sampleRequest("不存在的风格"), userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        Map<String, Object> parsed = objectMapper.readValue(captor.getValue().getInputParam(), Map.class);
        assertEquals("", parsed.get("userSkillPrompt"));
    }

    @Test
    void submit_shouldSnapshotSystemPresetSkillWhenUserSkillMissing() throws Exception {
        Long userId = 11L;
        UserSkill systemSkill = new UserSkill();
        systemSkill.setSkillName("正式");
        systemSkill.setPrompt("请用正式严谨的语气");

        stubCommonFlow(userId);
        // 第一次查用户自定义风格返回空，第二次查系统预设风格命中
        when(userSkillMapper.selectOne(any())).thenReturn(null, systemSkill);

        service.submit(sampleRequest("正式"), userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        Map<String, Object> parsed = objectMapper.readValue(captor.getValue().getInputParam(), Map.class);
        assertEquals("请用正式严谨的语气", parsed.get("userSkillPrompt"));
        verify(userSkillMapper, times(2)).selectOne(any());
    }

    @Test
    void submit_shouldSnapshotEmptyStringWhenSkillRefIsBlank() throws Exception {
        Long userId = 3L;

        stubCommonFlow(userId);

        service.submit(sampleRequest(""), userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        Map<String, Object> parsed = objectMapper.readValue(captor.getValue().getInputParam(), Map.class);
        assertEquals("", parsed.get("userSkillPrompt"));
        // skillRef 为空时根本不应查 DB
        verify(userSkillMapper, never()).selectOne(any());
    }

    @Test
    void submit_shouldLockUniquePublishedTemplate() {
        Long userId = 9L;
        stubCommonFlow(userId);
        PromptTemplate tpl = new PromptTemplate();
        tpl.setId(7L);
        tpl.setTemplateStatus(com.aichuangzuo.shared.creative.TemplateStatus.PUBLISHED.code);
        tpl.setLatestPublishedVersion(3);
        when(promptTemplateMapper.selectPublished()).thenReturn(List.of(tpl));

        service.submit(sampleRequest(""), userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        assertEquals(7L, captor.getValue().getPromptTemplateId());
        assertEquals(3, captor.getValue().getPromptTemplateVersion());
    }

    @Test
    void submit_shouldFailWhenNoPublishedTemplate() {
        Long userId = 10L;
        when(benefitResolver.ratePerMinute(userId)).thenReturn(5);
        when(benefitService.getPlanBenefitValue(userId, "generation_word_limit", "500")).thenReturn("1500");
        when(benefitService.getPlanBenefitValue(userId, "queue_max_tasks", "1")).thenReturn("1");
        when(taskMapper.countUserTasks(any(), any())).thenReturn(0L);
        when(activeModelConfigMapper.selectActiveId()).thenReturn(10L);
        when(promptTemplateMapper.selectPublished()).thenReturn(List.of());

        com.aichuangzuo.shared.exception.BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.aichuangzuo.shared.exception.BusinessException.class,
                        () -> service.submit(sampleRequest(""), userId));
        assertEquals(
                com.aichuangzuo.shared.enums.error.UserGenerationErrorCode
                        .GENERATION_TEMPLATE_DISABLED.getCode(),
                e.getCode());
    }

    @Test
    void submit_shouldRejectWhenWordCountExceedsPlanLimit() {
        Long userId = 12L;
        when(benefitResolver.ratePerMinute(userId)).thenReturn(5);
        when(benefitService.getPlanBenefitValue(userId, "generation_word_limit", "500")).thenReturn("1500");
        when(benefitService.getPlanBenefitValue(userId, "queue_max_tasks", "1")).thenReturn("1");
        when(taskMapper.countUserTasks(any(), any())).thenReturn(0L);

        com.aichuangzuo.shared.exception.BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.aichuangzuo.shared.exception.BusinessException.class,
                        () -> service.submit(sampleRequestWithWordCount("", 2000), userId));
        assertEquals(
                com.aichuangzuo.shared.enums.error.UserGenerationErrorCode
                        .GENERATION_WORD_LIMIT_EXCEEDS_PLAN.getCode(),
                e.getCode());
        verify(benefitService, never()).consume(any(), any());
        verify(taskMapper, never()).insert(any(GenerationTask.class));
    }

    @Test
    void submit_shouldRejectWhenBasicPlanWordCountExceeds500() {
        Long userId = 14L;
        when(benefitResolver.ratePerMinute(userId)).thenReturn(5);
        when(benefitService.getPlanBenefitValue(userId, "generation_word_limit", "500")).thenReturn("500");
        when(benefitService.getPlanBenefitValue(userId, "queue_max_tasks", "1")).thenReturn("1");
        when(taskMapper.countUserTasks(any(), any())).thenReturn(0L);

        com.aichuangzuo.shared.exception.BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.aichuangzuo.shared.exception.BusinessException.class,
                        () -> service.submit(sampleRequestWithWordCount("", 800), userId));
        assertEquals(
                com.aichuangzuo.shared.enums.error.UserGenerationErrorCode
                        .GENERATION_WORD_LIMIT_EXCEEDS_PLAN.getCode(),
                e.getCode());
    }

    @Test
    void submit_shouldRejectWhenQueueLimitExceeded() {
        Long userId = 15L;
        when(benefitService.getPlanBenefitValue(userId, "queue_max_tasks", "1")).thenReturn("1");
        when(taskMapper.countUserTasks(any(), any())).thenReturn(1L);

        com.aichuangzuo.shared.exception.BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.aichuangzuo.shared.exception.BusinessException.class,
                        () -> service.submit(sampleRequest(""), userId));
        assertEquals(
                com.aichuangzuo.shared.enums.error.UserGenerationErrorCode
                        .GENERATION_QUEUE_LIMIT_EXCEEDED.getCode(),
                e.getCode());
        verify(rateLimiter, never()).check(anyLong(), anyInt());
        verify(benefitService, never()).consume(anyLong(), any());
        verify(taskMapper, never()).insert(any(GenerationTask.class));
    }

    @Test
    void submit_shouldAllowWordCountEqualToPlanLimit() {
        Long userId = 13L;
        stubCommonFlow(userId);

        service.submit(sampleRequestWithWordCount("", 1500), userId);

        verify(benefitService).consume(userId, "ai_article_quota");
        verify(taskMapper).insert(any(GenerationTask.class));
    }

    @Test
    void submit_shouldSetPlanPriorityFromCurrentMembership() {
        Long flagshipUserId = 31L;
        stubCommonFlow(flagshipUserId);
        UserMembership flagship = activeBasicMembership();
        flagship.setLevel("flagship");
        when(userMembershipMapper.selectByUserId(flagshipUserId)).thenReturn(flagship);

        service.submit(sampleRequest(""), flagshipUserId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        assertEquals(2, captor.getValue().getPlanPriority());
    }

    @Test
    void submit_shouldSetPlanPriorityZeroWhenMembershipExpired() {
        Long expiredUserId = 32L;
        stubCommonFlow(expiredUserId);
        UserMembership expired = activeBasicMembership();
        expired.setLevel("flagship");
        expired.setExpiresAt(java.time.LocalDate.now().minusDays(1));
        when(userMembershipMapper.selectByUserId(expiredUserId)).thenReturn(expired);

        service.submit(sampleRequest(""), expiredUserId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getPlanPriority());
    }

    @Test
    void getProgress_shouldReturnProgressPctFromTask() {
        Long userId = 5L;
        GenerationTask task = new GenerationTask();
        task.setId(99L);
        task.setTargetUserId(userId);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setRetryCount(0);
        task.setProgressPct(42);  // worker 已跑到 42%
        task.setInputParam("{\"title\":\"测试\"}");
        when(taskMapper.selectById(99L)).thenReturn(task);

        GenerationTaskVO vo = service.getProgress(99L, userId);

        assertEquals(99L, vo.getId());
        assertEquals(42, vo.getProgressPct());
        assertEquals(GenerationTaskStatus.PROCESSING.getCode(), vo.getStatus());
    }

    @Test
    void getProgress_shouldReturnNullProgressForNewTask() {
        Long userId = 5L;
        GenerationTask task = new GenerationTask();
        task.setId(100L);
        task.setTargetUserId(userId);
        task.setStatus(GenerationTaskStatus.QUEUED);
        task.setRetryCount(0);
        task.setProgressPct(null);  // 还没 worker 拿过
        when(taskMapper.selectById(100L)).thenReturn(task);

        GenerationTaskVO vo = service.getProgress(100L, userId);

        assertEquals(null, vo.getProgressPct());
    }

    @Test
    void stop_shouldCancelQueuedTaskAndRefundQuota() {
        Long userId = 6L;
        GenerationTask task = new GenerationTask();
        task.setId(200L);
        task.setTargetUserId(userId);
        task.setStatus(GenerationTaskStatus.QUEUED);
        when(taskMapper.selectById(200L)).thenReturn(task);

        service.stop(200L, userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).updateById(captor.capture());
        assertEquals(GenerationTaskStatus.FAILED, captor.getValue().getStatus());
        assertEquals("用户手动停止", captor.getValue().getFailedReason());
        assertEquals(null, captor.getValue().getLockedBy());
        verify(benefitService).refund(userId, "ai_article_quota");
    }

    @Test
    void stop_shouldCancelProcessingTaskAndRefundQuota() {
        Long userId = 7L;
        GenerationTask task = new GenerationTask();
        task.setId(201L);
        task.setTargetUserId(userId);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setLockedBy("worker-1");
        task.setLeaseUntil(LocalDateTime.now().plusMinutes(5));
        when(taskMapper.selectById(201L)).thenReturn(task);

        service.stop(201L, userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).updateById(captor.capture());
        assertEquals(GenerationTaskStatus.FAILED, captor.getValue().getStatus());
        assertEquals("用户手动停止", captor.getValue().getFailedReason());
        assertEquals(null, captor.getValue().getLockedBy());
        assertEquals(null, captor.getValue().getLeaseUntil());
        verify(benefitService).refund(userId, "ai_article_quota");
    }

    @Test
    void stop_shouldRejectWhenTaskNotOwned() {
        Long userId = 8L;
        GenerationTask task = new GenerationTask();
        task.setId(202L);
        task.setTargetUserId(999L);
        task.setStatus(GenerationTaskStatus.QUEUED);
        when(taskMapper.selectById(202L)).thenReturn(task);

        com.aichuangzuo.shared.exception.BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.aichuangzuo.shared.exception.BusinessException.class,
                        () -> service.stop(202L, userId));
        assertEquals(com.aichuangzuo.shared.enums.error.UserGenerationErrorCode
                .GENERATION_TASK_NOT_FOUND.getCode(), e.getCode());
        verify(benefitService, never()).refund(any(), any());
    }

    @Test
    void stop_shouldRejectWhenTaskAlreadyCompleted() {
        Long userId = 9L;
        GenerationTask task = new GenerationTask();
        task.setId(203L);
        task.setTargetUserId(userId);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        when(taskMapper.selectById(203L)).thenReturn(task);

        com.aichuangzuo.shared.exception.BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.aichuangzuo.shared.exception.BusinessException.class,
                        () -> service.stop(203L, userId));
        assertEquals(com.aichuangzuo.shared.enums.error.UserGenerationErrorCode
                .GENERATION_TASK_INVALID_STATUS.getCode(), e.getCode());
        verify(benefitService, never()).refund(any(), any());
    }

    @Test
    void submit_shouldRejectWhenMarketSkillDeleted() {
        Long userId = 20L;
        stubPreSkillValidation(userId);

        SkillMarket marketSkill = new SkillMarket();
        marketSkill.setBizNo("SK123");
        marketSkill.setIsDeleted(1);
        marketSkill.setEnableStatus(1);
        marketSkill.setAuditStatus(1);
        when(skillMarketMapper.selectOne(any())).thenReturn(marketSkill);

        com.aichuangzuo.shared.exception.BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.aichuangzuo.shared.exception.BusinessException.class,
                        () -> service.submit(sampleRequest("SK123"), userId));
        assertEquals(com.aichuangzuo.shared.enums.error.UserGenerationErrorCode
                .GENERATION_SKILL_NOT_AVAILABLE.getCode(), e.getCode());
        verify(benefitService, never()).consume(any(), any());
        verify(taskMapper, never()).insert(any(GenerationTask.class));
    }

    @Test
    void submit_shouldRejectWhenMarketSkillNotApproved() {
        Long userId = 21L;
        stubPreSkillValidation(userId);

        SkillMarket marketSkill = new SkillMarket();
        marketSkill.setBizNo("SK456");
        marketSkill.setIsDeleted(0);
        marketSkill.setEnableStatus(0);
        marketSkill.setAuditStatus(1);
        when(skillMarketMapper.selectOne(any())).thenReturn(marketSkill);

        com.aichuangzuo.shared.exception.BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.aichuangzuo.shared.exception.BusinessException.class,
                        () -> service.submit(sampleRequest("SK456"), userId));
        assertEquals(com.aichuangzuo.shared.enums.error.UserGenerationErrorCode
                .GENERATION_SKILL_NOT_AVAILABLE.getCode(), e.getCode());
        verify(benefitService, never()).consume(any(), any());
        verify(taskMapper, never()).insert(any(GenerationTask.class));
    }
}
