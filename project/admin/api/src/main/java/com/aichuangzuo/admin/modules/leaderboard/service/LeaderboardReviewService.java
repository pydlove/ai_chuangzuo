package com.aichuangzuo.admin.modules.leaderboard.service;

import com.aichuangzuo.admin.modules.leaderboard.vo.IncomeSubmissionAdminVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 收益排行榜审核服务。
 */
public interface LeaderboardReviewService {

    /**
     * 分页查询收入申报记录。
     *
     * @param status 审核状态，null 表示全部
     * @param pageParam 分页参数
     */
    IPage<IncomeSubmissionAdminVO> page(Integer status, IPage<IncomeSubmissionAdminVO> pageParam);

    /**
     * 通过申报。
     */
    void approve(Long submissionId, Long adminUserId);

    /**
     * 拒绝申报。
     */
    void reject(Long submissionId, Long adminUserId, String reason);
}
