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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GenerationTaskService service;

    private GenerationSubmitRequest sampleRequest(String skillRef) {
        GenerationSubmitRequest req = new GenerationSubmitRequest();
        req.setTitle("t");
        req.setDescription("d");
        req.setPlatform("wechat");
        req.setSkillRef(skillRef);
        req.setWordCount(1500);
        return req;
    }

    private void stubCommonFlow(Long userId) {
        when(benefitResolver.ratePerMinute(userId)).thenReturn(5);
        when(activeModelConfigMapper.selectActiveId()).thenReturn(10L);
        when(benefitResolver.retentionDays(userId)).thenReturn(30);
        // 唯一已发布模板 id=1，latestPublishedVersion=1（submit 路径需要）
        PromptTemplate tpl = new PromptTemplate();
        tpl.setId(com.aichuangzuo.shared.creative.CreativeTemplateConstants.DEFAULT_TEMPLATE_ID);
        tpl.setTemplateStatus(com.aichuangzuo.shared.creative.TemplateStatus.PUBLISHED.code);
        tpl.setLatestPublishedVersion(1);
        when(promptTemplateMapper.selectPublished()).thenReturn(List.of(tpl));
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
}
