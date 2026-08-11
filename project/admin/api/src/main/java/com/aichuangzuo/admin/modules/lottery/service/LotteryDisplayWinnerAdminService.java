package com.aichuangzuo.admin.modules.lottery.service;

import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryDisplayWinnerSaveRequest;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryDisplayWinnerAdminVO;

import java.util.List;

public interface LotteryDisplayWinnerAdminService {

    List<LotteryDisplayWinnerAdminVO> listByCampaign(Long campaignId);

    void saveWinner(LotteryDisplayWinnerSaveRequest request, Long adminUserId);

    void toggleStatus(Long id, Integer status, Long adminUserId);

    void deleteWinner(Long id, Long adminUserId);
}
