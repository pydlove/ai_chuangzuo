package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

/**
 * AI 提示词配置实体，对应表 {@code c_ai_prompt}。
 *
 * <p>管理端维护，用户端/管理端运行时读取。</p>
 */
@Getter
@Setter
@TableName("c_ai_prompt")
public class AiPrompt extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String promptCode;
    private String promptName;
    private String module;
    private String category;
    private String systemRole;
    private String userPrompt;
    private String variableSchema;
    private Integer status;
    private Integer sortOrder;
    private String description;
    private Long tenantId;
}
