package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.membership.dto.request.SubscribePreviewRequest;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.dto.request.UpgradePreviewRequest;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribePreviewVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import com.aichuangzuo.user.modules.membership.vo.UpgradePreviewVO;

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
     * 订阅价格预览：计算套餐现金应付及创作币可抵扣上限。
     *
     * @param userId  当前用户ID
     * @param request 订阅预览请求
     * @return 订阅预览结果
     */
    SubscribePreviewVO previewSubscribe(Long userId, SubscribePreviewRequest request);

    /**
     * 查询当前用户会员状态。
     *
     * @param userId 用户ID
     * @return 会员状态
     */
    MembershipStatusVO getMyMembership(Long userId);

    /**
     * 升级套餐价格预览：计算当前订阅剩余价值可用于抵扣的金额。
     *
     * @param userId  当前用户ID
     * @param request 升级预览请求
     * @return 升级预览结果
     */
    UpgradePreviewVO previewUpgrade(Long userId, UpgradePreviewRequest request);

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
