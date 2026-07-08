package com.aichuangzuo.user.modules.feedback.service;

import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;

public interface FeedbackService {
    Long submit(Long userId, SubmitFeedbackRequest request);
}
