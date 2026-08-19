package com.aichuangzuo.admin.modules.security.accesscontrol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_access_control")
public class AccessControl {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer ruleType;

    private Integer listType;

    private String ruleValue;

    private Integer ruleStatus;

    private String remark;

    private Integer isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}
