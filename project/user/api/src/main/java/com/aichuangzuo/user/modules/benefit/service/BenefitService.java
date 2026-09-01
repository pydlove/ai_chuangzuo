package com.aichuangzuo.user.modules.benefit.service;

import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.benefit.vo.UserBenefitVO;

/**
 * 会员权益服务。
 */
public interface BenefitService {

    /**
     * 查询当前用户的套餐与全部权益（quota 类含已用量/剩余额度）。
     *
     * @param userId 用户ID
     * @return 用户权益视图；无会员时 planKey=free 且权益列表为空
     */
    UserBenefitVO getMyBenefits(Long userId);

    /**
     * 校验单项权益是否可用（quota 类只读不写）。
     *
     * @param userId 用户ID
     * @param code 权益编码
     * @return 校验结果
     */
    BenefitCheckVO check(Long userId, String code);

    /**
     * 消费一次配额（仅 quota 类），成功返回最新剩余额度。
     *
     * @param userId 用户ID
     * @param code 权益编码
     * @return 消费结果
     */
    BenefitCheckVO consume(Long userId, String code);

    /**
     * 预扣一次配额（仅 quota 类）。
     * 预扣不会立即增加 used_count，而是增加 pre_used_count，待业务确认后再转正。
     *
     * @param userId 用户ID
     * @param code 权益编码
     * @return 预扣结果
     */
    BenefitCheckVO preConsume(Long userId, String code);

    /**
     * 确认预扣：将 1 次预扣额度转为正式用量。
     *
     * @param userId 用户ID
     * @param code 权益编码
     */
    void confirmPreConsume(Long userId, String code);

    /**
     * 取消预扣：释放 1 次预扣额度。
     *
     * @param userId 用户ID
     * @param code 权益编码
     */
    void cancelPreConsume(Long userId, String code);

    /**
     * 退回一次配额（业务失败时调用），当前周期用量 -1，下限 0。
     * 已废弃：请使用 {@link #cancelPreConsume(Long, String)} 释放预扣。
     *
     * @param userId 用户ID
     * @param code 权益编码
     */
    @Deprecated
    void refund(Long userId, String code);

    /**
     * 读取当前用户某套餐权益的配置值（只读，不扣额度）。
     * 无会员、会员已过期或权益未配置时返回 defaultValue。
     *
     * @param userId 用户ID
     * @param code 权益编码
     * @param defaultValue 默认值
     * @return 权益配置值
     */
    String getPlanBenefitValue(Long userId, String code, String defaultValue);

    /**
     * 读取当前用户的有效套餐 key；无会员或已过期返回 "free"。
     *
     * @param userId 用户ID
     * @return 套餐 key：free / basic / pro / flagship
     */
    String getCurrentPlanKey(Long userId);
}
