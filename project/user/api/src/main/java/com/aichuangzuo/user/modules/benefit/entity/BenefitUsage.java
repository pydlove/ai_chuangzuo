package com.aichuangzuo.user.modules.benefit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 权益用量实体，对应表 u_benefit_usage。
 */
@Getter
@Setter
@TableName("u_benefit_usage")
public class BenefitUsage {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID。 */
    private Long userId;

    /** 权益编码。 */
    private String benefitCode;

    /** 周期标识，月度格式 yyyy-MM。 */
    private String period;

    /** 已用量。 */
    private Integer usedCount;

    /** 租户ID。 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
