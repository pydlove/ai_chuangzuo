package com.aichuangzuo.user.modules.commission.service;

import com.aichuangzuo.user.modules.commission.vo.CommissionStatsVO;
import com.aichuangzuo.user.modules.commission.vo.CommissionSubmissionMineVO;
import com.aichuangzuo.user.modules.commission.vo.CommissionTaskDetailVO;
import com.aichuangzuo.user.modules.commission.vo.CommissionTaskVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface CommissionService {
    IPage<CommissionTaskVO> list(String status, int page, int pageSize);
    CommissionTaskDetailVO detail(Long userId, Long taskId);
    Long submit(Long userId, Long taskId, String articleBizNo);
    void withdraw(Long userId, Long submissionId);
    IPage<CommissionSubmissionMineVO> mySubmissions(Long userId, int page, int pageSize);
    CommissionStatsVO stats(Long userId);
}
