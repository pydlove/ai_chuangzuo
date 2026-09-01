package com.aichuangzuo.user.modules.lottery.service;

import com.aichuangzuo.user.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.user.modules.lottery.entity.LotteryPrizeTier;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDisplayWinnerVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryRedemptionCodeVO;

import java.util.List;

public interface LotteryDisplayService {

    /**
     * 查询当前正在进行中的抽奖活动（已启用、未删除、时间在有效期内）。
     *
     * @return 当前活动；无则返回 null
     */
    LotteryCampaign getCurrentCampaign();

    /**
     * 根据 ID 查询活动。
     *
     * @param campaignId 活动 ID
     * @return 活动实体；不存在返回 null
     */
    LotteryCampaign getCampaignById(Long campaignId);

    /**
     * 查询指定活动下所有启用的奖项档位。
     *
     * @param campaignId 活动 ID
     * @return 奖项档位列表
     */
    List<LotteryPrizeTier> listActiveTiersByCampaignId(Long campaignId);

    List<LotteryDisplayWinnerVO> listDisplayWinners(Long campaignId, int limit);

    List<LotteryRedemptionCodeVO> listMyRedemptionCodes(Long userId);
}
