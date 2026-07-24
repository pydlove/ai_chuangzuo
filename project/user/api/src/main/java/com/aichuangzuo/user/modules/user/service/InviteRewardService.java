package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.user.vo.InviteStatsVO;

/**
 * 邀请奖励服务。
 *
 * <p>处理邀请关系的建立、奖励发放以及邀请统计查询。
 */
public interface InviteRewardService {

    /**
     * 新用户通过邀请码注册后，给被邀请人发注册奖励，并给邀请人累计阶梯会员天数。
     *
     * <p>该方法应在新用户事务内调用，失败会随注册事务一起回滚。
     *
     * @param invitee    被邀请人（已持久化）
     * @param inviteCode 邀请码
     */
    void rewardAfterRegister(User invitee, String inviteCode);

    /**
     * 已注册用户补绑邀请人后，发放奖励。
     *
     * <p>与 {@link #rewardAfterRegister} 的区别：调用方已经插入了邀请关系，
     * 本方法只负责发 5 币和计算邀请人会员天数。
     *
     * @param invitee  被邀请人（当前登录用户）
     * @param inviter  邀请人
     */
    void rewardAfterBinding(User invitee, User inviter);

    /**
     * 查询当前用户的邀请统计信息。
     *
     * @param userId 当前用户 ID
     * @return 邀请统计视图
     */
    InviteStatsVO getInviteStats(Long userId);
}
