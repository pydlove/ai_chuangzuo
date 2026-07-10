package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationSubmitRequest;
import com.aichuangzuo.user.modules.generation.mapper.GenerationActiveModelConfigMapper;
import com.aichuangzuo.user.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.user.modules.generation.mapper.UserPromptTemplateMapper;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.style.entity.UserStyle;
import com.aichuangzuo.user.modules.style.mapper.UserStyleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    private CoinRecordService coinRecordService;

    @Mock
    private UserStyleMapper userStyleMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GenerationTaskService service;

    private GenerationSubmitRequest sampleRequest(String styleRef) {
        GenerationSubmitRequest req = new GenerationSubmitRequest();
        req.setTitle("t");
        req.setDescription("d");
        req.setPlatform("wechat");
        req.setStyleRef(styleRef);
        req.setWordCount(1500);
        return req;
    }

    private void stubCommonFlow(Long userId) {
        when(benefitResolver.ratePerMinute(userId)).thenReturn(5);
        when(activeModelConfigMapper.selectActiveId()).thenReturn(10L);
        when(coinRecordService.getBalance(userId)).thenReturn(BigDecimal.TEN);
        when(benefitResolver.retentionDays(userId)).thenReturn(30);
        // 默认模板 id=1 已发布，latestPublishedVersion=1（submit 路径需要）
        PromptTemplate tpl = new PromptTemplate();
        tpl.setId(com.aichuangzuo.shared.creative.CreativeTemplateConstants.DEFAULT_TEMPLATE_ID);
        tpl.setTemplateStatus(com.aichuangzuo.shared.creative.TemplateStatus.PUBLISHED.code);
        tpl.setLatestPublishedVersion(1);
        when(promptTemplateMapper.selectById(any())).thenReturn(tpl);
    }

    @Test
    void submit_shouldSnapshotUserStylePromptWhenStyleExists() throws Exception {
        Long userId = 1L;
        UserStyle style = new UserStyle();
        style.setStyleName("轻松");
        style.setPrompt("请用轻松活泼的语气");

        stubCommonFlow(userId);
        when(userStyleMapper.selectOne(any())).thenReturn(style);

        service.submit(sampleRequest("轻松"), userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        Map<String, Object> parsed = objectMapper.readValue(captor.getValue().getInputParam(), Map.class);
        assertEquals("请用轻松活泼的语气", parsed.get("userStylePrompt"));
    }

    @Test
    void submit_shouldSnapshotEmptyStringWhenStyleNotFound() throws Exception {
        Long userId = 2L;

        stubCommonFlow(userId);
        when(userStyleMapper.selectOne(any())).thenReturn(null);

        service.submit(sampleRequest("不存在的风格"), userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        Map<String, Object> parsed = objectMapper.readValue(captor.getValue().getInputParam(), Map.class);
        assertEquals("", parsed.get("userStylePrompt"));
    }

    @Test
    void submit_shouldSnapshotEmptyStringWhenStyleRefIsBlank() throws Exception {
        Long userId = 3L;

        stubCommonFlow(userId);

        service.submit(sampleRequest(""), userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        Map<String, Object> parsed = objectMapper.readValue(captor.getValue().getInputParam(), Map.class);
        assertEquals("", parsed.get("userStylePrompt"));
        // styleRef 为空时根本不应查 DB
        verify(userStyleMapper, never()).selectOne(any());
    }

    @Test
    void getProgress_shouldReturnProgressPctFromTask() {
        Long userId = 5L;
        GenerationTask task = new GenerationTask();
        task.setId(99L);
        task.setTargetUserId(userId);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setRetryCount(0);
        task.setMaxRetry(3);
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
        task.setMaxRetry(3);
        task.setProgressPct(null);  // 还没 worker 拿过
        when(taskMapper.selectById(100L)).thenReturn(task);

        GenerationTaskVO vo = service.getProgress(100L, userId);

        assertEquals(null, vo.getProgressPct());
    }
}
