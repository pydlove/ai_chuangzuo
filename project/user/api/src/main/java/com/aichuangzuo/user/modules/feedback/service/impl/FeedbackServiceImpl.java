package com.aichuangzuo.user.modules.feedback.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;
import com.aichuangzuo.user.modules.feedback.entity.Feedback;
import com.aichuangzuo.user.modules.feedback.enums.FeedbackErrorCode;
import com.aichuangzuo.user.modules.feedback.enums.FeedbackType;
import com.aichuangzuo.user.modules.feedback.mapper.FeedbackMapper;
import com.aichuangzuo.user.modules.feedback.service.FeedbackService;
import com.aichuangzuo.user.modules.feedback.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private static final int DAILY_LIMIT = 5;

    private final FeedbackMapper feedbackMapper;

    @Override
    public Long submit(Long userId, SubmitFeedbackRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException(FeedbackErrorCode.CONTENT_REQUIRED);
        }
        if (!FeedbackType.isValid(request.getType())) {
            request.setType(FeedbackType.其他.name());
        }

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long recent = feedbackMapper.countRecentByUser(userId, since);
        if (recent >= DAILY_LIMIT) {
            throw new BusinessException(FeedbackErrorCode.DAILY_LIMIT_EXCEEDED);
        }

        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setType(request.getType());
        fb.setContent(request.getContent());
        fb.setStatus(0);
        fb.setTenantId(0L);
        fb.setCreatedBy(userId);
        fb.setUpdatedBy(userId);
        feedbackMapper.insert(fb);
        SecurityUserContext.clear();
        return fb.getId();
    }

    @Override
    public List<FeedbackVO> pageByUser(Long userId, Integer status, int page, int size) {
        int safeSize = Math.min(Math.max(1, size), 100);
        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * safeSize;
        List<Feedback> rows = feedbackMapper.pageByUser(userId, status, offset, safeSize);
        return rows.stream().map(this::toVO).toList();
    }

    @Override
    public long countByUser(Long userId, Integer status) {
        return feedbackMapper.countByUser(userId, status);
    }

    private FeedbackVO toVO(Feedback fb) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(fb.getId());
        vo.setType(fb.getType());
        vo.setContent(fb.getContent());
        vo.setStatus(fb.getStatus());
        vo.setReplyContent(fb.getReplyContent());
        vo.setRepliedAt(fb.getRepliedAt());
        vo.setCreatedAt(fb.getCreatedAt());
        return vo;
    }
}
