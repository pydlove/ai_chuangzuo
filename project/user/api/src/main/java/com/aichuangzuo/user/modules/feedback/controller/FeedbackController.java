package com.aichuangzuo.user.modules.feedback.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;
import com.aichuangzuo.user.modules.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-意见反馈")
@RestController
@RequestMapping("/api/v1/user/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "提交反馈")
    @PostMapping("/submit")
    public Result<Long> submit(@Valid @RequestBody SubmitFeedbackRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        Long id = feedbackService.submit(userId, request);
        return Result.success(id);
    }
}
