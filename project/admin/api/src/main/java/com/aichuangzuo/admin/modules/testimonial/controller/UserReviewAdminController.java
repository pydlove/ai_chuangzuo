package com.aichuangzuo.admin.modules.testimonial.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.testimonial.dto.request.UserReviewPageRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.UserReviewStatusRequest;
import com.aichuangzuo.admin.modules.testimonial.service.UserReviewService;
import com.aichuangzuo.admin.modules.testimonial.vo.UserReviewVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-用户评价")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/testimonials/user-reviews")
@RequiredArgsConstructor
public class UserReviewAdminController {

    private final UserReviewService userReviewService;

    @Operation(summary = "用户评价列表")
    @GetMapping
    public Result<IPage<UserReviewVO>> page(UserReviewPageRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询用户评价列表, adminUserId={}, keyword={}, pageNum={}, pageSize={}",
                adminUserId, request.getKeyword(), request.getPageNum(), request.getPageSize());
        return Result.success(userReviewService.page(request));
    }

    @Operation(summary = "设置用户评价是否展示到首页")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UserReviewStatusRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新用户评价展示状态, adminUserId={}, reviewId={}, isShowOnHomepage={}",
                adminUserId, id, request.getIsShowOnHomepage());
        userReviewService.updateStatus(id, request);
        return Result.success();
    }
}
