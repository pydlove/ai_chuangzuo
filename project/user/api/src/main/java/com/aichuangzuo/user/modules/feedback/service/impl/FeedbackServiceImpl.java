package com.aichuangzuo.user.modules.feedback.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;
import com.aichuangzuo.user.modules.feedback.entity.Feedback;
import com.aichuangzuo.user.modules.feedback.enums.FeedbackErrorCode;
import com.aichuangzuo.user.modules.feedback.enums.FeedbackType;
import com.aichuangzuo.user.modules.feedback.mapper.FeedbackMapper;
import com.aichuangzuo.user.modules.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
}
