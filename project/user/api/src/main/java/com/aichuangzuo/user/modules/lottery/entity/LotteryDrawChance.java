package com.aichuangzuo.user.modules.lottery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_lottery_draw_chance")
public class LotteryDrawChance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long campaignId;
    private Long userId;
    private String chanceType;
    private Long sourceInviteRelationId;
    private String status;

    private Long tenantId;

    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
}
