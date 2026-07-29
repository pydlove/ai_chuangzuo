package com.aichuangzuo.admin.modules.topictitle.vo;

import com.aichuangzuo.admin.modules.topictitle.entity.TopicTitleTask;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标题生成异步任务状态 VO，供前端轮询。
 */
@Data
public class TopicTitleTaskVO {

    private Long id;
    private Integer status;
    private String statusLabel;
    private Integer count;
    private Integer generatedCount;
    private String failedReason;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public static TopicTitleTaskVO from(TopicTitleTask t) {
        TopicTitleTaskVO vo = new TopicTitleTaskVO();
        vo.setId(t.getId());
        vo.setStatus(t.getStatus());
        vo.setStatusLabel(statusLabel(t.getStatus()));
        vo.setCount(t.getCount());
        vo.setGeneratedCount(t.getGeneratedCount());
        vo.setFailedReason(t.getFailedReason());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setStartedAt(t.getStartedAt());
        vo.setCompletedAt(t.getCompletedAt());
        return vo;
    }

    private static String statusLabel(Integer s) {
        if (s == null) return "-";
        return switch (s) {
            case 0 -> "queued";
            case 1 -> "processing";
            case 2 -> "completed";
            case 3 -> "failed";
            default -> "-";
        };
    }
}