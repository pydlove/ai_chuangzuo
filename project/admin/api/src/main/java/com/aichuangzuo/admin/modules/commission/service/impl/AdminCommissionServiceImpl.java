package com.aichuangzuo.admin.modules.commission.service.impl;

import com.aichuangzuo.admin.modules.commission.dto.request.CommissionSubmissionBatchCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionSubmissionCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskUpdateRequest;
import com.aichuangzuo.admin.modules.commission.dto.excel.CommissionTaskExcelRowData;
import com.aichuangzuo.admin.modules.commission.entity.CommissionSubmission;
import com.aichuangzuo.admin.modules.commission.entity.CommissionTask;
import com.aichuangzuo.admin.modules.commission.enums.AdminCommissionErrorCode;
import com.aichuangzuo.admin.modules.commission.mapper.CommissionSubmissionMapper;
import com.aichuangzuo.admin.modules.commission.mapper.CommissionTaskMapper;
import com.aichuangzuo.admin.modules.commission.service.AdminCommissionService;
import com.aichuangzuo.admin.modules.commission.util.CommissionExcelImportUtil;
import com.aichuangzuo.admin.modules.commission.vo.CommissionSubmissionVO;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskDetailVO;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskImportResultVO;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskImportRowErrorVO;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskListVO;
import com.aichuangzuo.admin.modules.leaderboard.client.UserApiClient;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCommissionServiceImpl implements AdminCommissionService {
    private static final int SUBMISSION = 0;
    private static final int REVIEW = 1;
    private static final int COMPLETED = 2;
    private static final int SUBMISSION_STATUS_SUBMITTED = 0;
    private static final int SUBMISSION_STATUS_ADOPTED = 1;
    private static final int SUBMISSION_STATUS_NOT_ADOPTED = 2;

    private final CommissionTaskMapper taskMapper;
    private final CommissionSubmissionMapper submissionMapper;
    private final UserApiClient userApiClient;
    private final PlatformUserMapper platformUserMapper;

    @Override
    public IPage<CommissionTaskListVO> list(String keyword, Integer status, int page, int pageSize) {
        LambdaQueryWrapper<CommissionTask> query = new LambdaQueryWrapper<CommissionTask>()
                .eq(CommissionTask::getIsDeleted, 0)
                .eq(status != null, CommissionTask::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(CommissionTask::getTitle, keyword)
                        .or().like(CommissionTask::getTaskNo, keyword))
                .orderByDesc(CommissionTask::getCreatedAt);
        IPage<CommissionTask> taskPage = taskMapper.selectPage(new Page<>(page, pageSize), query);
        List<Long> taskIds = taskPage.getRecords().stream().map(CommissionTask::getId).toList();
        Map<Long, long[]> countMap = taskIds.isEmpty() ? Map.of() : submissionMapper.countByTaskIds(taskIds).stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("taskId")).longValue(),
                        m -> new long[]{((Number) m.get("totalCount")).longValue(), ((Number) m.get("manualCount")).longValue()},
                        (a, b) -> a));
        List<CommissionTaskListVO> records = taskPage.getRecords().stream().map(t -> {
            long[] counts = countMap.getOrDefault(t.getId(), new long[]{0L, 0L});
            return new CommissionTaskListVO(
                    t.getId(), t.getTaskNo(), t.getTitle(), t.getDescription(), t.getMinWordCount(), t.getMaxWordCount(),
                    t.getSkillHint(), t.getRewardCoin(), t.getNeededCount(), t.getAdoptedCount(),
                    t.getStatus(), t.getDeadlineAt(), t.getSelectionDeadlineAt(), t.getCompletedAt(),
                    t.getPublishedBy(), t.getCreatedAt(), counts[0], counts[1]);
        }).toList();
        IPage<CommissionTaskListVO> result = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public CommissionTaskDetailVO detail(Long taskId) {
        CommissionTask task = findTask(taskId);
        List<CommissionSubmission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<CommissionSubmission>()
                        .eq(CommissionSubmission::getTaskId, taskId)
                        .eq(CommissionSubmission::getIsDeleted, 0)
                        .orderByDesc(CommissionSubmission::getCreatedAt));
        List<Long> submitterIds = submissions.stream().map(CommissionSubmission::getSubmitterId).distinct().toList();
        Map<Long, PlatformUser> userMap = submitterIds.isEmpty() ? Map.of() : platformUserMapper.selectList(
                        new LambdaQueryWrapper<PlatformUser>().in(PlatformUser::getId, submitterIds))
                .stream().collect(Collectors.toMap(PlatformUser::getId, u -> u));
        List<CommissionSubmissionVO> voList = submissions.stream().map(s -> {
            PlatformUser user = userMap.get(s.getSubmitterId());
            return new CommissionSubmissionVO(
                    s.getId(), s.getTaskId(), s.getSubmitterId(),
                    user != null ? user.getNickname() : null,
                    user != null ? user.getEmail() : null,
                    s.getArticleBizNo(), s.getArticleTitle(), s.getArticleBody(),
                    s.getWordCount(), s.getStatus(), s.getRewardCoin(),
                    s.getAdoptedAt(), s.getCreatedAt());
        }).toList();
        return new CommissionTaskDetailVO(task, voList);
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
        task.setSkillHint(request.getSkillHint());
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
        task.setSkillHint(request.getSkillHint());
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
    public Long createSubmission(Long taskId, CommissionSubmissionCreateRequest request, Long adminId) {
        CommissionTask task = taskMapper.selectByIdForUpdate(taskId);
        ensureTask(task);
        if (task.getStatus() != SUBMISSION && task.getStatus() != REVIEW) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        if (task.getStatus() == SUBMISSION && !LocalDateTime.now().isBefore(task.getDeadlineAt())) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        if (task.getStatus() == REVIEW && !LocalDateTime.now().isBefore(task.getSelectionDeadlineAt())) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        Integer wordCount = request.getWordCount();
        if (wordCount != null && (wordCount < task.getMinWordCount() || wordCount > task.getMaxWordCount())) {
            throw new BusinessException(AdminCommissionErrorCode.PARAM_INVALID);
        }

        PlatformUser user = platformUserMapper.selectById(request.getSubmitterId());
        if (user == null) {
            throw new BusinessException(AdminCommissionErrorCode.SUBMISSION_USER_NOT_FOUND);
        }

        Long existing = submissionMapper.selectCount(
                new LambdaQueryWrapper<CommissionSubmission>()
                        .eq(CommissionSubmission::getTaskId, taskId)
                        .eq(CommissionSubmission::getSubmitterId, request.getSubmitterId())
                        .eq(CommissionSubmission::getIsDeleted, 0));
        if (existing != null && existing > 0) {
            throw new BusinessException(AdminCommissionErrorCode.SUBMISSION_ALREADY_EXISTS);
        }

        CommissionSubmission submission = new CommissionSubmission();
        submission.setTaskId(taskId);
        submission.setSubmitterId(request.getSubmitterId());
        submission.setArticleBizNo("MANUAL:" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        submission.setArticleTitle(request.getArticleTitle() == null ? "" : request.getArticleTitle().trim());
        submission.setArticleBody(request.getArticleBody() == null ? "" : request.getArticleBody().trim());
        submission.setWordCount(request.getWordCount() == null ? 0 : request.getWordCount());
        submission.setStatus(SUBMISSION_STATUS_SUBMITTED);
        submission.setTenantId(0L);
        submissionMapper.insert(submission);
        return submission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createSubmissionBatch(Long taskId, CommissionSubmissionBatchCreateRequest request, Long adminId) {
        CommissionTask task = taskMapper.selectByIdForUpdate(taskId);
        ensureTask(task);
        if (task.getStatus() != SUBMISSION && task.getStatus() != REVIEW) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        if (task.getStatus() == SUBMISSION && !LocalDateTime.now().isBefore(task.getDeadlineAt())) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        if (task.getStatus() == REVIEW && !LocalDateTime.now().isBefore(task.getSelectionDeadlineAt())) {
            throw new BusinessException(AdminCommissionErrorCode.TASK_STATUS_INVALID);
        }
        Integer wordCount = request.getWordCount();
        if (wordCount != null && (wordCount < task.getMinWordCount() || wordCount > task.getMaxWordCount())) {
            throw new BusinessException(AdminCommissionErrorCode.PARAM_INVALID);
        }

        List<Long> submitterIds = request.getSubmitterIds();
        List<PlatformUser> users = platformUserMapper.selectList(
                new LambdaQueryWrapper<PlatformUser>().in(PlatformUser::getId, submitterIds));
        if (users.size() != submitterIds.size()) {
            throw new BusinessException(AdminCommissionErrorCode.SUBMISSION_USER_NOT_FOUND);
        }

        List<Long> existingSubmitterIds = submissionMapper.selectList(
                        new LambdaQueryWrapper<CommissionSubmission>()
                                .eq(CommissionSubmission::getTaskId, taskId)
                                .in(CommissionSubmission::getSubmitterId, submitterIds)
                                .eq(CommissionSubmission::getIsDeleted, 0))
                .stream().map(CommissionSubmission::getSubmitterId).toList();
        if (!existingSubmitterIds.isEmpty()) {
            throw new BusinessException(AdminCommissionErrorCode.SUBMISSION_ALREADY_EXISTS);
        }

        String title = request.getArticleTitle() == null ? "" : request.getArticleTitle().trim();
        String body = request.getArticleBody() == null ? "" : request.getArticleBody().trim();
        int count = 0;
        for (Long submitterId : submitterIds) {
            CommissionSubmission submission = new CommissionSubmission();
            submission.setTaskId(taskId);
            submission.setSubmitterId(submitterId);
            submission.setArticleBizNo("MANUAL:" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
            submission.setArticleTitle(title);
            submission.setArticleBody(body);
            submission.setWordCount(wordCount == null ? 0 : wordCount);
            submission.setStatus(SUBMISSION_STATUS_SUBMITTED);
            submission.setTenantId(0L);
            submissionMapper.insert(submission);
            count++;
        }
        return count;
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

    @Override
    public int reconcileTaskStatus() {
        LocalDateTime now = LocalDateTime.now();
        int toReview = taskMapper.update(null, new LambdaUpdateWrapper<CommissionTask>()
                .set(CommissionTask::getStatus, REVIEW)
                .eq(CommissionTask::getStatus, SUBMISSION)
                .le(CommissionTask::getDeadlineAt, now));
        int toCompleted = taskMapper.update(null, new LambdaUpdateWrapper<CommissionTask>()
                .set(CommissionTask::getStatus, COMPLETED)
                .set(CommissionTask::getCompletedAt, now)
                .eq(CommissionTask::getStatus, REVIEW)
                .le(CommissionTask::getSelectionDeadlineAt, now));
        return toReview + toCompleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommissionTaskImportResultVO importExcel(MultipartFile file, Long adminId) {
        List<CommissionTaskExcelRowData> rows = CommissionExcelImportUtil.readRows(file);
        List<CommissionTaskImportRowErrorVO> errors = new ArrayList<>();
        List<CommissionTask> tasks = new ArrayList<>(rows.size());
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < rows.size(); i++) {
            CommissionTaskExcelRowData row = rows.get(i);
            int rowIndex = i + 2;
            List<String> rowErrors = new ArrayList<>();
            CommissionTask task = validateAndBuildTask(row, rowIndex, rowErrors, adminId, now);
            if (!rowErrors.isEmpty()) {
                errors.add(new CommissionTaskImportRowErrorVO(rowIndex, row.getTitle(), rowErrors));
            } else {
                tasks.add(task);
            }
        }

        if (!errors.isEmpty()) {
            return new CommissionTaskImportResultVO(false, rows.size(), 0, errors);
        }

        taskMapper.batchInsert(tasks);
        return new CommissionTaskImportResultVO(true, rows.size(), tasks.size(), List.of());
    }

    private CommissionTask validateAndBuildTask(CommissionTaskExcelRowData row, int rowIndex,
                                                List<String> errors, Long adminId, LocalDateTime now) {
        String title = trim(row.getTitle());
        String description = trim(row.getDescription());
        String skillHint = trim(row.getSkillHint());

        if (title == null || title.isEmpty()) {
            errors.add("【任务标题】未填写，请输入任务标题");
        } else if (title.length() > 128) {
            errors.add("【任务标题】长度超过128字符，当前 " + title.length() + " 字符");
        }

        if (description == null || description.isEmpty()) {
            errors.add("【需求描述】未填写，请输入稿件需求描述");
        }

        Integer minWordCount = parsePositiveInt(row.getMinWordCount(), "最小字数", errors);
        Integer maxWordCount = parsePositiveInt(row.getMaxWordCount(), "最大字数", errors);
        if (minWordCount != null && maxWordCount != null) {
            if (minWordCount > maxWordCount) {
                errors.add("【字数范围】最小字数（" + minWordCount + "）不能大于最大字数（" + maxWordCount + "）");
            }
        }

        if (skillHint != null && skillHint.length() > 128) {
            errors.add("【风格提示】长度超过128字符，当前 " + skillHint.length() + " 字符");
        }

        BigDecimal rewardCoin = parseRewardCoin(row.getRewardCoin(), errors);
        Integer neededCount = parsePositiveInt(row.getNeededCount(), "需采纳数量", errors);
        LocalDateTime deadlineAt = parseDateTime(row.getDeadlineAt(), "投递截止时间", errors);
        LocalDateTime selectionDeadlineAt = parseDateTime(row.getSelectionDeadlineAt(), "评选截止时间", errors);

        if (deadlineAt != null && selectionDeadlineAt != null) {
            if (!selectionDeadlineAt.isAfter(deadlineAt)) {
                errors.add("【评选截止时间】必须晚于【投递截止时间】");
            }
            if (!deadlineAt.isAfter(now)) {
                errors.add("【投递截止时间】必须晚于当前时间");
            }
            if (!selectionDeadlineAt.isAfter(now)) {
                errors.add("【评选截止时间】必须晚于当前时间");
            }
        }

        if (!errors.isEmpty()) {
            return null;
        }

        CommissionTask task = new CommissionTask();
        task.setTaskNo("CT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        task.setTitle(title);
        task.setDescription(description);
        task.setMinWordCount(minWordCount);
        task.setMaxWordCount(maxWordCount);
        task.setSkillHint(skillHint);
        task.setRewardCoin(rewardCoin);
        task.setNeededCount(neededCount);
        task.setAdoptedCount(0);
        task.setStatus(SUBMISSION);
        task.setDeadlineAt(deadlineAt);
        task.setSelectionDeadlineAt(selectionDeadlineAt);
        task.setPublishedBy(adminId);
        task.setTenantId(0L);
        task.setIsDeleted(0);
        task.setCreatedBy(adminId);
        task.setUpdatedBy(adminId);
        return task;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private Integer parsePositiveInt(String value, String fieldName, List<String> errors) {
        if (value == null || value.trim().isEmpty()) {
            errors.add("【" + fieldName + "】未填写，请输入正整数");
            return null;
        }
        String text = value.trim();
        try {
            int number = Integer.parseInt(text);
            if (number < 1) {
                errors.add("【" + fieldName + "】必须大于等于1，当前为 " + text);
                return null;
            }
            return number;
        } catch (NumberFormatException e) {
            errors.add("【" + fieldName + "】格式错误（" + text + "），请输入正整数");
            return null;
        }
    }

    private BigDecimal parseRewardCoin(String value, List<String> errors) {
        if (value == null || value.trim().isEmpty()) {
            errors.add("【每篇奖励/创作币】未填写，请输入数值（至少5）");
            return null;
        }
        String text = value.trim();
        try {
            BigDecimal coin = new BigDecimal(text);
            if (coin.compareTo(new BigDecimal("5")) < 0) {
                errors.add("【每篇奖励/创作币】不能小于5，当前为 " + text);
                return null;
            }
            return coin;
        } catch (NumberFormatException e) {
            errors.add("【每篇奖励/创作币】格式错误（" + text + "），请输入有效数值");
            return null;
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime parseDateTime(String value, String fieldName, List<String> errors) {
        if (value == null || value.trim().isEmpty()) {
            errors.add("【" + fieldName + "】未填写，支持格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() <= 10) {
                LocalDate date = LocalDate.parse(text, DATE_FORMATTER);
                return LocalDateTime.of(date, LocalTime.MAX);
            }
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            errors.add("【" + fieldName + "】格式错误（" + text + "），支持：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
            return null;
        }
    }

    private void validateDeadlines(LocalDateTime deadlineAt, LocalDateTime selectionDeadlineAt, Integer minWordCount, Integer maxWordCount) {
        if (minWordCount > maxWordCount) {
            throw new BusinessException(AdminCommissionErrorCode.PARAM_INVALID);
        }
        if (!selectionDeadlineAt.isAfter(deadlineAt)) {
            throw new BusinessException(AdminCommissionErrorCode.PARAM_INVALID);
        }
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