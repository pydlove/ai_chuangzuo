package com.aichuangzuo.admin.modules.commission.vo;

import com.aichuangzuo.admin.modules.commission.entity.CommissionSubmission;
import com.aichuangzuo.admin.modules.commission.entity.CommissionTask;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CommissionTaskDetailVO {
    private CommissionTask task;
    private List<CommissionSubmission> submissions;
}
