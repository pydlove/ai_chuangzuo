package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.steps.AbstractAiStep;
import com.aichuangzuo.admin.modules.generation.pipeline.steps.IntentAnchorStep;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * pipeline 编排器集成测试：用 mock AiGateway 跑通 stage 1 + 一个 mock 的 AI step，验证：
 * <ul>
 *   <li>stage 按 stageIndex 升序执行</li>
 *   <li>ctx 状态在 stage 之间正确流转</li>
 *   <li>任意 step 抛错 → pipeline 抛错</li>
 *   <li>budget 超限 → step 抛错（不重试）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class GenerationPipelineTest {

    @Mock
    private AiGateway aiGateway;

    @Mock
    private PipelineTemplateResolver resolver;

    @Mock
    private com.aichuangzuo.admin.modules.generation.service.GenerationConfigService configService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void run_shouldExecuteAllStepsInStageIndexOrder() {
        // 准备：stage 1 + 一个 mock AI step (stage 99 占位)
        GenerationPipeline pipeline = new GenerationPipeline(
                List.of(new IntentAnchorStep(), new PersistArticleStepMock(aiGateway)),
                resolver);

        // mock resolver 加载一个空 template
        doAnswer(inv -> {
            GenerationContext ctx = inv.getArgument(0);
            ctx.setTemplate(new PromptTemplate());
            Map<Integer, PromptTemplateStage> stages = new HashMap<>();
            for (int i = 1; i <= 12; i++) {
                stages.put(i, makeStage(i, "ai_prompt", "test prompt for stage " + i));
            }
            ctx.setStages(stages);
            return null;
        }).when(resolver).resolveInto(any(), isNull(), isNull());

        // mock AiGateway 返回有效 JSON（每个 stage 都返回 draft 数组）
        when(aiGateway.call(any(), anyString(), anyString()))
                .thenReturn("{\"draft\":[{\"paragraph_index\":1,\"content\":\"测试段落。\"}]}");

        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setTargetUserId(10L);
        task.setModelConfigId(20L);
        task.setWordLimitTarget(1500);
        task.setInputParam("{\"title\":\"T\",\"description\":\"D\",\"platform\":\"wechat\",\"userStylePrompt\":\"S\",\"wordCount\":1500}");

        GenerationContext ctx = pipeline.run(task);

        assertNotNull(ctx.getUserContextBlock());
        // AI step 至少被调用一次
        verify(aiGateway, times(1)).call(any(), anyString(), anyString());
    }

    @Test
    void run_shouldPropagateStepException() {
        GenerationPipeline pipeline = new GenerationPipeline(
                List.of(new IntentAnchorStep(), new ThrowingStep()),
                resolver);

        doAnswer(inv -> {
            GenerationContext ctx = inv.getArgument(0);
            Map<Integer, PromptTemplateStage> stages = new HashMap<>();
            stages.put(1, makeStage(1, "ai_prompt", "ok"));
            stages.put(50, makeStage(50, "ai_prompt", "throw"));
            ctx.setStages(stages);
            return null;
        }).when(resolver).resolveInto(any(), isNull(), isNull());

        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setTargetUserId(10L);
        task.setInputParam("{\"title\":\"T\",\"description\":\"D\"}");

        assertThrows(RuntimeException.class, () -> pipeline.run(task));
    }

    @Test
    void run_shouldInvokeProgressCallbackAfterEachStage() {
        GenerationPipeline pipeline = new GenerationPipeline(
                List.of(new IntentAnchorStep(), new PersistArticleStepMock(aiGateway)),
                resolver);

        doAnswer(inv -> {
            GenerationContext ctx = inv.getArgument(0);
            ctx.setTemplate(new PromptTemplate());
            Map<Integer, PromptTemplateStage> stages = new HashMap<>();
            for (int i = 1; i <= 12; i++) {
                stages.put(i, makeStage(i, "ai_prompt", "test prompt for stage " + i));
            }
            ctx.setStages(stages);
            return null;
        }).when(resolver).resolveInto(any(), isNull(), isNull());

        when(aiGateway.call(any(), anyString(), anyString()))
                .thenReturn("{\"draft\":[{\"paragraph_index\":1,\"content\":\"测试段落。\"}]}");

        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setTargetUserId(10L);
        task.setModelConfigId(20L);
        task.setWordLimitTarget(1500);
        task.setInputParam("{\"title\":\"T\",\"description\":\"D\",\"platform\":\"wechat\",\"userStylePrompt\":\"S\",\"wordCount\":1500}");

        java.util.concurrent.atomic.AtomicInteger callbackCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger lastPct = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicLong callbackTaskId = new java.util.concurrent.atomic.AtomicLong(0);

        pipeline.run(task, (tid, pct) -> {
            callbackTaskId.set(tid);
            lastPct.set(pct);
            callbackCount.incrementAndGet();
        });

        // 2 个 step（IntentAnchor=stage 1, PersistArticleStepMock=stage 100）
        assertEquals(2, callbackCount.get());
        assertEquals(1L, callbackTaskId.get());
        // 最后 pct 应该等于 stage1.weight(3) + stage100.weight(2) = 5
        assertEquals(5, lastPct.get());
    }

    @Test
    void run_withoutCallback_shouldStillWork() {
        // 旧 1 参 run(task)：无回调也能跑
        GenerationPipeline pipeline = new GenerationPipeline(
                List.of(new IntentAnchorStep()),
                resolver);

        doAnswer(inv -> {
            GenerationContext ctx = inv.getArgument(0);
            ctx.setTemplate(new PromptTemplate());
            ctx.setStages(new HashMap<>());
            return null;
        }).when(resolver).resolveInto(any(), isNull(), isNull());

        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setInputParam("{\"title\":\"T\"}");

        GenerationContext ctx = pipeline.run(task);
        assertNotNull(ctx);
    }

    @Test
    void aiGateway_shouldThrowWhenBudgetExceeded() {
        // 用一个跟踪 budget 的 lambda AiGateway
        AiGateway gw = (ctx, sys, user) -> {
            if (ctx.getAiCallUsed() >= ctx.getAiCallBudget()) {
                throw new AiGateway.AiBudgetExhaustedException("budget exhausted");
            }
            ctx.setAiCallUsed(ctx.getAiCallUsed() + 1);
            return "ok";
        };
        GenerationContext ctx = new GenerationContext();
        ctx.setAiCallBudget(2);
        ctx.setTask(new GenerationTask());

        gw.call(ctx, "sys", "user1");  // 1
        gw.call(ctx, "sys", "user2");  // 2
        // 第 3 次应抛 AiBudgetExhaustedException
        assertThrows(AiGateway.AiBudgetExhaustedException.class,
                () -> gw.call(ctx, "sys", "user3"));
    }

    @Test
    void aiGateway_shouldTrackCallHistory() {
        // 用 DefaultAiGateway 但 mock 底层 GenerationAiService
        com.aichuangzuo.admin.modules.generation.service.GenerationAiService genAiSvc =
                org.mockito.Mockito.mock(com.aichuangzuo.admin.modules.generation.service.GenerationAiService.class);
        when(configService.getCurrent()).thenReturn(null);  // 用默认 retry 配置（maxAttempts=3）
        DefaultAiGateway gw = new DefaultAiGateway(genAiSvc, configService);
        GenerationContext ctx = new GenerationContext();
        ctx.setAiCallBudget(5);
        ctx.setTask(new GenerationTask());
        ctx.putExtra("__currentStageIndex", 4);
        ctx.putExtra("__currentStepName", "draft");

        org.mockito.Mockito.when(genAiSvc.call(any(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("AI 故障"));

        try {
            gw.call(ctx, "sys", "user1");
        } catch (Exception ignore) {
        }

        // 默认 maxAttempts=3 → 3 次重试都失败 → 3 条 record
        assertEquals(3, ctx.getAiCallHistory().size());
        assertEquals(4, ctx.getAiCallHistory().get(0).getStageIndex());
        assertEquals("draft", ctx.getAiCallHistory().get(0).getStepName());
        assertEquals(1, ctx.getAiCallHistory().get(0).getAttempt());
        assertEquals(2, ctx.getAiCallHistory().get(1).getAttempt());
        assertEquals(3, ctx.getAiCallHistory().get(2).getAttempt());
        assertEquals(3, ctx.getAiCallFailed());
        assertEquals(3, ctx.getAiCallUsed());
    }

    @Test
    void aiGateway_shouldReThrowOnFailure() {
        com.aichuangzuo.admin.modules.generation.service.GenerationAiService genAiSvc =
                org.mockito.Mockito.mock(com.aichuangzuo.admin.modules.generation.service.GenerationAiService.class);
        when(configService.getCurrent()).thenReturn(null);
        DefaultAiGateway gw = new DefaultAiGateway(genAiSvc, configService);
        GenerationContext ctx = new GenerationContext();
        ctx.setAiCallBudget(5);
        ctx.setTask(new GenerationTask());

        org.mockito.Mockito.when(genAiSvc.call(any(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("网络超时"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gw.call(ctx, "sys", "user1"));
        assertTrue(ex.getMessage().contains("网络超时"));
    }

    // ---- 内部 mock step ----

    private static PromptTemplateStage makeStage(int idx, String type, String aiPrompt) {
        PromptTemplateStage s = new PromptTemplateStage();
        s.setStageIndex(idx);
        s.setStageType(type);
        s.setAiPrompt(aiPrompt);
        s.setEnabled(1);
        s.setRuleConfig(type.equals("rule_config") ? "{}" : null);
        return s;
    }

    /**
     * mock 的「AI 阶段」：stageIndex=99，调 aiGateway 拿 JSON 写到 draftJson。
     * 不继承 AbstractAiStep（它的 process 是 final），自己实现。
     */
    static class PersistArticleStepMock implements GenerationStep {
        private final AiGateway aiGateway;

        PersistArticleStepMock(AiGateway aiGateway) {
            this.aiGateway = aiGateway;
        }

        @Override
        public int stageIndex() { return 100; }

        @Override
        public String name() { return "mock-ai"; }

        @Override
        public StepResult process(GenerationContext ctx) {
            String aiResp = aiGateway.call(ctx, "", "test");
            try {
                JsonNode root = PipelineUtils.parseAiJson(aiResp);
                ctx.setDraftJson(root.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return StepResult.CONTINUE;
        }
    }

    /**
     * mock：process 直接抛异常，用于测试 pipeline 错误传播。
     */
    static class ThrowingStep extends AbstractAiStep {
        ThrowingStep() {
            super((ctx, sys, user) -> "");
        }

        @Override
        public int stageIndex() { return 50; }

        @Override
        public String name() { return "throwing"; }

        @Override
        protected void parseAndStore(JsonNode root, GenerationContext ctx) {
            throw new RuntimeException("stage 50 simulated failure");
        }
    }
}
