package com.aichuangzuo.admin.modules.article.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.article.service.AdminArticleService;
import com.aichuangzuo.admin.modules.article.vo.AdminArticleDetailVO;
import com.aichuangzuo.admin.modules.article.vo.AdminArticlePageVO;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端用户作品")
@RestController
@RequestMapping("/api/v1/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final AdminArticleService adminArticleService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询用户作品列表")
    @GetMapping
    public Result<AdminArticlePageVO> listUserArticles(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        checkSuperAdmin();
        return Result.success(adminArticleService.listUserArticles(userId, keyword, page, pageSize));
    }

    @Operation(summary = "查询作品详情")
    @GetMapping("/{bizNo}")
    public Result<AdminArticleDetailVO> getArticleDetail(@PathVariable(name = "bizNo") String bizNo) {
        checkSuperAdmin();
        return Result.success(adminArticleService.getArticleDetail(bizNo));
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
