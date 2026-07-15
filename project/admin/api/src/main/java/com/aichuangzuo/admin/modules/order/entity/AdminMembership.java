package com.aichuangzuo.admin.modules.order.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户会员状态实体，对应 u_user_membership。
 */
@Data
public class AdminMembership {
    private Long id;
    private Long userId;
    private String level;
    private LocalDate startedAt;
    private LocalDate expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
