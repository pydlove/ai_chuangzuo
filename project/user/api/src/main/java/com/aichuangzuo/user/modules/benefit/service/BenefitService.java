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
}
