package com.aichuangzuo.admin.modules.modelconfig.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_provider_model")
public class ProviderModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String providerType;
    private String modelCode;
    private String modelName;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
