package com.aichuangzuo.user.modules.feedback.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackVO {
    private Long id;
    private String type;
    private String content;
    private String contact;
    private String replyContent;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private Integer status;
}
