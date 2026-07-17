package com.aichuangzuo.admin.modules.exporttemplate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_export_template")
public class ExportTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateKey;
    private String name;
    private String platform;
    private String description;
    private String bgColor;
    private String textColor;
    private String visualStyleJson;
    private String signatureText;
    private String signaturePosition;
    private Integer sortOrder;
    private Integer status;

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
