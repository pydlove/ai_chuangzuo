package com.aichuangzuo.user.modules.exporttemplate.vo;

import lombok.Data;

/**
 * 用户端导出模板 VO：visualStyle 直接返回解析后的 JSON 对象。
 */
@Data
public class ExportTemplateVO {

    private String templateKey;
    private String name;
    private String platform;
    private String description;
    private String bgColor;
    private String textColor;
    private Object visualStyle;
    private String signatureText;
    private String signaturePosition;
    private Integer sortOrder;
}
