package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.entity.GenerationConfig;
import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.admin.modules.generation.service.GenerationConfigService;
import com.aichuangzuo.shared.entity.GenerationTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * DefaultAiGateway 重试行为测试。
 *
 * <p>用反射注入一个 mock 的「clock + sleeper」加速断言；通过 mock GenerationConfigService
 * 返回指定 maxAttempts / baseDelay / multiplier 来覆盖各种重试场景。
 */
@ExtendWith(MockitoExtension.class)
class DefaultAiGatewayRetryTest {

    @Mock
    private GenerationAiService genAiService;

    @Mock
    private GenerationConfigService configService;

    @Test
    void call_shouldRetryUntilSuccess() {
        GenerationConfig cfg = newCfg(3, 100, 2);
        when(configService.getCurrent()).thenReturn(cfg);

        // 第 1 次抛错，第 2 次成功
        when(genAiService.call(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("timeout-1"))
                .thenReturn("{\"ok\":true}");

        DefaultAiGateway gw = newGatewayWithFakeClock(0);  // 跳过 sleep

        GenerationContext ctx = newCtx(10);
        String result = gw.call(ctx, "sys", "user");

        assertEquals("{\"ok\":true}", result);
        // 1 次失败 + 1 次成功 = 2 条 history
        assertEquals(2, ctx.getAiCallHistory().size());
        assertEquals(1, ctx.getAiCallHistory().get(0).getAttempt());
        assertEquals(2, ctx.getAiCallHistory().get(1).getAttempt());
        assertEquals(1, ctx.getAiCallFailed());
        assertEquals(2, ctx.getAiCallUsed());
        // 第 2 次成功 → aiCallRetried 应该 +1
        assertEquals(1, ctx.getAiCallRetried());
    }

    @Test
    void call_shouldThrowAfterMaxAttempts() {
        GenerationConfig cfg = newCfg(3, 100, 2);
        when(configService.getCurrent()).thenReturn(cfg);

        when(genAiService.call(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("始终失败"));

        DefaultAiGateway gw = newGatewayWithFakeClock(0);

        GenerationContext ctx = newCtx(10);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gw.call(ctx, "sys", "user"));
        assertTrue(ex.getMessage().contains("始终失败"));

        // 3 次都失败 → 3 条 history
        assertEquals(3, ctx.getAiCallHistory().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, ctx.getAiCallHistory().get(i).getAttempt());
            assertEquals(false, ctx.getAiCallHistory().get(i).isSuccess());
        }
        assertEquals(3, ctx.getAiCallFailed());
        assertEquals(3, ctx.getAiCallUsed());
        // 全失败不算 retry 成功 → aiCallRetried 仍为 0
        assertEquals(0, ctx.getAiCallRetried());
    }

    @Test
    void call_shouldRespectBackoffFormula() {
        // baseDelay=100, multiplier=2
        // attempt=1 失败后 → 下次重试睡 100
        // attempt=2 失败后 → 下次重试睡 200
        // attempt=3 失败后 → 下次重试睡 400
        // attempt=4 失败后 → 下次重试睡 800（封顶 60s）
        try {
            java.lang.reflect.Method m = DefaultAiGateway.class
                    .getDeclaredMethod("backoffMs", int.class, int.class, int.class);
            m.setAccessible(true);
            assertEquals(100L, m.invoke(null, 100, 2, 1));
            assertEquals(200L, m.invoke(null, 100, 2, 2));
            assertEquals(400L, m.invoke(null, 100, 2, 3));
            assertEquals(800L, m.invoke(null, 100, 2, 4));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void call_shouldUseDefaultConfigWhenConfigServiceReturnsNull() {
        when(configService.getCurrent()).thenReturn(null);

        when(genAiService.call(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("err"));

        DefaultAiGateway gw = newGatewayWithFakeClock(0);

        GenerationContext ctx = newCtx(10);
        assertThrows(RuntimeException.class, () -> gw.call(ctx, "sys", "user"));
        // 默认 maxAttempts=3
        assertEquals(3, ctx.getAiCallHistory().size());
    }

    @Test
    void call_shouldThrowBudgetExhaustedBeforeFirstAttempt() {
        GenerationConfig cfg = newCfg(3, 100, 2);
        when(configService.getCurrent()).thenReturn(cfg);

        DefaultAiGateway gw = newGatewayWithFakeClock(0);

        GenerationContext ctx = newCtx(0);  // budget=0
        // 验证 budget 已用完时直接抛错
        assertThrows(AiGateway.AiBudgetExhaustedException.class,
                () -> gw.call(ctx, "sys", "user"));
        // 没产生任何 history
        assertEquals(0, ctx.getAiCallHistory().size());
    }

    @Test
    void call_shouldInjectLastErrorIntoRetryPrompt() {
        // 测：第 2 次重试时 userMsg 头部多了上次的错误信息
        GenerationConfig cfg = newCfg(3, 100, 2);
        when(configService.getCurrent()).thenReturn(cfg);

        List<String> receivedUserMsgs = new ArrayList<>();
        when(genAiService.call(any(), anyString(), anyString()))
                .thenAnswer(inv -> {
                    receivedUserMsgs.add(inv.getArgument(2));
                    if (receivedUserMsgs.size() == 1) {
                        throw new RuntimeException("第 1 次失败");
                    }
                    return "ok";
                });

        DefaultAiGateway gw = newGatewayWithFakeClock(0);
        GenerationContext ctx = newCtx(10);
        gw.call(ctx, "sys", "original-user-msg");

        assertEquals(2, receivedUserMsgs.size());
        // 首次是原 userMsg
        assertEquals("original-user-msg", receivedUserMsgs.get(0));
        // 第二次重试 userMsg 头部有错误提示
        String retry = receivedUserMsgs.get(1);
        assertTrue(retry.contains("第 1 次失败"));
        assertTrue(retry.contains("original-user-msg"));
    }

    // ---------- helpers ----------

    private GenerationConfig newCfg(int max, int base, int mult) {
        GenerationConfig c = new GenerationConfig();
        c.setLlmRetryMaxAttempts(max);
        c.setLlmRetryBaseDelayMs(base);
        c.setLlmRetryBackoffMultiplier(mult);
        return c;
    }

    private GenerationContext newCtx(int budget) {
        GenerationContext ctx = new GenerationContext();
        ctx.setAiCallBudget(budget);
        GenerationTask t = new GenerationTask();
        t.setId(1L);
        t.setModelConfigId(10L);
        ctx.setTask(t);
        ctx.putExtra("__currentStageIndex", 4);
        ctx.putExtra("__currentStepName", "draft");
        return ctx;
    }

    /**
     * 创建一个 DefaultAiGateway 但把 sleeper 换成「立即返回」（不真睡）。
     * 通过反射覆盖 final 字段。
     */
    private DefaultAiGateway newGatewayWithFakeClock(long ignored) {
        DefaultAiGateway gw = new DefaultAiGateway(genAiService, configService);
        ReflectionTestUtils.setField(gw, "sleeper", (java.util.function.LongConsumer) (ms) -> {
            // 不睡，加速测试
        });
        return gw;
    }
}
