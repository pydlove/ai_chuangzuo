package com.aichuangzuo.admin.modules.commission.service.impl;

import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskUpdateRequest;
import com.aichuangzuo.admin.modules.commission.entity.CommissionSubmission;
import com.aichuangzuo.admin.modules.commission.entity.CommissionTask;
import com.aichuangzuo.admin.modules.commission.enums.AdminCommissionErrorCode;
import com.aichuangzuo.admin.modules.commission.mapper.CommissionSubmissionMapper;
import com.aichuangzuo.admin.modules.commission.mapper.CommissionTaskMapper;
import com.aichuangzuo.admin.modules.commission.service.AdminCommissionService;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskDetailVO;
import com.aichuangzuo.admin.modules.leaderboard.client.UserApiClient;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCommissionServiceImpl implements AdminCommissionService {
    private static final int SUBMISSION = 0;
    private static final int REVIEW = 1;
    private static final int PUBLIC = 2;
    private static final int COMPLETED = 3;
    private static final int SUBMISSION_STATUS_SUBMITTED = 0;
    private static final int SUBMISSION_STATUS_ADOPTED = 1;
    private static final int SUBMISSION_STATUS_NOT_ADOPTED = 2;

    private final CommissionTaskMapper taskMapper;
    private final CommissionSubmissionMapper submissionMapper;
    private final UserApiClient userApiClient;

    @Override
    public IPage<CommissionTask> list(String keyword, Integer status, int page, int pageSize) {
        reconcilePhases();
        LambdaQueryWrapper<CommissionTask> query = new LambdaQueryWrapper<CommissionTask>()
                .eq(CommissionTask::getIsDeleted, 0)
                .eq(status != null, CommissionTask::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(CommissionTask::getTitle, keyword)
                        .or().like(CommissionTask::getTaskNo, keyword))
                .orderByDesc(CommissionTask::getCreatedAt);
        return taskMapper.selectPage(new Page<>(page, pageSize), query);
    }

    @Override
    public CommissionTaskDetailVO detail(Long taskId) {
        reconcilePhase(taskId);
        CommissionTask task = findTask(taskId);
        List<CommissionSubmission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<CommissionSubmission>()
                        .eq(CommissionSubmission::getTaskId, taskId)
                        .eq(CommissionSubmission::getIsDeleted, 0)
                        .orderByDesc(CommissionSubmission::getCreatedAt));
        return new CommissionTaskDetailVO(task, submissions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CommissionTaskCreateRequest request, Long adminId) {
        validateDeadlines(request.getDeadlineAt(), request.getSelectionDeadlineAt(), request.getMinWordCount(), request.getMaxWordCount());
        CommissionTask task = new CommissionTask();
        task.setTaskNo("CT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription().trim());
        task.setMinWordCount(request.getMinWordCount());
        task.setMaxWordCount(request.getMaxWordCount());
        task.setStyleHint(request.getStyleHint());
        task.setRewardCoin(request.getRewardCoin());
        task.setNeededCount(request.getNeededCount());
        task.setAdoptedCount(0);
        task.setStatus(SUBMISSION);
        task.setDeadlineAt(request.getDeadlineAt());
        task.setSelectionDeadlineAt(request.getSelectionDeadlineAt());
        task.setPublishedBy(adminId);
        task.setTenantId(0L);
        taskMapper.insert(task);
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long taskId, CommissionTaskUpdateRequest request) {
        CommissionTask task = taskMapper.selectByIdForUpdate(taskId);
        ensureTask(task);
        if (task.getStatus() != SUBMISSION) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        validateDeadlines(request.getDeadlineAt(), request.getSelectionDeadlineAt(), request.getMinWordCount(), request.getMaxWordCount());
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription().trim());
        task.setMinWordCount(request.getMinWordCount());
        task.setMaxWordCount(request.getMaxWordCount());
        task.setStyleHint(request.getStyleHint());
        task.setRewardCoin(request.getRewardCoin());
        task.setNeededCount(request.getNeededCount());
        task.setDeadlineAt(request.getDeadlineAt());
        task.setSelectionDeadlineAt(request.getSelectionDeadlineAt());
        taskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long taskId) {
        CommissionTask task = taskMapper.selectByIdForUpdate(taskId);
        ensureTask(task);
        if (task.getStatus() != SUBMISSION) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        task.setStatus(REVIEW);
        taskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void announce(Long taskId) {
        CommissionTask task = taskMapper.selectByIdForUpdate(taskId);
        ensureTask(task);
        if (task.getStatus() != REVIEW) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        task.setStatus(PUBLIC);
        taskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adopt(Long taskId, List<Long> submissionIds) {
        CommissionTask task = taskMapper.selectByIdForUpdate(taskId);
        ensureTask(task);
        if (task.getStatus() == SUBMISSION && !LocalDateTime.now().isBefore(task.getDeadlineAt())) {
            task.setStatus(REVIEW);
        }
        if (task.getStatus() != REVIEW) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        List<Long> ids = List.copyOf(new LinkedHashSet<>(submissionIds));
        int remaining = task.getNeededCount() - task.getAdoptedCount();
        if (ids.isEmpty() || ids.size() > remaining) {
            throw new BusinessException(AdminCommissionErrorCode.ADOPT_COUNT_EXCEEDED);
        }
        List<CommissionSubmission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<CommissionSubmission>()
                        .eq(CommissionSubmission::getTaskId, taskId)
                        .in(CommissionSubmission::getId, ids)
                        .eq(CommissionSubmission::getStatus, SUBMISSION_STATUS_SUBMITTED));
        if (submissions.size() != ids.size()) {
            throw new BusinessException(AdminCommissionErrorCode.SUBMISSION_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        for (CommissionSubmission submission : submissions) {
            String refId = "commission:" + submission.getId();
            String coinBizNo = userApiClient.grantCoin(submission.getSubmitterId(), "commission_reward",
                    task.getRewardCoin(), refId, "约稿采纳奖励：" + task.getTitle());
            submission.setStatus(SUBMISSION_STATUS_ADOPTED);
            submission.setRewardCoin(task.getRewardCoin());
            submission.setCoinRecordBizNo(coinBizNo);
            submission.setAdoptedAt(now);
            submissionMapper.updateById(submission);
        }
        task.setAdoptedCount(task.getAdoptedCount() + submissions.size());
        if (task.getAdoptedCount() >= task.getNeededCount()) {
            task.setStatus(COMPLETED);
            task.setCompletedAt(now);
            submissionMapper.update(null, new LambdaUpdateWrapper<CommissionSubmission>()
                    .set(CommissionSubmission::getStatus, SUBMISSION_STATUS_NOT_ADOPTED)
                    .eq(CommissionSubmission::getTaskId, taskId)
                    .eq(CommissionSubmission::getStatus, SUBMISSION_STATUS_SUBMITTED));
        }
        taskMapper.updateById(task);
    }

    private void validateDeadlines(LocalDateTime deadlineAt, LocalDateTime selectionDeadlineAt, Integer minWordCount, Integer maxWordCount) {
        if (minWordCount > maxWordCount) {
            throw new BusinessException(AdminCommissionErrorCode.PARAM_INVALID);
        }
        if (!selectionDeadlineAt.isAfter(deadlineAt)) {
            throw new BusinessException(AdminCommissionErrorCode.PARAM_INVALID);
        }
    }

    private void reconcilePhases() {
        LocalDateTime now = LocalDateTime.now();
        taskMapper.update(null, new LambdaUpdateWrapper<CommissionTask>()
                .set(CommissionTask::getStatus, REVIEW)
                .eq(CommissionTask::getStatus, SUBMISSION)
                .le(CommissionTask::getDeadlineAt, now));
        taskMapper.update(null, new LambdaUpdateWrapper<CommissionTask>()
                .set(CommissionTask::getStatus, PUBLIC)
                .eq(CommissionTask::getStatus, REVIEW)
                .le(CommissionTask::getSelectionDeadlineAt, now));
    }

    private void reconcilePhase(Long taskId) {
        LocalDateTime now = LocalDateTime.now();
        taskMapper.update(null, new LambdaUpdateWrapper<CommissionTask>()
                .set(CommissionTask::getStatus, REVIEW)
                .eq(CommissionTask::getId, taskId)
                .eq(CommissionTask::getStatus, SUBMISSION)
                .le(CommissionTask::getDeadlineAt, now));
        taskMapper.update(null, new LambdaUpdateWrapper<CommissionTask>()
                .set(CommissionTask::getStatus, PUBLIC)
                .eq(CommissionTask::getId, taskId)
                .eq(CommissionTask::getStatus, REVIEW)
                .le(CommissionTask::getSelectionDeadlineAt, now));
    }

    private CommissionTask findTask(Long taskId) {
        CommissionTask task = taskMapper.selectById(taskId);
        ensureTask(task);
        return task;
    }

    private void ensureTask(CommissionTask task) {
        if (task == null || Integer.valueOf(1).equals(task.getIsDeleted())) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_NOT_FOUND);
        }
    }
}