package com.aichuangzuo.user.modules.commission.vo;

import com.aichuangzuo.user.modules.commission.entity.CommissionSubmission;
import com.aichuangzuo.user.modules.commission.entity.CommissionTask;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommissionTaskDetailVO {
    private CommissionTask task;
    private long submissionCount;
    private CommissionSubmission mySubmission;
}
