package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.entity.GenerationCallLog;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import com.aichuangzuo.admin.modules.generation.pipeline.AiCallRecord;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.vo.GenerationCallLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 创作 AI 调用日志服务。
 *
 * <p>每次 pipeline 跑完后（成功 or 失败）由 worker 调 {@link #persistAll(GenerationContext)}
 * 把 ctx 里所有 AI 调用记录批量落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationCallLogService {

    private static final int PREVIEW_LEN = 200;

    private final GenerationCallLogMapper mapper;

    /**
     * 把 ctx.aiCallHistory 全量落库。taskId 必传（worker 兜底设过）。
     * @return 插入条数
     */
    public int persistAll(GenerationContext ctx) {
        if (ctx == null || ctx.getTask() == null) return 0;
        if (ctx.getAiCallHistory() == null || ctx.getAiCallHistory().isEmpty()) return 0;

        Long taskId = ctx.getTask().getId();
        List<GenerationCallLog> rows = new ArrayList<>();
        for (AiCallRecord rec : ctx.getAiCallHistory()) {
            GenerationCallLog row = new GenerationCallLog();
            row.setTaskId(taskId);
            row.setStageIndex(rec.getStageIndex());
            row.setStageName(rec.getStepName() == null ? "?" : rec.getStepName());
            row.setAttempt(rec.getAttempt() <= 0 ? 1 : rec.getAttempt());
            row.setSuccess(rec.isSuccess() ? 1 : 0);
            row.setError(truncate(rec.getError(), 500));
            row.setDurationMs((int) Math.min(rec.getDurationMs(), Integer.MAX_VALUE));
            row.setCalledAt(rec.getCalledAt());
            row.setUserMsgPreview(extractUserMsgPreview(ctx, rec));
            row.setResponsePreview(rec.isSuccess()
                    ? truncate(rec.getStepName() + ":" + extractResponse(ctx, rec), PREVIEW_LEN)
                    : null);
            row.setTenantId(0L);
            rows.add(row);
        }
        int n = mapper.batchInsert(rows);
        log.info("call log 落库 task={} rows={} aiUsed={} aiFailed={} retried={}",
                taskId, n, ctx.getAiCallUsed(), ctx.getAiCallFailed(), ctx.getAiCallRetried());
        return n;
    }

    /**
     * 查某任务的所有调用记录（按 stage 升序）。
     */
    public List<GenerationCallLogVO> queryByTaskId(Long taskId) {
        List<GenerationCallLog> rows = mapper.selectByTaskId(taskId);
        List<GenerationCallLogVO> vos = new ArrayList<>();
        for (GenerationCallLog r : rows) {
            GenerationCallLogVO vo = new GenerationCallLogVO();
            BeanUtils.copyProperties(r, vo);
            vo.setSuccess(r.getSuccess() != null && r.getSuccess() == 1);
            vos.add(vo);
        }
        return vos;
    }

    /**
     * 查某任务所有记录，按 stage 分组（key=stageIndex，value=该 stage 的所有 attempt 记录）。
     * 方便前端展示「stage 4 调了 2 次：第 1 次失败 timeout，第 2 次成功」。
     */
    public Map<Integer, List<GenerationCallLogVO>> queryByTaskIdGrouped(Long taskId) {
        List<GenerationCallLogVO> all = queryByTaskId(taskId);
        Map<Integer, List<GenerationCallLogVO>> grouped = new java.util.TreeMap<>();
        for (GenerationCallLogVO v : all) {
            grouped.computeIfAbsent(v.getStageIndex(), k -> new ArrayList<>()).add(v);
        }
        return grouped;
    }

    private static String truncate(String s, int n) {
        if (s == null) return null;
        return s.length() > n ? s.substring(0, n) : s;
    }

    /**
     * 从 ctx 的「最后一次 user msg」截取对应 stage 的前 200 字作为 preview。
     * 简化：直接用 ctx 里 stage 的 last user prompt（如果有）。
     */
    private String extractUserMsgPreview(GenerationContext ctx, AiCallRecord rec) {
        // ctx 里没有保留每次的 user msg（只有 aiPrompt 模板），preview 只能从模板名 + stage 信息凑
        String stageKey = rec.getStepName() == null ? "?" : rec.getStepName();
        String aiPrompt = ctx.stageAiPrompt(rec.getStageIndex());
        if (aiPrompt == null || aiPrompt.isBlank()) {
            return "[stage " + rec.getStageIndex() + " " + stageKey + "]";
        }
        return truncate("[stage " + rec.getStageIndex() + " " + stageKey + "] " + aiPrompt, PREVIEW_LEN);
    }

    private String extractResponse(GenerationContext ctx, AiCallRecord rec) {
        // ctx 没有存每次的 response；返回 stage 的「最新产出」作为参考
        int idx = rec.getStageIndex();
        switch (idx) {
            case 1: return ctx.getUserContextBlock();
            case 2: return ctx.getOutlineJson();
            case 3: return ctx.getMaterialsJson();
            case 4: return ctx.getDraftJson();
            case 6: return ctx.getDraftAfterRhythmJson();
            case 8: return ctx.getDraftAfterTargetedJson();
            case 9: return ctx.getFinalDraftJson();
            case 11: return ctx.getWordAdjustResult() == null ? null
                    : (ctx.getWordAdjustResult().getAction() + " " + ctx.getWordAdjustResult().getReason());
            case 7: {
                StringBuilder sb = new StringBuilder("[");
                if (ctx.getToxicComments() != null) {
                    for (int i = 0; i < ctx.getToxicComments().size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(ctx.getToxicComments().get(i).getToxicComment());
                    }
                }
                return sb.append("]").toString();
            }
            case 5: {
                StringBuilder sb = new StringBuilder("[");
                if (ctx.getRhythmIssues() != null) {
                    for (int i = 0; i < ctx.getRhythmIssues().size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(ctx.getRhythmIssues().get(i).getSuggestion());
                    }
                }
                return sb.append("]").toString();
            }
            case 10: return ctx.getWordStats() == null ? null
                    : (ctx.getWordStats().getActual() + "/" + ctx.getWordStats().getTarget());
            case 12: return ctx.getExportResult() == null ? null
                    : ctx.getExportResult().getRenderedDocument();
            default: return null;
        }
    }
}
