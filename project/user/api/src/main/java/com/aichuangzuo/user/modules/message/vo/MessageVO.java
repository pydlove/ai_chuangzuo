package com.aichuangzuo.user.modules.message.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息视图对象，用于用户端消息中心展示。
 */
@Getter
@Setter
public class MessageVO {

    /** 消息ID。 */
    private Long id;

    /** 消息类型：announcement / feature / promotion / generation / membership。 */
    private String type;

    /** 标题。 */
    private String title;

    /** 摘要。 */
    private String summary;

    /** 完整正文，前端详情弹框用，可空（无 content 时回退到 summary）。 */
    private String content;

    /** 子类型，可空（如 membership.subscribed / membership.expiring）。 */
    private String subType;

    /** 点击跳转路由，可能为 null。 */
    private String link;

    /** 是否已读。 */
    private Boolean read;

    /** 创建时间。 */
    private LocalDateTime createdAt;
}
