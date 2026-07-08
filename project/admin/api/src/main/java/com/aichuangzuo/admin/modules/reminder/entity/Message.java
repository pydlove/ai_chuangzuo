package com.aichuangzuo.admin.modules.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * admin-api 侧 u_message 实体（仅 insert）。
 * 不复用 user-api 的实体是为了避免 admin-api 引入 user-api 模块。
 * 注意：此实体的 @TableField(fill=...) 故意不设 createdAt/updatedAt 等，
 *       由调用方显式赋值，不依赖 SecurityContext 自动填充。
 */
@Getter
@Setter
@TableName("u_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号：单用户提醒固定 bizNo=RMD-uuid16。 */
    private String bizNo;

    /** membership / announcement / ... */
    private String msgType;

    /** 1-广播 2-个人。 */
    private Integer scope;

    private Long targetUserId;

    private String title;
    private String summary;
    private String content;
    private String subType;
    private String linkUrl;

    /** admin-api 单租户，固定 0。 */
    private Long tenantId;

    /** 0=未删除 1=已删除。 */
    private Integer isDeleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}