package com.aichuangzuo.admin.modules.lottery.service;

import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryDrawRecordQueryRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryRedemptionCodeQueryRequest;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryDrawRecordAdminVO;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryRedemptionCodeAdminVO;

public interface LotteryRecordAdminService {

    PageResult<LotteryRedemptionCodeAdminVO> listRedemptionCodes(LotteryRedemptionCodeQueryRequest request);

    PageResult<LotteryDrawRecordAdminVO> listDrawRecords(LotteryDrawRecordQueryRequest request);

    void resetDrawChance(Long campaignId, Long userId);

    record PageResult<T>(java.util.List<T> items, long total, long page, long size) {
    }
}
