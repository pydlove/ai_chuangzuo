package com.aichuangzuo.user.modules.benefit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 权益定义实体，对应表 u_benefit。
 */
@Getter
@Setter
@TableName("u_benefit")
public class Benefit {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权益编码，如 ai_article_quota。 */
    private String code;

    /** 权益名称。 */
    private String name;

    /** 类型：boolean/quota/tier。 */
    private String type;

    /** 权益描述。 */
    private String description;

    /** 对比表行标签 / 默认名称。 */
    private String displayLabel;

    /** 价格卡值模板，含 {value} 占位；为空时仅显示 value。 */
    private String cardValueTpl;

    /** value→显示文本 JSON 覆盖（tier 类型 / 特殊值如 -1→永久）。 */
    private String valueLabelJson;

    /** 排序号。 */
    private Integer sortOrder;

    /** 状态：0-停用，1-启用。 */
    private Integer status;

    /** 租户ID。 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
