package com.aichuangzuo.admin.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryDrawRecordAdminVO {

    private Long id;
    private String bizNo;
    private Long campaignId;
    private String campaignName;
    private Long userId;
    private String nickname;
    private String email;
    private Long tierId;
    private String tierName;
    private Long codeId;
    private String code;
    private String drawType;
    private Long inviteRelationId;
    private LocalDateTime createdAt;
}
