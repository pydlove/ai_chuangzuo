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

    /** 删除人工发奖记录：删除兑换码、展示墙记录并释放奖项额度（兑换码已使用的除外） */
    void deleteManualGrant(Long recordId);

    /** 修改人工发奖记录的获奖人（仅兑换码未使用时可改） */
    void changeManualGrantUser(Long recordId, Long newUserId);

    record PageResult<T>(java.util.List<T> items, long total, long page, long size) {
    }
}
