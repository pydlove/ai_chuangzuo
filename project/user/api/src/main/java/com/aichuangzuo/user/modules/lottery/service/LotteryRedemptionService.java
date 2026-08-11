package com.aichuangzuo.user.modules.lottery.service;

import com.aichuangzuo.user.modules.lottery.vo.LotteryRedemptionResultVO;

public interface LotteryRedemptionService {

    LotteryRedemptionResultVO redeem(Long userId, String code);
}
