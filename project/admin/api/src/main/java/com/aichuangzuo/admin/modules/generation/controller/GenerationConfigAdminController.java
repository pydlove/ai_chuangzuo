package com.aichuangzuo.admin.modules.generation.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.generation.dto.request.GenerationConfigUpdateRequest;
import com.aichuangzuo.admin.modules.generation.service.GenerationConfigService;
import com.aichuangzuo.admin.modules.generation.vo.GenerationConfigVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 端-创作运行时配置 API。
 */
@Tag(name = "管理端-创作设置")
@RestController
@RequestMapping("/api/v1/admin/generation/config")
@RequiredArgsConstructor
public class GenerationConfigAdminController {

    private final GenerationConfigService service;

    @GetMapping
    public Result<GenerationConfigVO> detail() {
        return Result.success(service.detail());
    }

    @PutMapping
    public Result<GenerationConfigVO> update(@Valid @RequestBody GenerationConfigUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        return Result.success(service.update(request, adminUserId));
    }
}
