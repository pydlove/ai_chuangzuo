package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.mapper.GenerationHistoryMapper;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.shared.entity.GenerationHistory;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创作任务归档服务：将过期 completed/failed 任务迁移到历史表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationRetentionService {

    private final GenerationTaskMapper taskMapper;
    private final GenerationHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;

    /**
     * 单批归档：查询任务 → 写入 history → 删除原任务。
     * 事务边界控制在单批，失败只影响本批，不阻塞其它批次。
     */
    @Transactional(rollbackFor = Exception.class)
    public int archiveBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<GenerationTask> tasks = taskMapper.selectBatchIds(ids);
        if (tasks == null || tasks.isEmpty()) return 0;

        LocalDateTime now = LocalDateTime.now();
        for (GenerationTask task : tasks) {
            GenerationHistory history = toHistory(task, now);
            historyMapper.insert(history);
        }
        taskMapper.deleteBatchIds(ids);
        return tasks.size();
    }

    private GenerationHistory toHistory(GenerationTask task, LocalDateTime archivedAt) {
        GenerationHistory h = new GenerationHistory();
        h.setTaskId(task.getId());
        h.setBizNo(task.getBizNo());
        h.setTargetUserId(task.getTargetUserId());
        h.setTitle(extractTitle(task.getInputParam()));
        h.setInputParam(task.getInputParam());
        h.setStatus(task.getStatus() == null ? null : task.getStatus().getCode());
        h.setRetryCount(task.getRetryCount());
        h.setFailedReason(task.getFailedReason());
        h.setWordLimitTarget(task.getWordLimitTarget());
        h.setModelConfigId(task.getModelConfigId());
        h.setPromptTemplateId(task.getPromptTemplateId());
        h.setCreatedAt(task.getCreatedAt());
        h.setCompletedAt(task.getCompletedAt());
        h.setDurationMs(calcDurationMs(task));
        h.setArchivedAt(archivedAt);
        return h;
    }

    private String extractTitle(String inputParam) {
        if (inputParam == null || inputParam.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(inputParam);
            JsonNode title = node.get("title");
            return title == null || title.isNull() ? null : title.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private Long calcDurationMs(GenerationTask task) {
        if (task.getCreatedAt() == null || task.getCompletedAt() == null) return null;
        return java.time.Duration.between(task.getCreatedAt(), task.getCompletedAt()).toMillis();
    }
}
