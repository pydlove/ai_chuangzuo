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

    /** 所需套餐：basic / pro / flagship。 */
    private String tier;

    /**
     * 当前用户是否可访问；根据登录用户所在套餐的 template_access 权益动态计算。
     * 未登录 / 套餐内无可访问模板时为 false。
     */
    private Boolean accessible;
}
