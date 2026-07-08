package com.aichuangzuo.user.modules.article.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户创作草稿，对应表 u_draft。
 */
@Getter
@Setter
@TableName("u_draft")
public class Draft {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizNo;

    private Long userId;

    private String customTitle;

    private String customRequirement;

    private String platform;

    private Integer wordCount;

    private String style;

    private String template;

    private LocalDateTime savedAt;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}