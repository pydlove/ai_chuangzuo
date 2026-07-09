package com.aichuangzuo.user.modules.feedback.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;
import com.aichuangzuo.user.modules.feedback.service.FeedbackService;
import com.aichuangzuo.user.modules.feedback.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Operation(summary = "我的反馈历史")
    @GetMapping("/mine")
    public Result<Map<String, Object>> mine(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUserContext.getCurrentUserId();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        List<FeedbackVO> rows = feedbackService.pageByUser(userId, status, safePage, safeSize);
        long total = feedbackService.countByUser(userId, status);
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("total", total);
        data.put("page", safePage);
        data.put("size", safeSize);
        return Result.success(data);
    }
}
