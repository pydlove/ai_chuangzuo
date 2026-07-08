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
 * 用户已生成作品，对应表 u_article。
 */
@Getter
@Setter
@TableName("u_article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizNo;

    private Long userId;

    private String title;

    private String body;

    /**
     * 编辑器内联样式覆盖，JSON 字符串。
     * 形如：{"blocks":{},"inlines":[{"block":0,"start":1,"end":5,"styles":{...}}]}。
     */
    private String styleOverrides;

    private String platform;

    private String style;

    private String template;

    private Integer wordCount;

    private LocalDateTime completedAt;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}