package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.GenerationTaskListRow;
import com.aichuangzuo.admin.modules.generation.dto.TaskTokenSum;
import com.aichuangzuo.admin.modules.generation.dto.request.GenerationTaskQueryRequest;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.admin.modules.generation.vo.GenerationTaskAdminPageVO;
import com.aichuangzuo.admin.modules.generation.vo.GenerationTaskAdminVO;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin 端-创作任务查询 / 运维服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationTaskAdminService {

    private final GenerationTaskMapper taskMapper;
    private final GenerationCallLogMapper callLogMapper;

    public GenerationTaskAdminPageVO list(GenerationTaskQueryRequest req) {
        long page = Math.max(1, req.getPage());
        long pageSize = Math.min(Math.max(1, req.getPageSize()), 100);
        long offset = (page - 1) * pageSize;
        String keyword = req.getKeyword() == null ? null : req.getKeyword().trim();
        Integer status = req.getStatus();

        List<GenerationTaskListRow> rows = taskMapper.selectAdminList(status,
                (keyword == null || keyword.isEmpty()) ? null : keyword,
                offset, (int) pageSize);
        long total = taskMapper.countAdminList(status,
                (keyword == null || keyword.isEmpty()) ? null : keyword);

        // 批量聚合当前页所有任务的累计 token（一次 SQL，避免 N+1）
        Map<Long, Long> tokenMap = new HashMap<>();
        if (!rows.isEmpty()) {
            List<Long> taskIds = rows.stream().map(GenerationTaskListRow::getId).toList();
            for (TaskTokenSum s : callLogMapper.sumTokensByTaskIds(taskIds)) {
                tokenMap.put(s.getTaskId(), s.getTotalTokens());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        GenerationTaskAdminPageVO vo = new GenerationTaskAdminPageVO();
        vo.setList(rows.stream().map(r -> toVo(r, now, tokenMap.get(r.getId()))).toList());
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        return vo;
    }

    /**
     * 手动停止任务：QUEUED / PROCESSING → FAILED，写失败原因「管理员手动停止」，清 lease，置 completedAt。
     * 与「标记失败」区别：本接口不接受自定义原因，是一键停止；后者允许填具体 reason。
     */
    @Transactional
    public void stopTask(Long taskId) {
        GenerationTask task = requireById(taskId);
        if (task.getStatus() != GenerationTaskStatus.QUEUED
                && task.getStatus() != GenerationTaskStatus.PROCESSING) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_INVALID_STATUS);
        }
        task.setStatus(GenerationTaskStatus.FAILED);
        task.setFailedReason("管理员手动停止");
        task.setCompletedAt(LocalDateTime.now());
        task.setLockedAt(null);
        task.setLockedBy(null);
        task.setLeaseUntil(null);
        taskMapper.updateById(task);
        log.info("admin 手动停止任务 task={}", taskId);
    }

    /**
     * 手动标记失败：直接置 FAILED（不再回 queued），便于踢出积压的坏任务。
     */
    @Transactional
    public void manualMarkFailed(Long taskId, String reason) {
        GenerationTask task = requireById(taskId);
        task.setStatus(GenerationTaskStatus.FAILED);
        task.setFailedReason(reason == null ? "admin 手动标记失败" : reason);
        task.setCompletedAt(LocalDateTime.now());
        if (task.getLockedBy() != null) {
            task.setLockedAt(null);
            task.setLockedBy(null);
            task.setLeaseUntil(null);
        }
        taskMapper.updateById(task);
        log.info("admin 手动标记失败 task={} reason={}", taskId, task.getFailedReason());
    }

    private GenerationTask requireById(Long id) {
        GenerationTask t = taskMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_NOT_FOUND);
        }
        return t;
    }

    private GenerationTaskAdminVO toVo(GenerationTaskListRow r, LocalDateTime now, Long totalTokens) {
        GenerationTaskAdminVO vo = new GenerationTaskAdminVO();
        BeanUtils.copyProperties(r, vo);
        vo.setStatus(r.getStatus());
        vo.setStatusLabel(statusLabel(r.getStatus()));
        // waitingSeconds: queued / processing 都算「从 created_at 起等了多久」
        if (r.getCreatedAt() != null) {
            vo.setWaitingSeconds(Math.max(0, Duration.between(r.getCreatedAt(), now).getSeconds()));
        }
        if (r.getStatus() != null && r.getStatus() == 3 && r.getCompletedAt() != null) {
            vo.setFailedSecondsAgo(Math.max(0, Duration.between(r.getCompletedAt(), now).getSeconds()));
        }
        vo.setTotalTokens(totalTokens == null ? 0L : totalTokens);
        return vo;
    }

    private static String statusLabel(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 0 -> "queued";
            case 1 -> "processing";
            case 2 -> "completed";
            case 3 -> "failed";
            default -> "-";
        };
    }
}
