package com.aichuangzuo.user.modules.leaderboard.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.storage.LocalFileStorage;
import com.aichuangzuo.user.modules.leaderboard.dto.request.IncomeSubmissionUploadRequest;
import com.aichuangzuo.user.modules.leaderboard.entity.IncomeSubmission;
import com.aichuangzuo.user.modules.leaderboard.entity.SubmissionStatus;
import com.aichuangzuo.user.modules.leaderboard.enums.LeaderboardErrorCode;
import com.aichuangzuo.user.modules.leaderboard.mapper.IncomeSubmissionMapper;
import com.aichuangzuo.user.modules.leaderboard.service.IncomeSubmissionService;
import com.aichuangzuo.user.modules.leaderboard.vo.IncomeSubmissionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 自媒体收入申报服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncomeSubmissionServiceImpl implements IncomeSubmissionService {

    private static final String BIZ_NO_PREFIX = "IS";
    private static final String PERIOD_PATTERN = "^\\d{4}-\\d{2}$";

    private final IncomeSubmissionMapper submissionMapper;
    private final LocalFileStorage localFileStorage;
    private final ObjectMapper objectMapper;

    @Override
    @CacheEvict(value = "leaderboard", key = "'income:month:' + #p1.periodMonth + ':' + #p0")
    public IncomeSubmissionVO submit(Long userId, IncomeSubmissionUploadRequest request) {
        validate(request);

        IncomeSubmission submission = new IncomeSubmission();
        submission.setBizNo(generateBizNo());
        submission.setUserId(userId);
        submission.setPeriodMonth(request.getPeriodMonth());
        submission.setAmount(request.getAmount());
        submission.setPlatform(request.getPlatform());
        submission.setScreenshotPaths(toJson(request.getScreenshotPaths()));
        submission.setAuditStatus(SubmissionStatus.PENDING.getCode());
        submission.setTenantId(0L);

        submissionMapper.insert(submission);
        log.info("收入申报提交 userId={}, bizNo={}, periodMonth={}", userId, submission.getBizNo(), submission.getPeriodMonth());
        return toVO(submission);
    }

    @Override
    public List<IncomeSubmissionVO> listByUser(Long userId, Integer status) {
        LambdaQueryWrapper<IncomeSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IncomeSubmission::getUserId, userId);
        if (status != null) {
            wrapper.eq(IncomeSubmission::getAuditStatus, status);
        }
        wrapper.orderByDesc(IncomeSubmission::getCreatedAt);
        return submissionMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public List<String> uploadScreenshots(Long userId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(LeaderboardErrorCode.INCOME_FILE_INVALID);
        }
        String batchId = "B" + System.nanoTime();
        return localFileStorage.store(userId, batchId, files);
    }

    private void validate(IncomeSubmissionUploadRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(LeaderboardErrorCode.INCOME_AMOUNT_INVALID);
        }
        if (request.getPeriodMonth() == null || !request.getPeriodMonth().matches(PERIOD_PATTERN)) {
            throw new BusinessException(LeaderboardErrorCode.INCOME_PERIOD_INVALID);
        }
        if (request.getScreenshotPaths() == null || request.getScreenshotPaths().isEmpty()) {
            throw new BusinessException(LeaderboardErrorCode.INCOME_SCREENSHOT_REQUIRED);
        }
    }

    private String generateBizNo() {
        return BIZ_NO_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String toJson(List<String> paths) {
        try {
            return objectMapper.writeValueAsString(paths);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("截图路径序列化失败", e);
        }
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("截图路径反序列化失败: {}", json);
            return Collections.emptyList();
        }
    }

    private IncomeSubmissionVO toVO(IncomeSubmission submission) {
        IncomeSubmissionVO vo = new IncomeSubmissionVO();
        vo.setBizNo(submission.getBizNo());
        vo.setPeriodMonth(submission.getPeriodMonth());
        vo.setAmount(submission.getAmount());
        vo.setPlatform(submission.getPlatform());
        vo.setAuditStatus(submission.getAuditStatus());
        vo.setRejectReason(submission.getRejectReason());
        vo.setScreenshotPaths(parseJson(submission.getScreenshotPaths()));
        vo.setCreatedAt(submission.getCreatedAt());
        return vo;
    }
}
