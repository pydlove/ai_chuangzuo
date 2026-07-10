package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.entity.PromptTemplate;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 12 阶段流水线执行上下文：步骤间共享的可变状态。
 *
 * <p>每个 step 读自己关心的字段 + 写自己产出的字段，pipeline 负责把它按 stageIndex 串起来。
 */
@Getter
@Setter
public class GenerationContext {

    // ===== 入口固定字段 =====
    /** 当前任务（worker 写入）。 */
    private GenerationTask task;

    /** 启用的 prompt template（resolver 写入）。 */
    private PromptTemplate template;

    /** 12 个 stage 配置（按 stageIndex 索引，resolver 写入）。 */
    private Map<Integer, PromptTemplateStage> stages = new HashMap<>();

    /**
     * 任务锁定的模板版本快照（V2.0.0_019 引入；阶段 3+）。
     * 来自 t_prompt_template_version.config_json，仅供回溯/调试，不影响运行时 stage 执行。
     * 老任务（prompt_template_version=null）保持 null。
     */
    private String configJsonSnapshot;

    /** 解析后的 inputParam（key=String, value=Object）。 */
    private Map<String, Object> input = new HashMap<>();

    // ===== AI 调用预算与追踪 =====
    /** 任务允许调 AI 的总次数（默认 3，worker 写入；可按会员等级调整）。 */
    private int aiCallBudget = 3;

    /** 已用 AI 调用次数（每次 AiGateway.call 自增，含 retry 内部尝试）。 */
    private int aiCallUsed = 0;

    /** AI 调用失败次数（AiGateway 内部 catch 写入，含 retry 内部尝试）。 */
    private int aiCallFailed = 0;

    /** 总重试次数（最后一次成功的 retry 次数累加）。 */
    private int aiCallRetried = 0;

    /** 所有 AI 调用的耗时累计（ms）。 */
    private long aiCallTotalMs = 0L;

    /** AI 调用历史（每条 step 自己 append）。 */
    private List<AiCallRecord> aiCallHistory = new ArrayList<>();

    // ===== 12 阶段产出（按 stageIndex 顺序填充）=====

    /** Stage 1：拼好的 user_context_block 文本。 */
    private String userContextBlock;

    /** Stage 2：结构骨架 JSON 字符串。 */
    private String outlineJson;

    /** Stage 3：素材清单 JSON 字符串。 */
    private String materialsJson;

    /** Stage 4：分块初稿 JSON 字符串。 */
    private String draftJson;

    /** Stage 5：韵律问题列表（detector 内部结构化结果）。 */
    private List<RhythmIssue> rhythmIssues = new ArrayList<>();

    /** Stage 6：韵律改写后初稿 JSON 字符串。 */
    private String draftAfterRhythmJson;

    /** Stage 7：毒舌点评列表。 */
    private List<ToxicComment> toxicComments = new ArrayList<>();

    /** Stage 8：定向改写后初稿 JSON 字符串。 */
    private String draftAfterTargetedJson;

    /** Stage 9：节奏打磨后最终初稿 JSON 字符串。 */
    private String finalDraftJson;

    /** Stage 10：字数统计结果。 */
    private WordStats wordStats;

    /** Stage 11：字数调整结果（可能是 keep 或 cut）。 */
    private WordAdjustResult wordAdjustResult;

    /** Stage 12：导出模板渲染结果。 */
    private ExportResult exportResult;

    // ===== 收尾（PersistArticleStep 写入）=====
    /** 最终落库的 article 业务号。 */
    private String articleBizNo;

    // ===== 工具方法 =====

    /** step 写新字段用。 */
    public void putExtra(String key, Object value) {
        input.put(key, value);
    }

    public Object getExtra(String key) {
        return input.get(key);
    }

    /**
     * 取出指定 stage 的 ai_prompt（已经过默认值兜底）。不存在返回 null。
     */
    public String stageAiPrompt(int stageIndex) {
        PromptTemplateStage s = stages.get(stageIndex);
        return s == null ? null : s.getAiPrompt();
    }

    /**
     * 取出指定 stage 的 rule_config（JSON 字符串）。不存在返回 null。
     */
    public String stageRuleConfig(int stageIndex) {
        PromptTemplateStage s = stages.get(stageIndex);
        return s == null ? null : s.getRuleConfig();
    }

    // ===== 嵌套 POJO（用于 5/7/10/11/12 阶段产出）=====

    /** Stage 5 韵律问题。 */
    @Getter
    @Setter
    public static class RhythmIssue {
        private String type;             // uniform_length / no_breath / monotonous_start
        private Integer paragraphIndex;
        private String text;             // 问题片段
        private String suggestion;
    }

    /** Stage 7 毒舌点评。 */
    @Getter
    @Setter
    public static class ToxicComment {
        private Integer paragraph;
        private Integer sentence;
        private String type;             // 太正确了 / 软弱无力 / 不敢站队 / ...
        private String original;
        private String toxicComment;
        private String severity;         // 高 / 中 / 低
    }

    /** Stage 10 字数统计。 */
    @Getter
    @Setter
    public static class WordStats {
        private int target;
        private int actual;
        private int diff;
        private String status;           // over / under / ok
    }

    /** Stage 11 字数调整。 */
    @Getter
    @Setter
    public static class WordAdjustResult {
        private String action;           // cut / keep
        private String reason;
        private int estimatedFinalCount;
    }

    /** Stage 12 导出结果。 */
    @Getter
    @Setter
    public static class ExportResult {
        private String format;           // html / markdown / plain
        private String platform;
        private String renderedDocument;
        private String sourceDraftJson;
    }
}
