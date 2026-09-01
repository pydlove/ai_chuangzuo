package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.shared.vo.AiDetectReport;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 14 阶段：质量检测。
 *
 * <p>在发布描述之后、落库之前调用一次大模型，对最终稿给出 0-100 的综合质量分数，
 * 并映射为"低 / 中 / 高 / 极高"等级。结果写入 {@code ctx.aiDetectReport}，
 * 由 {@link PersistArticleStep} 随文章一起落库，供前端展示质量检测报告。
 */
@Component
public class AiDetectStep extends AbstractAiStep {

    public AiDetectStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() {
        return 14;
    }

    @Override
    public String name() {
        return "ai-detect";
    }

    @Override
    protected String systemMessage() {
        return "你是一位资深的文章质量评估专家，擅长判断文章质量并识别是否存在明显的 AI 生成痕迹。你只输出合法 JSON。";
    }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        AiDetectReport report = new AiDetectReport();

        int score = clampScore(root.path("score").asInt(0));
        // 兼容旧模型仍返回 ai_rate 的情况：ai_rate 越低则质量越高
        if (score == 0 && root.path("ai_rate").isInt()) {
            score = clampScore(100 - root.path("ai_rate").asInt(0));
        }
        report.setScore(score);
        report.setQualityLevel(toQualityLevel(score));

        report.setSummary(root.path("summary").asText("").trim());

        // 旧字段兼容解析，新 prompt 不再要求返回
        JsonNode aiRateNode = root.path("ai_rate");
        if (aiRateNode.isInt()) {
            report.setAiRate(clampRate(aiRateNode.asInt()));
        }
        JsonNode suspiciousRateNode = root.path("suspicious_rate");
        if (suspiciousRateNode.isInt()) {
            report.setSuspiciousRate(clampRate(suspiciousRateNode.asInt()));
        }
        JsonNode humanRateNode = root.path("human_rate");
        if (humanRateNode.isInt()) {
            report.setHumanRate(clampRate(humanRateNode.asInt()));
        }

        ctx.setAiDetectReport(report);
    }

    private static int clampScore(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static int clampRate(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static String toQualityLevel(int score) {
        if (score >= 90) {
            return "极高";
        }
        if (score >= 80) {
            return "高";
        }
        if (score >= 60) {
            return "中";
        }
        return "低";
    }
}
