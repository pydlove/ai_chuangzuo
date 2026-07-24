package com.aichuangzuo.user.modules.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 邀请有礼统计视图，供「我的」页和邀请弹框展示。
 */
@Data
public class InviteStatsVO {

    /** 当前用户的邀请码。 */
    private String inviteCode;

    /** 累计有效邀请人数。 */
    private Integer invitedCount;

    /** 累计获得的会员天数奖励。 */
    private Integer membershipDaysEarned;

    /** 累计获得的创作币返利（仅邀请人所得，不含被邀请人注册奖励）。 */
    private BigDecimal coinEarned;

    /** 当前创作币余额。 */
    private BigDecimal coinBalance;

    /** 被邀请人列表。 */
    private List<InviteFriendVO> friends = new ArrayList<>();
}
