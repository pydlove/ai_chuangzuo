package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.entity.GenerationCallLog;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.admin.modules.generation.pipeline.AiCallRecord;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.vo.GenerationCallLogGroupedVO;
import com.aichuangzuo.admin.modules.generation.vo.GenerationCallLogVO;
import com.aichuangzuo.shared.entity.GenerationTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 创作 AI 调用日志服务。
 *
 * <p>每次 pipeline 阶段完成后及 pipeline 结束时由 worker 调 {@link #persistAll(GenerationContext)}
 * 把 ctx 里当前累积的 AI 调用记录全量替换落库（先删后插，幂等）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationCallLogService {

    private final GenerationCallLogMapper mapper;
    private final GenerationTaskMapper taskMapper;

    /**
     * 把 ctx.aiCallHistory 全量同步到库。taskId 必传（worker 兜底设过）。
     * 采用先删除该任务旧记录、再批量插入当前全部记录的方式，保证同一任务多次调用幂等。
     *
     * @return 插入条数
     */
    @Transactional
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
            row.setPromptTokens(rec.getPromptTokens());
            row.setCompletionTokens(rec.getCompletionTokens());
            row.setTotalTokens(rec.getTotalTokens());
            row.setCalledAt(rec.getCalledAt());
            row.setUserMsg(rec.getUserMsg());
            row.setResponseContent(rec.getResponseContent());
            row.setTenantId(0L);
            rows.add(row);
        }
        int deleted = mapper.deleteByTaskId(taskId);
        int inserted = mapper.batchInsert(rows);
        log.info("call log 落库 task={} deleted={} rows={} aiUsed={} aiFailed={} retried={}",
                taskId, deleted, inserted, ctx.getAiCallUsed(), ctx.getAiCallFailed(), ctx.getAiCallRetried());
        return inserted;
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
     * 顺带返回任务当前 status，前端用于判断是否继续轮询。
     */
    public GenerationCallLogGroupedVO queryByTaskIdGrouped(Long taskId) {
        List<GenerationCallLogVO> all = queryByTaskId(taskId);
        Map<Integer, List<GenerationCallLogVO>> grouped = new java.util.TreeMap<>();
        for (GenerationCallLogVO v : all) {
            grouped.computeIfAbsent(v.getStageIndex(), k -> new ArrayList<>()).add(v);
        }
        GenerationCallLogGroupedVO vo = new GenerationCallLogGroupedVO();
        vo.setGrouped(grouped);
        GenerationTask task = taskMapper.selectById(taskId);
        vo.setTaskStatus(task == null || task.getStatus() == null ? null : task.getStatus().getCode());
        return vo;
    }

    private static String truncate(String s, int n) {
        if (s == null) return null;
        return s.length() > n ? s.substring(0, n) : s;
    }
}
