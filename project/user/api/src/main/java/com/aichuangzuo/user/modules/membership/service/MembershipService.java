package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;

/**
 * 会员服务。
 */
public interface MembershipService {

    /**
     * 立即订阅（测试支付）。
     *
     * @param userId  当前用户ID
     * @param request 订阅请求
     * @return 订阅结果
     */
    SubscribeResultVO subscribe(Long userId, SubscribeRequest request);

    /**
     * 查询当前用户会员状态。
     *
     * @param userId 用户ID
     * @return 会员状态
     */
    MembershipStatusVO getMyMembership(Long userId);

    /**
     * 给指定用户延长会员天数。
     *
     * <p>若用户无会员或已过期，从今天起算；否则从当前到期日起算。
     *
     * @param userId 用户ID
     * @param level  会员等级
     * @param days   延长天数
     */
    void extendMembership(Long userId, String level, long days);
}
