package com.aichuangzuo.admin.modules.lottery.service;

import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryDrawRecordQueryRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryManualGrantRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryRedemptionCodeQueryRequest;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryDrawRecordAdminVO;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryRedemptionCodeAdminVO;

public interface LotteryRecordAdminService {

    PageResult<LotteryRedemptionCodeAdminVO> listRedemptionCodes(LotteryRedemptionCodeQueryRequest request);

    PageResult<LotteryDrawRecordAdminVO> listDrawRecords(LotteryDrawRecordQueryRequest request);

    void resetDrawChance(Long campaignId, Long userId);

    /** 人工分配中奖：等效于用户抽中该奖项，扣库存、生成兑换码、写抽奖记录与展示墙 */
    void manualGrant(LotteryManualGrantRequest request);

    record PageResult<T>(java.util.List<T> items, long total, long page, long size) {
    }
}
