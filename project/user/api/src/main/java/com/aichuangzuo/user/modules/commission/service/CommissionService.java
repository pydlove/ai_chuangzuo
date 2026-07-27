package com.aichuangzuo.user.modules.commission.service;

import com.aichuangzuo.user.modules.commission.entity.CommissionSubmission;
import com.aichuangzuo.user.modules.commission.entity.CommissionTask;
import com.aichuangzuo.user.modules.commission.vo.CommissionTaskDetailVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface CommissionService {
    IPage<CommissionTask> list(Integer status, int page, int pageSize);
    CommissionTaskDetailVO detail(Long userId, Long taskId);
    Long submit(Long userId, Long taskId, String articleBizNo);
    void withdraw(Long userId, Long submissionId);
    IPage<CommissionSubmission> mySubmissions(Long userId, int page, int pageSize);
}
