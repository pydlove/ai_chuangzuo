package com.aichuangzuo.admin.modules.message.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端-消息管理-列表/详情 VO。
 *
 * <p>列表里 id 字段等于 u_message 表里代表行的 id（按 biz_no 聚合后取 created_at 最新的一行的 id），
 * 详情里 id 即消息主键。bizNo 用于后端再聚合查询已读统计与受众列表。
 */
@Data
public class MessageAdminVO {

    /** 代表行 id（按 biz_no 聚合后的主键）。 */
    private Long id;

    /** 批次号。 */
    private String bizNo;

    /** 消息类型。 */
    private String msgType;

    /** 消息类型展示名。 */
    private String msgTypeLabel;

    /** 标题。 */
    private String title;

    /** 摘要。 */
    private String summary;

    /** 跳转链接。 */
    private String linkUrl;

    /** 范围：1-广播，2-个人。 */
    private Integer scope;

    /** 范围展示名。 */
    private String scopeLabel;

    /** 受众数量：全体=u_user 总数；指定人=targetUserIds 数。 */
    private Long audienceCount;

    /** 受众展示文本，例如「全体 58 人」「指定 3 人」。 */
    private String audienceLabel;

    /** 已读人数。 */
    private Long readCount;

    /** 已读数 / 送达数展示文本，例如「12/58」。 */
    private String readLabel;

    /** 创建人ID。 */
    private Long createdBy;

    /** 创建人展示名。 */
    private String createdByName;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 仅详情：受众用户列表（指定人消息才有意义，广播返回空）。 */
    private List<MessageAudienceVO> audience;
}
