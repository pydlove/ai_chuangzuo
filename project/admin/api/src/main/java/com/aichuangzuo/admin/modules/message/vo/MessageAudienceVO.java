package com.aichuangzuo.admin.modules.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端-消息管理-详情-受众用户 VO。
 */
@Data
public class MessageAudienceVO {

    private Long userId;
    private String nickname;
    private String email;
    private Long messageId;
    private LocalDateTime bizTime;
    private Boolean read;
    private LocalDateTime readAt;
}
