package com.aichuangzuo.admin.modules.aiprompt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiPromptVO {

    private Long id;
    private String promptCode;
    private String promptName;
    private String module;
    private String category;
    private Integer status;
    private LocalDateTime updatedAt;
}
