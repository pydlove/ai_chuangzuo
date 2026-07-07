package com.aichuangzuo.admin.modules.hotsearch.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchConfigRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyQueryRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchPlatformRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchConfig;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.job.HotSearchCrawlJob;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchConfigService;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchDailyAdminService;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchPlatformAdminService;
import com.aichuangzuo.admin.modules.hotsearch.vo.CrawlResultVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.HotSearchDailyAdminVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.LastRunVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端热搜管理")
@RestController
@RequestMapping("/api/v1/admin/hot-search")
@RequiredArgsConstructor
public class HotSearchAdminController {

    private final HotSearchPlatformAdminService platformService;
    private final HotSearchDailyAdminService dailyService;
    private final HotSearchConfigService configService;
    private final HotSearchCrawlJob crawlJob;
    private final AdminUserPermissionService permissionService;

    // ===== 平台 =====
    @GetMapping("/platforms")
    public Result<List<HotSearchPlatform>> listPlatforms() {
        checkSuperAdmin();
        return Result.success(platformService.listAll());
    }

    @PostMapping("/platforms")
    public Result<HotSearchPlatform> createPlatform(@Valid @RequestBody HotSearchPlatformRequest req) {
        checkSuperAdmin();
        return Result.success(platformService.create(req));
    }

    @PutMapping("/platforms/{id}")
    public Result<HotSearchPlatform> updatePlatform(@PathVariable("id") Long id, @Valid @RequestBody HotSearchPlatformRequest req) {
        checkSuperAdmin();
        return Result.success(platformService.update(id, req));
    }

    @DeleteMapping("/platforms/{id}")
    public Result<Void> deletePlatform(@PathVariable("id") Long id) {
        checkSuperAdmin();
        platformService.delete(id);
        return Result.success();
    }

    // ===== 每日榜单 =====
    @GetMapping("/daily")
    public Result<HotSearchDailyAdminService.PageResult> listDaily(@ModelAttribute HotSearchDailyQueryRequest req) {
        checkSuperAdmin();
        return Result.success(dailyService.list(req));
    }

    @PostMapping("/daily")
    public Result<HotSearchDailyAdminVO> createDaily(@Valid @RequestBody HotSearchDailyRequest req) {
        checkSuperAdmin();
        return Result.success(dailyService.create(req));
    }

    @PutMapping("/daily/{id}")
    public Result<HotSearchDailyAdminVO> updateDaily(@PathVariable("id") Long id, @Valid @RequestBody HotSearchDailyRequest req) {
        checkSuperAdmin();
        return Result.success(dailyService.update(id, req));
    }

    @DeleteMapping("/daily/{id}")
    public Result<Void> deleteDaily(@PathVariable("id") Long id) {
        checkSuperAdmin();
        dailyService.delete(id);
        return Result.success();
    }

    @PostMapping("/daily/{id}/re-crawl")
    public Result<CrawlResultVO> recrawlDaily(@PathVariable("id") Long id) {
        checkSuperAdmin();
        HotSearchDailyAdminVO vo = dailyService.get(id);
        return Result.success(crawlJob.recrawlPlatform(vo.getPlatformCode()));
    }

    // ===== 配置 =====
    @GetMapping("/config")
    public Result<HotSearchConfig> getConfig() {
        checkSuperAdmin();
        HotSearchConfig cfg = configService.getConfig();
        return Result.success(cfg);
    }

    @PutMapping("/config")
    public Result<HotSearchConfig> saveConfig(@Valid @RequestBody HotSearchConfigRequest req) {
        checkSuperAdmin();
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return Result.success(configService.saveConfig(req, adminId));
    }

    // ===== 手动抓取 & 摘要 =====
    @PostMapping("/crawl")
    public Result<CrawlResultVO> crawlNow() {
        checkSuperAdmin();
        return Result.success(crawlJob.crawlAll());
    }

    @GetMapping("/crawl/last-run")
    public Result<LastRunVO> lastRun() {
        checkSuperAdmin();
        return Result.success(crawlJob.getLastRun());
    }

    private void checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
