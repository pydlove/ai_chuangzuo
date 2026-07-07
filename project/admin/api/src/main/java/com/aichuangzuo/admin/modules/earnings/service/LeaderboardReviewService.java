package com.aichuangzuo.admin.modules.earnings.service;

import com.aichuangzuo.admin.modules.earnings.dto.request.LeaderboardRejectRequest;
import com.aichuangzuo.admin.modules.earnings.vo.IncomeSubmissionAdminVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface LeaderboardReviewService {
    Page<IncomeSubmissionAdminVO> listSubmissions(Integer auditStatus, String periodMonth, int page, int size);
    void approve(Long id);
    void reject(Long id, LeaderboardRejectRequest request);
}
