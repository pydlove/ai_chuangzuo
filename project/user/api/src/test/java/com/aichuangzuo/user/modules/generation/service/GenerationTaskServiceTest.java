package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationSubmitRequest;
import com.aichuangzuo.user.modules.generation.mapper.GenerationActiveModelConfigMapper;
import com.aichuangzuo.user.modules.generation.mapper.GenerationTaskMapper;
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
}
