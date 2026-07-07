package com.aichuangzuo.user.modules.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户消息已读记录实体，对应表 u_message_read。
 */
@Getter
@Setter
@TableName("u_message_read")
public class MessageRead {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID。 */
    private Long userId;

    /** 消息ID。 */
    private Long messageId;

    /** 已读时间。 */
    private LocalDateTime readAt;

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
