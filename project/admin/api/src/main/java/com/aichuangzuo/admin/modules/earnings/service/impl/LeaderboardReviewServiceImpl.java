package com.aichuangzuo.admin.modules.earnings.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.earnings.dto.request.LeaderboardRejectRequest;
import com.aichuangzuo.admin.modules.earnings.entity.IncomeSubmission;
import com.aichuangzuo.admin.modules.earnings.mapper.IncomeSubmissionMapper;
import com.aichuangzuo.admin.modules.earnings.service.LeaderboardReviewService;
import com.aichuangzuo.admin.modules.earnings.vo.IncomeSubmissionAdminVO;
import com.aichuangzuo.shared.enums.error.AdminEarningsErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LeaderboardReviewServiceImpl implements LeaderboardReviewService {

    private final IncomeSubmissionMapper incomeSubmissionMapper;

    @Override
    public Page<IncomeSubmissionAdminVO> listSubmissions(Integer auditStatus, String periodMonth, int page, int size) {
        Page<IncomeSubmission> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<IncomeSubmission> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(IncomeSubmission::getIsDeleted, 0);
        if (auditStatus != null) {
            wrapper.eq(IncomeSubmission::getAuditStatus, auditStatus);
        }
        if (periodMonth != null && !periodMonth.isBlank()) {
            wrapper.eq(IncomeSubmission::getPeriodMonth, periodMonth);
        }
        wrapper.orderByDesc(IncomeSubmission::getCreatedAt);
        Page<IncomeSubmission> result = incomeSubmissionMapper.selectPage(pageParam, wrapper);

        Page<IncomeSubmissionAdminVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVo).toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        updateAuditStatus(id, 1, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, LeaderboardRejectRequest request) {
        updateAuditStatus(id, 2, request.getRejectReason());
    }

    private void updateAuditStatus(Long id, int status, String rejectReason) {
        IncomeSubmission submission = incomeSubmissionMapper.selectById(id);
        if (submission == null || submission.getIsDeleted() == 1 || submission.getAuditStatus() != 0) {
            throw new BusinessException(AdminEarningsErrorCode.SUBMISSION_NOT_FOUND_OR_AUDITED);
        }
        submission.setAuditStatus(status);
        submission.setAuditedBy(SecurityAdminContext.getCurrentAdminUserId());
        submission.setAuditedAt(LocalDateTime.now());
        if (rejectReason != null) {
            submission.setRejectReason(rejectReason);
        }
        incomeSubmissionMapper.updateById(submission);
    }

    private IncomeSubmissionAdminVO toVo(IncomeSubmission entity) {
        IncomeSubmissionAdminVO vo = new IncomeSubmissionAdminVO();
        vo.setId(entity.getId());
        vo.setBizNo(entity.getBizNo());
        vo.setUserId(entity.getUserId());
        vo.setPeriodMonth(entity.getPeriodMonth());
        vo.setAmount(entity.getAmount());
        vo.setPlatform(entity.getPlatform());
        vo.setScreenshotPaths(entity.getScreenshotPaths());
        vo.setAuditStatus(entity.getAuditStatus());
        vo.setAuditedBy(entity.getAuditedBy());
        vo.setAuditedAt(entity.getAuditedAt());
        vo.setRejectReason(entity.getRejectReason());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
