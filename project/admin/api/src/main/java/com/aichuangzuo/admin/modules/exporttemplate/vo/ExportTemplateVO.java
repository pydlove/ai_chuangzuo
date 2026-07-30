package com.aichuangzuo.admin.modules.exporttemplate.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExportTemplateVO {

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

    /** 所需套餐：basic / pro / flagship。 */
    private String tier;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
