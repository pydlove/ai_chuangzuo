package com.aichuangzuo.admin.modules.feedback.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminFeedbackVO {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userBizNo;
    private String type;
    private String content;
    private String replyContent;
    private LocalDateTime repliedAt;
    private Integer status;
    private LocalDateTime createdAt;
}
