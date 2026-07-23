package com.aichuangzuo.admin.modules.style.review.controller;

import com.aichuangzuo.admin.modules.style.review.dto.request.BatchApproveStyleReviewRequest;
import com.aichuangzuo.admin.modules.style.review.dto.request.RejectStyleReviewRequest;
import com.aichuangzuo.admin.modules.style.review.dto.request.StyleReviewPageRequest;
import com.aichuangzuo.admin.modules.style.review.service.StyleReviewService;
import com.aichuangzuo.admin.modules.style.review.vo.StyleReviewVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 风格审核接口。
 */
@Tag(name = "管理端 - 风格审核")
@RestController
@RequestMapping("/api/v1/admin/style-reviews")
@RequiredArgsConstructor
public class StyleReviewAdminController {

    private final StyleReviewService styleReviewService;

    @Operation(summary = "审核列表（分页）")
    @GetMapping
    public Result<IPage<StyleReviewVO>> page(StyleReviewPageRequest request) {
        return Result.success(styleReviewService.page(request));
    }

    @Operation(summary = "通过风格")
    @PostMapping("/{bizNo}/actions/approve")
    public Result<Void> approve(@PathVariable String bizNo) {
        styleReviewService.approve(bizNo);
        return Result.success();
    }

    @Operation(summary = "批量通过风格")
    @PostMapping("/actions/batch-approve")
    public Result<Integer> batchApprove(@Valid @RequestBody BatchApproveStyleReviewRequest request) {
        int count = styleReviewService.batchApprove(request.getBizNos());
        return Result.success(count);
    }

    @Operation(summary = "打回风格")
    @PostMapping("/{bizNo}/actions/reject")
    public Result<Void> reject(@PathVariable String bizNo,
                                @Valid @RequestBody RejectStyleReviewRequest request) {
        styleReviewService.reject(bizNo, request.getReason());
        return Result.success();
    }
}