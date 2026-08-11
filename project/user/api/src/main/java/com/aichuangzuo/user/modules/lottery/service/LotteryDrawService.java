package com.aichuangzuo.user.modules.lottery.service;

import com.aichuangzuo.user.modules.lottery.vo.LotteryDrawResultVO;

public interface LotteryDrawService {

    LotteryDrawResultVO draw(Long userId, Long campaignId);
}
