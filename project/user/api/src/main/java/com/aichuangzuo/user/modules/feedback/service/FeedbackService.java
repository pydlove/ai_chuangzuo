package com.aichuangzuo.user.modules.feedback.service;

import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;
import com.aichuangzuo.user.modules.feedback.vo.FeedbackVO;

import java.util.List;

public interface FeedbackService {
    Long submit(Long userId, SubmitFeedbackRequest request);

    List<FeedbackVO> pageByUser(Long userId, Integer status, int page, int size);

    long countByUser(Long userId, Integer status);
}
