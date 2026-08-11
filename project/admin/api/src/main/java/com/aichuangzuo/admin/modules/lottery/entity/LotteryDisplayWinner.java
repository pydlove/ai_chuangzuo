package com.aichuangzuo.admin.modules.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_lottery_display_winner")
public class LotteryDisplayWinner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long campaignId;
    private Long tierId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String prizeName;
    private LocalDateTime winTime;
    private Integer isReal;
    private Integer sortOrder;
    private Integer status;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
