package com.aichuangzuo.user.modules.lottery.service;

import com.aichuangzuo.user.modules.lottery.vo.LotteryDisplayWinnerVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryRedemptionCodeVO;

import java.util.List;

public interface LotteryDisplayService {

    List<LotteryDisplayWinnerVO> listDisplayWinners(Long campaignId, int limit);

    List<LotteryRedemptionCodeVO> listMyRedemptionCodes(Long userId);
}
