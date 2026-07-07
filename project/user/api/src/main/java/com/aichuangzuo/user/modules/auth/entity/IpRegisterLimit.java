package com.aichuangzuo.user.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_ip_register_limit")
public class IpRegisterLimit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String clientIp;
    private Integer registerCount;
    private Integer isBlocked;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
