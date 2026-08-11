package com.aichuangzuo.admin.modules.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_lottery_campaign")
public class LotteryCampaign {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String imageUrl;
    private String rules;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Integer freeDrawsPerUser;
    private Long tenantId;
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
