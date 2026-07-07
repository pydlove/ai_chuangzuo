package com.aichuangzuo.admin.modules.leaderboard.service.impl;

import com.aichuangzuo.admin.modules.leaderboard.entity.IncomeSubmission;
import com.aichuangzuo.admin.modules.leaderboard.entity.SubmissionStatus;
import com.aichuangzuo.admin.modules.leaderboard.enums.AdminLeaderboardErrorCode;
import com.aichuangzuo.admin.modules.leaderboard.mapper.IncomeSubmissionMapper;
import com.aichuangzuo.admin.modules.leaderboard.service.LeaderboardReviewService;
import com.aichuangzuo.admin.modules.leaderboard.vo.IncomeSubmissionAdminVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 收益排行榜审核服务实现。
 */
@Service
@RequiredArgsConstructor
public class LeaderboardReviewServiceImpl implements LeaderboardReviewService {

    private final IncomeSubmissionMapper submissionMapper;

    @Override
    public IPage<IncomeSubmissionAdminVO> page(Integer status, IPage<IncomeSubmissionAdminVO> pageParam) {
        LambdaQueryWrapper<IncomeSubmission> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(IncomeSubmission::getIsDeleted, 0);
        if (status != null) {
            wrapper.eq(IncomeSubmission::getAuditStatus, status);
        }
        wrapper.orderByDesc(IncomeSubmission::getCreatedAt);
        Page<IncomeSubmission> entityPage = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        submissionMapper.selectPage(entityPage, wrapper);
        return entityPage.convert(this::toAdminVo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long submissionId, Long adminUserId) {
        IncomeSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null || submission.getAuditStatus() != SubmissionStatus.PENDING.getCode()) {
            throw new BusinessException(AdminLeaderboardErrorCode.SUBMISSION_NOT_FOUND);
        }
        submission.setAuditStatus(SubmissionStatus.APPROVED.getCode());
        submission.setAuditedBy(adminUserId);
        submission.setAuditedAt(LocalDateTime.now());
        submission.setUpdatedBy(adminUserId);
        submissionMapper.updateById(submission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long submissionId, Long adminUserId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(AdminLeaderboardErrorCode.REJECT_REASON_EMPTY);
        }
        IncomeSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null || submission.getAuditStatus() != SubmissionStatus.PENDING.getCode()) {
            throw new BusinessException(AdminLeaderboardErrorCode.SUBMISSION_NOT_FOUND);
        }
        submission.setAuditStatus(SubmissionStatus.REJECTED.getCode());
        submission.setAuditedBy(adminUserId);
        submission.setAuditedAt(LocalDateTime.now());
        submission.setRejectReason(reason.trim());
        submission.setUpdatedBy(adminUserId);
        submissionMapper.updateById(submission);
    }

    private IncomeSubmissionAdminVO toAdminVo(IncomeSubmission entity) {
        IncomeSubmissionAdminVO vo = new IncomeSubmissionAdminVO();
        vo.setId(entity.getId());
        vo.setBizNo(entity.getBizNo());
        vo.setUserId(entity.getUserId());
        vo.setPeriodMonth(entity.getPeriodMonth());
        vo.setAmount(entity.getAmount());
        vo.setPlatform(entity.getPlatform());
        vo.setAuditStatus(entity.getAuditStatus());
        vo.setAuditedBy(entity.getAuditedBy());
        vo.setAuditedAt(entity.getAuditedAt());
        vo.setRejectReason(entity.getRejectReason());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
