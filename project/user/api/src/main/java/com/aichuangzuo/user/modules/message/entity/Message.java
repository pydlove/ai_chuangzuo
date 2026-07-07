package com.aichuangzuo.user.modules.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息内容实体，对应表 u_message。
 */
@Getter
@Setter
@TableName("u_message")
public class Message {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息类型：announcement / feature / promotion / generation / membership。 */
    private String msgType;

    /** 范围：1-广播（全体），2-个人。 */
    private Integer scope;

    /** 目标用户ID：个人消息填写，广播为 null。 */
    private Long targetUserId;

    /** 标题。 */
    private String title;

    /** 摘要。 */
    private String summary;

    /** 点击跳转路由，空则前端按类型默认跳转。 */
    private String linkUrl;

    /** 租户ID。 */
    private Long tenantId;

    /** 逻辑删除标记。 */
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
