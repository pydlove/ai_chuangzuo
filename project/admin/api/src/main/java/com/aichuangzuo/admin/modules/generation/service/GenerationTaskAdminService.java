package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.GenerationTaskListRow;
import com.aichuangzuo.admin.modules.generation.dto.request.GenerationTaskQueryRequest;
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
import java.util.List;

/**
 * Admin 端-创作任务查询 / 运维服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationTaskAdminService {

    private final GenerationTaskMapper taskMapper;

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

        LocalDateTime now = LocalDateTime.now();
        GenerationTaskAdminPageVO vo = new GenerationTaskAdminPageVO();
        vo.setList(rows.stream().map(r -> toVo(r, now)).toList());
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        return vo;
    }

    /**
     * 手动重试：把任务状态回滚为 QUEUED，清空 lease 和失败原因；retry_count 不重置以便持续累计。
     */
    @Transactional
    public void manualRetry(Long taskId) {
        GenerationTask task = requireById(taskId);
        if (task.getStatus() == GenerationTaskStatus.COMPLETED) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_INVALID_STATUS);
        }
        task.setStatus(GenerationTaskStatus.QUEUED);
        task.setLockedAt(null);
        task.setLockedBy(null);
        task.setLeaseUntil(null);
        task.setFailedReason(null);
        taskMapper.updateById(task);
        log.info("admin 手动重试 task={}", taskId);
    }

    /**
     * 强制释放 lease：processing → queued，清空 lease，retry_count +1。
     * 用于卡死任务恢复。
     */
    @Transactional
    public void releaseLease(Long taskId) {
        GenerationTask task = requireById(taskId);
        if (task.getStatus() != GenerationTaskStatus.PROCESSING) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_INVALID_STATUS);
        }
        task.setStatus(GenerationTaskStatus.QUEUED);
        task.setLockedAt(null);
        task.setLockedBy(null);
        task.setLeaseUntil(null);
        task.setRetryCount((task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1);
        taskMapper.updateById(task);
        log.info("admin 强制释放 lease task={}", taskId);
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

    private GenerationTaskAdminVO toVo(GenerationTaskListRow r, LocalDateTime now) {
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
