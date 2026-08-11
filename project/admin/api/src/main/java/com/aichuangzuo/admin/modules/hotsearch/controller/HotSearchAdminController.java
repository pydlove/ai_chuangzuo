package com.aichuangzuo.admin.modules.hotsearch.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchConfigRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchCrawlLogQueryRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyQueryRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchPlatformRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchConfig;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.enums.HotSearchTriggerType;
import com.aichuangzuo.admin.modules.hotsearch.job.HotSearchCrawlJob;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchConfigService;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchCrawlLogService;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchDailyAdminService;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchPlatformAdminService;
import com.aichuangzuo.admin.modules.hotsearch.vo.CrawlResultVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.LastRunVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.HotSearchDailyAdminVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/hot-search")
@RequiredArgsConstructor
public class HotSearchAdminController {

    private final HotSearchPlatformAdminService platformService;
    private final HotSearchDailyAdminService dailyService;
    private final HotSearchConfigService configService;
    private final HotSearchCrawlLogService crawlLogService;
    private final HotSearchCrawlJob crawlJob;
    private final AdminUserPermissionService permissionService;

    // ===== 平台 =====
    @GetMapping("/platforms")
    public Result<List<HotSearchPlatform>> listPlatforms() {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询热搜平台列表, adminUserId={}", adminUserId);
        return Result.success(platformService.listAll());
    }

    @PostMapping("/platforms")
    public Result<HotSearchPlatform> createPlatform(@Valid @RequestBody HotSearchPlatformRequest req) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员创建热搜平台, adminUserId={}, code={}, name={}",
                adminUserId, req.getCode(), req.getName());
        return Result.success(platformService.create(req));
    }

    @PutMapping("/platforms/{id}")
    public Result<HotSearchPlatform> updatePlatform(@PathVariable("id") Long id, @Valid @RequestBody HotSearchPlatformRequest req) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员更新热搜平台, adminUserId={}, platformId={}, code={}, name={}",
                adminUserId, id, req.getCode(), req.getName());
        return Result.success(platformService.update(id, req));
    }

    @DeleteMapping("/platforms/{id}")
    public Result<Void> deletePlatform(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除热搜平台, adminUserId={}, platformId={}", adminUserId, id);
        platformService.delete(id);
        return Result.success();
    }

    // ===== 每日榜单 =====
    @GetMapping("/daily")
    public Result<HotSearchDailyAdminService.PageResult> listDaily(@ModelAttribute HotSearchDailyQueryRequest req) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询热搜每日榜单, adminUserId={}, platform={}, date={}, page={}, size={}",
                adminUserId, req.getPlatform(), req.getDate(), req.getPage(), req.getSize());
        return Result.success(dailyService.list(req));
    }

    @PostMapping("/daily")
    public Result<HotSearchDailyAdminVO> createDaily(@Valid @RequestBody HotSearchDailyRequest req) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员创建热搜每日条目, adminUserId={}, platformCode={}, title={}, snapshotDate={}",
                adminUserId, req.getPlatformCode(), req.getTitle(), req.getSnapshotDate());
        return Result.success(dailyService.create(req));
    }

    @PutMapping("/daily/{id}")
    public Result<HotSearchDailyAdminVO> updateDaily(@PathVariable("id") Long id, @Valid @RequestBody HotSearchDailyRequest req) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员更新热搜每日条目, adminUserId={}, dailyId={}, platformCode={}, title={}",
                adminUserId, id, req.getPlatformCode(), req.getTitle());
        return Result.success(dailyService.update(id, req));
    }

    @DeleteMapping("/daily/{id}")
    public Result<Void> deleteDaily(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除热搜每日条目, adminUserId={}, dailyId={}", adminUserId, id);
        dailyService.delete(id);
        return Result.success();
    }

    @PostMapping("/daily/{id}/re-crawl")
    public Result<CrawlResultVO> recrawlDaily(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员手动重新抓取热搜, adminUserId={}, dailyId={}", adminUserId, id);
        HotSearchDailyAdminVO vo = dailyService.get(id);
        return Result.success(crawlJob.recrawlPlatform(vo.getPlatformCode(), HotSearchTriggerType.MANUAL));
    }

    // ===== 配置 =====
    @GetMapping("/config")
    public Result<HotSearchConfig> getConfig() {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询热搜配置, adminUserId={}", adminUserId);
        HotSearchConfig cfg = configService.getConfig();
        return Result.success(cfg);
    }

    @PutMapping("/config")
    public Result<HotSearchConfig> saveConfig(@Valid @RequestBody HotSearchConfigRequest req) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员更新热搜配置, adminUserId={}, cron={}, enabled={}, topN={}",
                adminUserId, req.getCron(), req.getEnabled(), req.getTopN());
        return Result.success(configService.saveConfig(req, adminUserId));
    }

    // ===== 手动抓取 & 摘要 & 日志 =====
    @PostMapping("/crawl")
    public Result<CrawlResultVO> crawlNow() {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员手动触发热搜全量抓取, adminUserId={}", adminUserId);
        return Result.success(crawlJob.crawlAll(HotSearchTriggerType.MANUAL));
    }

    @GetMapping("/crawl/last-run")
    public Result<LastRunVO> lastRun() {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询热搜最后抓取时间, adminUserId={}", adminUserId);
        return Result.success(crawlJob.getLastRun());
    }

    @GetMapping("/crawl/logs")
    public Result<HotSearchCrawlLogService.PageResult> listCrawlLogs(@ModelAttribute HotSearchCrawlLogQueryRequest req) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询热搜抓取日志, adminUserId={}, triggerType={}, page={}, size={}",
                adminUserId, req.getTriggerType(), req.getPage(), req.getSize());
        return Result.success(crawlLogService.list(req));
    }

    private Long checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminId;
    }
}
