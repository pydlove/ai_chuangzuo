package com.aichuangzuo.admin.modules.homebanner.controller;

import com.aichuangzuo.admin.modules.homebanner.dto.request.HomeBannerReq;
import com.aichuangzuo.admin.modules.homebanner.service.HomeBannerService;
import com.aichuangzuo.admin.modules.homebanner.vo.HomeBannerVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "首页 Banner 管理")
@RestController
@RequestMapping("/api/v1/admin/home-banner")
@RequiredArgsConstructor
public class HomeBannerAdminController {

    private final HomeBannerService service;

    @Operation(summary = "Banner 列表")
    @GetMapping
    public Result<List<HomeBannerVO>> list() {
        return Result.success(service.list());
    }

    @Operation(summary = "新增 Banner")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody HomeBannerReq req) {
        return Result.success(service.create(req));
    }

    @Operation(summary = "更新 Banner")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody HomeBannerReq req) {
        service.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除 Banner")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }
}
