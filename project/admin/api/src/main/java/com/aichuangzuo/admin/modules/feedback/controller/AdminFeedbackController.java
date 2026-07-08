package com.aichuangzuo.admin.modules.feedback.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.feedback.dto.request.AdminReplyFeedbackRequest;
import com.aichuangzuo.admin.modules.feedback.service.AdminFeedbackService;
import com.aichuangzuo.admin.modules.feedback.vo.AdminFeedbackVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "管理端-用户反馈")
@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final AdminFeedbackService feedbackService;

    @Operation(summary = "分页列表（status=0 待回复，1 已回复，null 全部）")
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size) {
        List<AdminFeedbackVO> rows = feedbackService.page(status, page, size);
        long total = feedbackService.count(status);
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Operation(summary = "反馈详情")
    @GetMapping("/{id}")
    public Result<AdminFeedbackVO> detail(@PathVariable("id") Long id) {
        return Result.success(feedbackService.detail(id));
    }

    @Operation(summary = "回复反馈")
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable("id") Long id,
                              @Valid @RequestBody AdminReplyFeedbackRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        feedbackService.reply(id, adminId, request.getContent());
        return Result.success();
    }
}
