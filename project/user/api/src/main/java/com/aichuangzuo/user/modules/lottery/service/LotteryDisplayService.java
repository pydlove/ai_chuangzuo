package com.aichuangzuo.user.modules.lottery.service;

import com.aichuangzuo.user.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.user.modules.lottery.entity.LotteryPrizeTier;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDisplayWinnerPageVO;
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

    /**
     * 分页查询中奖展示墙（按中奖时间倒序）。
     *
     * @param campaignId 活动 ID
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    LotteryDisplayWinnerPageVO listDisplayWinnersPage(Long campaignId, int page, int pageSize);

    /**
     * 查询全部大奖得主（特等奖/一等奖/二等奖），按奖项等级升序、中奖时间倒序。
     *
     * @param campaignId 活动 ID
     * @return 大奖得主列表
     */
    List<LotteryDisplayWinnerVO> listGrandWinners(Long campaignId);

    List<LotteryRedemptionCodeVO> listMyRedemptionCodes(Long userId);
}
