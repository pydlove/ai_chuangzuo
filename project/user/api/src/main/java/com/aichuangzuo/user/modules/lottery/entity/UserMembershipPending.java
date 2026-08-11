package com.aichuangzuo.user.modules.lottery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_user_membership_pending")
public class UserMembershipPending {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String planKey;
    private Integer days;
    private LocalDate plannedStartAt;
    private String status;
    private Long sourceCodeId;

    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
}
