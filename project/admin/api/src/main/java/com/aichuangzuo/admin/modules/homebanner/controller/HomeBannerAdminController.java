package com.aichuangzuo.admin.modules.homebanner.controller;

import com.aichuangzuo.admin.modules.homebanner.dto.request.HomeBannerReq;
import com.aichuangzuo.admin.modules.homebanner.service.HomeBannerService;
import com.aichuangzuo.admin.modules.homebanner.vo.HomeBannerVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "首页 Banner 管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/home-banner")
@RequiredArgsConstructor
public class HomeBannerAdminController {

    private final HomeBannerService service;

    @Operation(summary = "Banner 列表")
    @GetMapping
    public Result<List<HomeBannerVO>> list() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询首页 Banner 列表, adminUserId={}", adminUserId);
        return Result.success(service.list());
    }

    @Operation(summary = "新增 Banner")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody HomeBannerReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员新增首页 Banner, adminUserId={}, linkUrl={}", adminUserId, req.getLinkUrl());
        return Result.success(service.create(req));
    }

    @Operation(summary = "更新 Banner")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody HomeBannerReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新首页 Banner, adminUserId={}, bannerId={}, linkUrl={}",
                adminUserId, id, req.getLinkUrl());
        service.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除 Banner")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员删除首页 Banner, adminUserId={}, bannerId={}", adminUserId, id);
        service.delete(id);
        return Result.success();
    }
}
