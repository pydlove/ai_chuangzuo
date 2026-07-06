package com.aichuangzuo.admin.modules.modelconfig.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_model_config")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String providerType;
    private String baseUrl;
    private String apiKeyEncrypted;
    private String modelCode;
    private String modelName;
    private Integer isActive;

    @TableLogic
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
