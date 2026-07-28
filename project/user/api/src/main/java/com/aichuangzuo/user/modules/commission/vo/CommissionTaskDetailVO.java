package com.aichuangzuo.user.modules.commission.vo;

import com.aichuangzuo.user.modules.commission.entity.CommissionSubmission;
import com.aichuangzuo.user.modules.commission.entity.CommissionTask;
import lombok.Data;

import java.util.List;

/**
 * 约稿任务详情 VO。
 */
@Data
public class CommissionTaskDetailVO {

    private CommissionTask task;

    private long submissionCount;

    private CommissionSubmission mySubmission;

    private List<CommissionSubmitterVO> submitters;

    private List<CommissionSubmitterVO> adopters;

    public CommissionTaskDetailVO(CommissionTask task, long submissionCount,
                                  CommissionSubmission mySubmission,
                                  List<CommissionSubmitterVO> submitters,
                                  List<CommissionSubmitterVO> adopters) {
        this.task = task;
        this.submissionCount = submissionCount;
        this.mySubmission = mySubmission;
        this.submitters = submitters;
        this.adopters = adopters;
    }
}
