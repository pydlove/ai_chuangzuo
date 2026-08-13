package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.infrastructure.ai.AiProvider;
import com.aichuangzuo.admin.modules.generation.dto.GenerationTaskListRow;
import com.aichuangzuo.admin.modules.generation.dto.TaskTokenSum;
import com.aichuangzuo.admin.modules.generation.dto.request.GenerationTaskQueryRequest;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.admin.modules.generation.vo.BatchStopGenerationTaskResultVO;
import com.aichuangzuo.admin.modules.generation.vo.GenerationTaskAdminPageVO;
import com.aichuangzuo.admin.modules.generation.vo.GenerationTaskAdminVO;
import com.aichuangzuo.admin.modules.generation.vo.GeneratedArticleVO;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final QuotaRefundInternalClient refundClient;
    private final ArticleReadInternalClient articleReadClient;
    private final ObjectMapper objectMapper;

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
        log.info("管理端查询创作任务列表完成, status={}, keyword={}, page={}, pageSize={}, total={}",
                status, keyword, page, pageSize, total);
        return vo;
    }

    /**
     * 手动停止任务：QUEUED / PROCESSING → FAILED，写失败原因「管理员手动停止」，清 lease，置 completedAt，
     * 并退回该任务预扣的文章额度（与异常失败同待遇）。
     *
     * <p>PROCESSING 任务由 worker 在下一 stage 前协作式中止（stopTask 已置 FAILED + 清 lockedBy）；
     * 退额度失败不影响停止本身，仅记错误日志待人工介入。
     */
    @Transactional
    public void stopTask(Long taskId) {
        GenerationTask task = requireById(taskId);
        if (task.getStatus() != GenerationTaskStatus.QUEUED
                && task.getStatus() != GenerationTaskStatus.PROCESSING) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_INVALID_STATUS);
        }
        doStopTask(task);
        log.info("admin 手动停止任务 task={}", taskId);
    }

    /**
     * 批量停止任务：仅处理 status=QUEUED/PROCESSING 的任务；不存在或状态不允许的任务返回在结果中，
     * 不影响其他任务。每个任务的停止独立提交，单任务失败不会回滚已成功停止的任务。
     *
     * @param ids 任务 ID 列表，单次最多 100 个
     */
    public BatchStopGenerationTaskResultVO batchStop(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_INVALID_STATUS);
        }
        if (ids.size() > 100) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_INVALID_STATUS);
        }

        BatchStopGenerationTaskResultVO result = new BatchStopGenerationTaskResultVO();
        result.setTotal(ids.size());
        result.setSuccessCount(0);
        result.setMissingIds(new ArrayList<>());
        result.setInvalidIds(new ArrayList<>());
        result.setFailedIds(new ArrayList<>());

        for (Long id : ids) {
            GenerationTask task = taskMapper.selectById(id);
            if (task == null) {
                result.getMissingIds().add(id);
                continue;
            }
            if (task.getStatus() != GenerationTaskStatus.QUEUED
                    && task.getStatus() != GenerationTaskStatus.PROCESSING) {
                result.getInvalidIds().add(id);
                continue;
            }
            try {
                doStopTask(task);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.getFailedIds().add(id);
                log.error("批量停止任务失败 task={}: {}", id, e.getMessage());
            }
        }
        log.info("admin 批量停止任务完成, total={}, success={}, missing={}, invalid={}, failed={}",
                result.getTotal(), result.getSuccessCount(), result.getMissingIds().size(),
                result.getInvalidIds().size(), result.getFailedIds().size());
        return result;
    }

    /**
     * 停止任务核心逻辑（无事务注解，由调用方控制事务边界）。
     */
    private void doStopTask(GenerationTask task) {
        task.setStatus(GenerationTaskStatus.FAILED);
        task.setFailedReason("管理员手动停止");
        task.setCompletedAt(LocalDateTime.now());
        task.setLockedAt(null);
        task.setLockedBy(null);
        task.setLeaseUntil(null);
        taskMapper.updateById(task);

        try {
            refundClient.refund(task.getId(), task.getTargetUserId());
        } catch (Exception e) {
            log.error("task={} 手动停止后退文章额度失败，需人工介入: {}", task.getId(), e.getMessage());
        }
    }

    /**
     * 在线预览已完成任务的最终文章。
     *
     * @return 文章内容（标题、正文、描述、标签等）
     */
    public GeneratedArticleVO previewArticle(Long taskId) {
        GenerationTask task = requireCompletedTaskWithArticle(taskId);
        return articleReadClient.getArticle(task.getArticleBizNo());
    }

    /**
     * 下载已完成任务的最终文章（markdown 格式）。
     *
     * @return 文件名 + 文件字节数组
     */
    public ArticleDownload downloadArticle(Long taskId) {
        GenerationTask task = requireCompletedTaskWithArticle(taskId);
        GeneratedArticleVO article = articleReadClient.getArticle(task.getArticleBizNo());

        StringBuilder sb = new StringBuilder();
        if (article.getTitle() != null && !article.getTitle().isBlank()) {
            sb.append("# ").append(article.getTitle()).append("\n\n");
        }
        if (article.getDescription() != null && !article.getDescription().isBlank()) {
            sb.append("> ").append(article.getDescription().replace("\n", "\n> ")).append("\n\n");
        }
        if (article.getBody() != null) {
            sb.append(article.getBody());
        }
        String filename = (task.getBizNo() == null ? "article" : task.getBizNo()) + ".md";
        return new ArticleDownload(filename, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private GenerationTask requireCompletedTaskWithArticle(Long taskId) {
        GenerationTask task = requireById(taskId);
        if (task.getStatus() != GenerationTaskStatus.COMPLETED) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_INVALID_STATUS);
        }
        if (task.getArticleBizNo() == null || task.getArticleBizNo().isBlank()) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_ARTICLE_NOT_FOUND);
        }
        return task;
    }

    private GenerationTask requireById(Long id) {
        GenerationTask t = taskMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_NOT_FOUND);
        }
        return t;
    }

    GenerationTaskAdminVO toVo(GenerationTaskListRow r, LocalDateTime now, Long totalTokens) {
        GenerationTaskAdminVO vo = new GenerationTaskAdminVO();
        BeanUtils.copyProperties(r, vo);
        vo.setStatus(r.getStatus());
        vo.setStatusLabel(statusLabel(r.getStatus()));
        vo.setModelConfigDisplay(buildModelConfigDisplay(r.getModelConfigName(), r.getProviderType()));

        Map<String, Object> input = parseInputParam(r.getInputParam());
        vo.setTitle(nullToEmpty(input.get("title")));
        vo.setDescription(nullToEmpty(input.get("description")));
        vo.setPlatform(nullToEmpty(input.get("platform")));
        vo.setSkillRef(nullToEmpty(input.get("skillRef")));
        vo.setTemplate(nullToEmpty(input.get("template")));
        vo.setUserSkillPrompt(nullToEmpty(input.get("userSkillPrompt")));

        // waitingSeconds: queued / processing 算「从 created_at 起到现在等了多久」；
        // completed / failed 任务已结束，按 completed_at - created_at 算实际耗时，避免已完成任务耗时继续增长。
        if (r.getCreatedAt() != null) {
            LocalDateTime end = r.getCompletedAt() != null ? r.getCompletedAt() : now;
            vo.setWaitingSeconds(Math.max(0, Duration.between(r.getCreatedAt(), end).getSeconds()));
        }
        if (r.getStatus() != null && r.getStatus() == 3 && r.getCompletedAt() != null) {
            vo.setFailedSecondsAgo(Math.max(0, Duration.between(r.getCompletedAt(), now).getSeconds()));
        }
        vo.setTotalTokens(totalTokens == null ? 0L : totalTokens);
        return vo;
    }

    private Map<String, Object> parseInputParam(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            return parsed == null ? Map.of() : parsed;
        } catch (Exception e) {
            log.warn("解析 task input_param 失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
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

    private static String buildModelConfigDisplay(String configName, String providerType) {
        String providerName = AiProvider.fromCode(providerType)
                .map(AiProvider::getName)
                .orElse(providerType != null ? providerType : "-");
        if (configName == null || configName.isBlank()) {
            return providerName;
        }
        return configName + "/" + providerName;
    }
}
