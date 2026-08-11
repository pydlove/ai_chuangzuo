package com.aichuangzuo.admin.modules.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_lottery_draw_record")
public class LotteryDrawRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizNo;
    private Long campaignId;
    private Long userId;
    private Long tierId;
    private Long codeId;
    private String drawType;
    private Long inviteRelationId;
    private Long tenantId;
    private LocalDateTime createdAt;
}
