package com.aichuangzuo.admin.modules.generation.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 进度权重合计必须 = 100，否则 progressPct 累加结果不为 100。
 * 详见 spec §5.3。
 */
class ProgressWeightTest {

    @Test
    void allStageWeights_shouldSumTo100() {
        int sum = 0;
        for (PipelineStage s : PipelineStage.ALL) {
            sum += s.weight;
        }
        assertEquals(100, sum, "13 阶段 + persist 阶段的 weight 合计必须为 100");
    }

    @Test
    void persistArticleWeight_shouldBe2() {
        assertEquals(2, PipelineStage.byIndex(100).weight);
    }
}