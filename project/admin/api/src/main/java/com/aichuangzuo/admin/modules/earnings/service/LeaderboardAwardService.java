package com.aichuangzuo.admin.modules.earnings.service;

import com.aichuangzuo.admin.modules.earnings.dto.request.LeaderboardGrantRequest;
import com.aichuangzuo.admin.modules.earnings.vo.LeaderboardTop10VO;
import com.aichuangzuo.admin.modules.earnings.vo.RewardRecordAdminVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface LeaderboardAwardService {
    List<LeaderboardTop10VO> preview(Integer leaderboardType, String periodMonth);
    int grant(LeaderboardGrantRequest request);
    Page<RewardRecordAdminVO> listRewards(Integer leaderboardType, String periodMonth, int page, int size);
}
