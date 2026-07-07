package com.aichuangzuo.admin.modules.earnings.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_leaderboard_reward_record")
public class RewardRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizNo;
    private Integer leaderboardType;
    private String periodMonth;
    private Integer rankNo;
    private Long userId;
    private BigDecimal amount;
    private String coinRecordBizNo;
    private Long grantedBy;
    private LocalDateTime grantedAt;
    private Long tenantId;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
