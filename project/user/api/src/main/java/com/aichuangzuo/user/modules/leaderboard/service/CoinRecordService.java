package com.aichuangzuo.user.modules.leaderboard.service;

import java.math.BigDecimal;

/**
 * 用户创作币流水与余额服务。
 */
public interface CoinRecordService {

    /**
     * 给用户入账创作币。
     *
     * @param userId 用户ID
     * @param bizType 业务类型
     * @param amount 金额（必须为正）
     * @param refId 关联业务ID
     * @param remark 备注
     * @return 流水业务编号
     */
    String grant(Long userId, String bizType, BigDecimal amount, String refId, String remark);

    /**
     * 扣减用户创作币。
     *
     * @param userId 用户ID
     * @param bizType 业务类型
     * @param amount 金额（必须为正）
     * @param refId 关联业务ID
     * @param remark 备注
     * @return 流水业务编号
     */
    String spend(Long userId, String bizType, BigDecimal amount, String refId, String remark);

    /**
     * 查询用户当前余额。
     *
     * @param userId 用户ID
     * @return 余额，用户不存在返回 0
     */
    BigDecimal getBalance(Long userId);
}
