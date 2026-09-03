package com.aichuangzuo.admin.modules.lottery.service;

import com.aichuangzuo.admin.modules.lottery.dto.request.CloneCampaignRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryCampaignQueryRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryCampaignSaveRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryPrizeTierSaveRequest;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryCampaignAdminVO;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryPrizeTierAdminVO;

import java.util.List;

public interface LotteryCampaignAdminService {

    PageResult listCampaigns(LotteryCampaignQueryRequest request);

    LotteryCampaignAdminVO getCampaign(Long id);

    void saveCampaign(LotteryCampaignSaveRequest request, Long adminUserId);

    void openCampaign(Long id, Long adminUserId);

    void closeCampaign(Long id, Long adminUserId);

    Long cloneCampaign(Long sourceId, CloneCampaignRequest request, Long adminUserId);

    void deleteCampaign(Long id, Long adminUserId);

    List<LotteryPrizeTierAdminVO> listTiers(Long campaignId);

    void saveTier(Long campaignId, LotteryPrizeTierSaveRequest request, Long adminUserId);

    void deleteTier(Long campaignId, Long tierId, Long adminUserId);

    record PageResult(List<LotteryCampaignAdminVO> items, long total, long page, long size) {
    }
}
