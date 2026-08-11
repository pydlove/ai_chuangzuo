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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端用户作品")
@Slf4j
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
        Long adminUserId = checkSuperAdmin();
        log.info("管理端查询用户作品列表, adminUserId={}, userId={}, keyword={}, page={}, pageSize={}",
                adminUserId, userId, keyword, page, pageSize);
        return Result.success(adminArticleService.listUserArticles(userId, keyword, page, pageSize));
    }

    @Operation(summary = "查询作品详情")
    @GetMapping("/{bizNo}")
    public Result<AdminArticleDetailVO> getArticleDetail(@PathVariable(name = "bizNo") String bizNo) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理端查询作品详情, adminUserId={}, bizNo={}", adminUserId, bizNo);
        return Result.success(adminArticleService.getArticleDetail(bizNo));
    }

    private Long checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            log.warn("管理端作品管理权限校验失败, adminUserId={}", adminUserId);
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminUserId;
    }
}
