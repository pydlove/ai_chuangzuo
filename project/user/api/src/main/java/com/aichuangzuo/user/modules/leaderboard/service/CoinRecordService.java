package com.aichuangzuo.user.modules.leaderboard.service;

import java.math.BigDecimal;

public interface CoinRecordService {

    /**
     * 给用户增加创作币，同时更新余额快照。必须在事务内调用。
     *
     * @param userId  用户ID
     * @param bizType 业务类型，如 leaderboard_reward
     * @param amount  金额（正数）
     * @param refId   关联业务ID
     * @param remark  备注
     * @return 流水业务编号 biz_no
     */
    String grant(Long userId, String bizType, BigDecimal amount, String refId, String remark);
}
