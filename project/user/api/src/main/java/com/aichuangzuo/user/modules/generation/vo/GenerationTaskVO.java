package com.aichuangzuo.user.modules.generation.vo;

import com.aichuangzuo.shared.entity.GenerationTask;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/** 单个 task 的视图对象。 */
@Data
public class GenerationTaskVO {

    private Long id;
    private String bizNo;
    private Integer status;
    private String statusLabel;
    private String title;
    private String summary;

    /** 输入参数（解析后的对象）。 */
    private Map<String, Object> inputParam;

    private Integer wordLimitTarget;
    private Integer retryCount;
    private Integer maxRetry;
    private String failedReason;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    /** 归一化映射。 */
    public static GenerationTaskVO from(GenerationTask t, ObjectMapper objectMapper) {
        GenerationTaskVO vo = new GenerationTaskVO();
        vo.id = t.getId();
        vo.bizNo = t.getBizNo();
        vo.status = t.getStatus() == null ? null : t.getStatus().getCode();
        vo.statusLabel = t.getStatus() == null ? "" : t.getStatus().getLabel();
        // title 可从 input_param.title 兜底（任务入队时还没 AI 输出没正式 title）
        vo.title = parseTitle(t.getInputParam(), objectMapper);
        vo.inputParam = parseInput(t.getInputParam(), objectMapper);
        vo.wordLimitTarget = t.getWordLimitTarget();
        vo.retryCount = t.getRetryCount();
        vo.maxRetry = t.getMaxRetry();
        vo.failedReason = t.getFailedReason();
        vo.completedAt = t.getCompletedAt();
        vo.createdAt = t.getCreatedAt();
        return vo;
    }

    private static String parseTitle(String json, ObjectMapper om) {
        if (json == null || json.isBlank()) return "";
        try {
            Map<String, Object> m = om.readValue(json, new TypeReference<>() {});
            Object t = m.get("title");
            return t == null ? "" : t.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static Map<String, Object> parseInput(String json, ObjectMapper om) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return om.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
