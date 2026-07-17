package com.aichuangzuo.user.modules.exporttemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

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
}
