package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GenerationCallLogVO {
    private Long id;
    private Long taskId;
    private Integer stageIndex;
    private String stageName;
    private Integer attempt;
    private Boolean success;
    private String error;
    private Integer durationMs;
    private LocalDateTime calledAt;
    private String userMsg;
    private String responseContent;
}
