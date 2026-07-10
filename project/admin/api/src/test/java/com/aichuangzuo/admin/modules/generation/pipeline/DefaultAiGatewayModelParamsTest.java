package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.shared.entity.GenerationTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 DefaultAiGateway 把 ctx.modelParams 透传给 GenerationAiService.call 的 4 参版本。
 */
@ExtendWith(MockitoExtension.class)
class DefaultAiGatewayModelParamsTest {

    @Mock
    private GenerationAiService aiService;

    @Mock
    private com.aichuangzuo.admin.modules.generation.service.GenerationConfigService configService;

    @Test
    void call_4arg_shouldPassModelParamsToAiService() {
        when(configService.getCurrent()).thenReturn(null);  // 用默认 retry 配置
        when(aiService.call(anyLong(), anyString(), anyString(), any())).thenReturn("ok");

        DefaultAiGateway gw = new DefaultAiGateway(aiService, configService);
        GenerationContext ctx = new GenerationContext();
        ctx.setAiCallBudget(5);
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setModelConfigId(20L);
        ctx.setTask(task);

        Map<String, Object> params = Map.of("temperature", 0.3, "max_tokens", 1500);
        String result = gw.call(ctx, "sys", "user", params);

        assertEquals("ok", result);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(aiService, times(1)).call(anyLong(), anyString(), anyString(), captor.capture());
        Map<String, Object> passed = captor.getValue();
        assertEquals(0.3, passed.get("temperature"));
        assertEquals(1500, passed.get("max_tokens"));
    }

    @Test
    void call_4arg_shouldHandleNullParams() {
        when(configService.getCurrent()).thenReturn(null);
        when(aiService.call(anyLong(), anyString(), anyString(), any())).thenReturn("ok");

        DefaultAiGateway gw = new DefaultAiGateway(aiService, configService);
        GenerationContext ctx = new GenerationContext();
        ctx.setAiCallBudget(5);
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setModelConfigId(20L);
        ctx.setTask(task);

        gw.call(ctx, "sys", "user", null);

        // 即使 modelParams=null，AiService 也要收到调用（参数为 null，由 AiService 用默认值）
        verify(aiService, times(1)).call(anyLong(), anyString(), anyString(), any());
    }
}