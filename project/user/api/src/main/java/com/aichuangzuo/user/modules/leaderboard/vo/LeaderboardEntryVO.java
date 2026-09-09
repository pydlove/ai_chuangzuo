package com.aichuangzuo.user.modules.leaderboard.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 榜单单项 VO。
 */
@Data
public class LeaderboardEntryVO {

    /** 用户ID。 */
    private Long userId;

    /** 用户昵称。 */
    private String nickname;

    /** 头像 URL。 */
    private String avatarUrl;

    /** 统计金额。 */
    private BigDecimal amount;

    /** 排名。 */
    private Integer rank;

    /** 是否为当前登录用户。 */
    private Boolean isMe;

    /** 会员等级：basic/pro/flagship；非会员或已过期为 null。 */
    private String memberLevel;

    /** 累计邀请人数（邀请排行榜用）。 */
    private Long inviteCount;
}
