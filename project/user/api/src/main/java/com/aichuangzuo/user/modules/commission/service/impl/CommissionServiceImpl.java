package com.aichuangzuo.user.modules.commission.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.article.entity.Article;
import com.aichuangzuo.user.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.user.modules.commission.entity.CommissionSubmission;
import com.aichuangzuo.user.modules.commission.entity.CommissionTask;
import com.aichuangzuo.user.modules.commission.enums.CommissionErrorCode;
import com.aichuangzuo.user.modules.commission.enums.CommissionTaskStatus;
import com.aichuangzuo.user.modules.commission.mapper.CommissionSubmissionMapper;
import com.aichuangzuo.user.modules.commission.mapper.CommissionTaskMapper;
import com.aichuangzuo.user.modules.commission.service.CommissionService;
import com.aichuangzuo.user.modules.commission.vo.CommissionStatsVO;
import com.aichuangzuo.user.modules.commission.vo.CommissionSubmissionMineVO;
import com.aichuangzuo.user.modules.commission.vo.CommissionSubmitterVO;
import com.aichuangzuo.user.modules.commission.vo.CommissionTaskDetailVO;
import com.aichuangzuo.user.modules.commission.vo.CommissionTaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {
    private static final int SUBMISSION = 0;
    private static final int REVIEW = 1;
    private static final int SUBMISSION_STATUS_SUBMITTED = 0;
    private static final int SUBMISSION_STATUS_WITHDRAWN = 3;
    private static final int DETAIL_SUBMITTER_LIMIT = 10;

    private final CommissionTaskMapper taskMapper;
    private final CommissionSubmissionMapper submissionMapper;
    private final ArticleMapper articleMapper;

    @Override
    public IPage<CommissionTaskVO> list(String status, int page, int pageSize) {
        reconcilePhases();
        List<Integer> statuses = CommissionTaskStatus.resolveCodes(status);
        boolean filterByStatus = status != null && !status.isBlank() && !statuses.isEmpty();
        IPage<CommissionTask> taskPage = taskMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<CommissionTask>()
                        .eq(CommissionTask::getIsDeleted, 0)
                        .in(filterByStatus, CommissionTask::getStatus, statuses)
                        .orderByDesc(CommissionTask::getCreatedAt));

        List<CommissionTask> records = taskPage.getRecords();
        Map<Long, Long> countMap = countSubmissionsByTaskIds(records);

        List<CommissionTaskVO> voList = records.stream()
                .map(task -> toTaskVO(task, countMap.getOrDefault(task.getId(), 0L)))
                .collect(Collectors.toList());

        IPage<CommissionTaskVO> result = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public CommissionTaskDetailVO detail(Long userId, Long taskId) {
        reconcilePhase(taskId);
        CommissionTask task = findTask(taskId);
        long submissionCount = submissionMapper.selectCount(new LambdaQueryWrapper<CommissionSubmission>()
                .eq(CommissionSubmission::getTaskId, taskId)
                .ne(CommissionSubmission::getStatus, SUBMISSION_STATUS_WITHDRAWN));
        CommissionSubmission mine = submissionMapper.selectOne(new LambdaQueryWrapper<CommissionSubmission>()
                .eq(CommissionSubmission::getTaskId, taskId)
                .eq(CommissionSubmission::getSubmitterId, userId)
                .orderByDesc(CommissionSubmission::getCreatedAt)
                .last("LIMIT 1"));
        List<CommissionSubmitterVO> submitters = submissionMapper.selectSubmittersByTaskId(taskId, DETAIL_SUBMITTER_LIMIT);
        List<CommissionSubmitterVO> adopters = submissionMapper.selectAdoptersByTaskId(taskId);
        return new CommissionTaskDetailVO(task, submissionCount, mine, submitters, adopters);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, Long taskId, String articleBizNo) {
        reconcilePhase(taskId);
        CommissionTask task = findTask(taskId);
        if (task.getStatus() != SUBMISSION || !LocalDateTime.now().isBefore(task.getDeadlineAt())) {
            throw new BusinessException(CommissionErrorCode.TASK_NOT_RECRUITING);
        }
        Long active = submissionMapper.selectCount(new LambdaQueryWrapper<CommissionSubmission>()
                .eq(CommissionSubmission::getTaskId, taskId)
                .eq(CommissionSubmission::getSubmitterId, userId)
                .eq(CommissionSubmission::getStatus, SUBMISSION_STATUS_SUBMITTED));
        if (active > 0) {
            throw new BusinessException(CommissionErrorCode.ACTIVE_SUBMISSION_EXISTS);
        }
        Long articleSubmitted = submissionMapper.selectCount(new LambdaQueryWrapper<CommissionSubmission>()
                .eq(CommissionSubmission::getSubmitterId, userId)
                .eq(CommissionSubmission::getArticleBizNo, articleBizNo)
                .ne(CommissionSubmission::getStatus, SUBMISSION_STATUS_WITHDRAWN));
        if (articleSubmitted > 0) {
            throw new BusinessException(CommissionErrorCode.ARTICLE_ALREADY_SUBMITTED);
        }
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getUserId, userId)
                .eq(Article::getBizNo, articleBizNo)
                .eq(Article::getIsDeleted, 0));
        if (article == null) {
            throw new BusinessException(CommissionErrorCode.ARTICLE_NOT_FOUND);
        }
        if (article.getCompletedAt() == null) {
            throw new BusinessException(CommissionErrorCode.ARTICLE_NOT_COMPLETED);
        }
        if (article.getWordCount() == null || article.getWordCount() < task.getMinWordCount()
                || article.getWordCount() > task.getMaxWordCount()) {
            throw new BusinessException(CommissionErrorCode.ARTICLE_WORD_COUNT_INVALID);
        }
        CommissionSubmission submission = new CommissionSubmission();
        submission.setTaskId(taskId);
        submission.setSubmitterId(userId);
        submission.setArticleBizNo(article.getBizNo());
        submission.setArticleTitle(article.getTitle());
        submission.setArticleBody(article.getBody());
        submission.setWordCount(article.getWordCount());
        submission.setStatus(SUBMISSION_STATUS_SUBMITTED);
        submission.setTenantId(0L);
        submissionMapper.insert(submission);
        return submission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long userId, Long submissionId) {
        CommissionSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<CommissionSubmission>()
                .eq(CommissionSubmission::getId, submissionId)
                .eq(CommissionSubmission::getSubmitterId, userId));
        if (submission == null) {
            throw new BusinessException(CommissionErrorCode.SUBMISSION_NOT_FOUND);
        }
        CommissionTask task = findTask(submission.getTaskId());
        if (submission.getStatus() != SUBMISSION_STATUS_SUBMITTED || task.getStatus() != SUBMISSION
                || !LocalDateTime.now().isBefore(task.getDeadlineAt())) {
            throw new BusinessException(CommissionErrorCode.SUBMISSION_NOT_WITHDRAWABLE);
        }
        submission.setStatus(SUBMISSION_STATUS_WITHDRAWN);
        submission.setWithdrawnAt(LocalDateTime.now());
        submissionMapper.updateById(submission);
    }

    @Override
    public IPage<CommissionSubmissionMineVO> mySubmissions(Long userId, int page, int pageSize) {
        IPage<CommissionSubmission> submissionPage = submissionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<CommissionSubmission>()
                        .eq(CommissionSubmission::getSubmitterId, userId)
                        .orderByDesc(CommissionSubmission::getCreatedAt));

        List<CommissionSubmission> records = submissionPage.getRecords();
        if (records.isEmpty()) {
            return new Page<>(submissionPage.getCurrent(), submissionPage.getSize(), submissionPage.getTotal());
        }

        List<Long> taskIds = records.stream()
                .map(CommissionSubmission::getTaskId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> titleMap = taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(CommissionTask::getId, CommissionTask::getTitle));

        List<CommissionSubmissionMineVO> voList = records.stream()
                .map(s -> {
                    CommissionSubmissionMineVO vo = new CommissionSubmissionMineVO();
                    vo.setId(s.getId());
                    vo.setTaskId(s.getTaskId());
                    vo.setTitle(titleMap.get(s.getTaskId()));
                    vo.setArticleTitle(s.getArticleTitle());
                    vo.setArticleBizNo(s.getArticleBizNo());
                    vo.setWordCount(s.getWordCount());
                    vo.setStatus(s.getStatus());
                    vo.setRewardCoin(s.getRewardCoin());
                    vo.setCreatedAt(s.getCreatedAt());
                    return vo;
                })
                .collect(Collectors.toList());

        IPage<CommissionSubmissionMineVO> result = new Page<>(submissionPage.getCurrent(), submissionPage.getSize(), submissionPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public CommissionStatsVO stats(Long userId) {
        reconcilePhases();
        Long activeTaskCount = taskMapper.selectCount(new LambdaQueryWrapper<CommissionTask>()
                .eq(CommissionTask::getIsDeleted, 0)
                .eq(CommissionTask::getStatus, SUBMISSION));
        Long mySubmissionCount = submissionMapper.selectCount(new LambdaQueryWrapper<CommissionSubmission>()
                .eq(CommissionSubmission::getSubmitterId, userId));
        BigDecimal earnedCoinTotal = submissionMapper.selectSumRewardCoinBySubmitterId(userId);
        CommissionStatsVO vo = new CommissionStatsVO();
        vo.setActiveTaskCount(activeTaskCount);
        vo.setMySubmissionCount(mySubmissionCount);
        vo.setEarnedCoinTotal(earnedCoinTotal);
        return vo;
    }

    private Map<Long, Long> countSubmissionsByTaskIds(List<CommissionTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> taskIds = tasks.stream()
                .map(CommissionTask::getId)
                .distinct()
                .collect(Collectors.toList());
        return submissionMapper.selectCountGroupByTaskId(taskIds).stream()
                .collect(Collectors.toMap(
                        m -> Long.valueOf(m.get("taskId").toString()),
                        m -> Long.valueOf(m.get("cnt").toString()),
                        (a, b) -> a));
    }

    private CommissionTaskVO toTaskVO(CommissionTask task, long submissionCount) {
        CommissionTaskVO vo = new CommissionTaskVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setMinWordCount(task.getMinWordCount());
        vo.setMaxWordCount(task.getMaxWordCount());
        vo.setSkillHint(task.getSkillHint());
        vo.setRewardCoin(task.getRewardCoin());
        vo.setNeededCount(task.getNeededCount());
        vo.setAdoptedCount(task.getAdoptedCount());
        vo.setStatus(task.getStatus());
        vo.setDeadlineAt(task.getDeadlineAt());
        vo.setSelectionDeadlineAt(task.getSelectionDeadlineAt());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setSubmissionCount(submissionCount);
        return vo;
    }

    private void reconcilePhases() {
        LocalDateTime now = LocalDateTime.now();
        taskMapper.update(null, new LambdaUpdateWrapper<CommissionTask>()
                .set(CommissionTask::getStatus, REVIEW)
                .eq(CommissionTask::getStatus, SUBMISSION)
                .le(CommissionTask::getDeadlineAt, now));
        taskMapper.update(null, new LambdaUpdateWrapper<CommissionTask>()
                .set(CommissionTask::getStatus, 2)
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
                .set(CommissionTask::getStatus, 2)
                .eq(CommissionTask::getId, taskId)
                .eq(CommissionTask::getStatus, REVIEW)
                .le(CommissionTask::getSelectionDeadlineAt, now));
    }

    private CommissionTask findTask(Long taskId) {
        CommissionTask task = taskMapper.selectById(taskId);
        if (task == null || Integer.valueOf(1).equals(task.getIsDeleted())) {
            throw new BusinessException(CommissionErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }
}
