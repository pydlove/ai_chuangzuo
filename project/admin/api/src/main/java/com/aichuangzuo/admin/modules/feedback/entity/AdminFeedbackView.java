package com.aichuangzuo.admin.modules.feedback.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminFeedbackView {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userBizNo;
    private String type;
    private String content;
    private String contact;
    private String replyContent;
    private Long replyAdminId;
    private String replyAdminName;
    private LocalDateTime repliedAt;
    private Integer status;
    private LocalDateTime createdAt;
}
