package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 体验会员令牌实体，对应表 {@code u_experience_token}。
 *
 * <p>管理端负责批量生成与管理；用户端注册时消费。</p>
 */
@Getter
@Setter
@TableName("u_experience_token")
public class ExperienceToken extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号，用于按运营批次管理。 */
    private String batchId;

    /** 体验令牌，唯一。 */
    private String token;

    /** 套餐类型：basic / pro / flagship。 */
    private String planKey;

    /** 赠送会员天数。 */
    private Integer membershipDays;

    /** 状态：0-未使用 1-已使用 2-过期。 */
    private Integer status;

    /** 使用人用户ID。 */
    private Long usedByUserId;

    /** 使用时间。 */
    private LocalDateTime usedAt;

    /** 令牌有效期。 */
    private LocalDateTime expiresAt;

    /** 租户ID。 */
    private Long tenantId;
}
