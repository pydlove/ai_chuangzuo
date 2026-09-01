package com.aichuangzuo.admin.modules.experience.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.experience.dto.request.ExperienceTokenBatchGenerateRequest;
import com.aichuangzuo.admin.modules.experience.dto.request.ExperienceTokenQueryRequest;
import com.aichuangzuo.admin.modules.experience.service.ExperienceTokenAdminService;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端-体验会员管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/experience-tokens")
@RequiredArgsConstructor
public class ExperienceTokenAdminController {

    private final ExperienceTokenAdminService experienceTokenAdminService;
    private final AdminUserPermissionService permissionService;

    @PostMapping("/batch-generate")
    public Result<List<String>> batchGenerate(@Valid @RequestBody ExperienceTokenBatchGenerateRequest request) {
        Long adminId = checkSuperAdmin();
        return Result.success(experienceTokenAdminService.batchGenerate(request, adminId));
    }

    @GetMapping
    public Result<ExperienceTokenAdminService.PageResult> list(@ModelAttribute ExperienceTokenQueryRequest request) {
        Long adminId = checkSuperAdmin();
        log.info("管理员查询体验令牌列表, adminId={}, batchId={}, status={}",
                adminId, request.getBatchId(), request.getStatus());
        return Result.success(experienceTokenAdminService.list(request));
    }

    private Long checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminId;
    }
}
