package com.aichuangzuo.admin.modules.message.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 管理端对用户表 {@code u_message} 的读模型。
 *
 * <p>沿用 admin 端既有风格：与 leaderboard / earnings 等模块的 aggregate 实体同款做法，
 * 字段命名严格沿用迁移脚本 V1.0.0_016 与 V2.0.0_004。
 */
@Getter
@Setter
@TableName("u_message")
public class MessageAggregate extends BaseEntity {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号：广播单行独立；指定人一次发布的多行共享同一个 biz_no。 */
    private String bizNo;

    /** 消息类型：announcement-公告 / feature-新功能 / promotion-优惠活动。 */
    private String msgType;

    /** 范围：1-广播（全体），2-个人（指定人）。 */
    private Integer scope;

    /** 目标用户ID：个人消息填写，广播为 null。 */
    private Long targetUserId;

    /** 标题。 */
    private String title;

    /** 摘要。 */
    private String summary;

    /** 点击跳转路由，空则前端按类型默认跳转。 */
    private String linkUrl;

    /** 完整正文（MEDIUMTEXT），summary 是列表摘要，弹框用。 */
    private String content;

    /** 子类型，例如 membership.subscribed / membership.expiring / feedback.reply。 */
    private String subType;

    /** 租户ID。 */
    private Long tenantId;
}
