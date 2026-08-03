package com.aichuangzuo.admin.modules.benefit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理端对用户额度用量表 {@code u_benefit_usage} 的读模型。
 */
@Getter
@Setter
@TableName("u_benefit_usage")
public class BenefitUsageAggregate {

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

    /** 预扣量。 */
    private Integer preUsedCount;

    /** 租户ID。 */
    private Long tenantId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
