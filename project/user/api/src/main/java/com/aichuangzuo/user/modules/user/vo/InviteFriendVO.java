package com.aichuangzuo.user.modules.user.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 邀请有礼弹框中的好友贡献信息。
 */
@Data
public class InviteFriendVO {

    /** 被邀请人邮箱。 */
    private String email;

    /** 被邀请人昵称；未设置时退化展示。 */
    private String nickname;

    /** 状态：registered 已注册 / purchased 已购买。 */
    private String status;

    /** 该好友给邀请人带来的创作币返利合计。 */
    private BigDecimal commission;
}
