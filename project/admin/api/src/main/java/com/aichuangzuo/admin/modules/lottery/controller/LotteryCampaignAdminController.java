package com.aichuangzuo.admin.modules.lottery.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryCampaignQueryRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryCampaignSaveRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryPrizeTierSaveRequest;
import com.aichuangzuo.admin.modules.lottery.service.LotteryCampaignAdminService;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryCampaignAdminVO;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryPrizeTierAdminVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端-抽奖活动配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/lottery")
@RequiredArgsConstructor
public class LotteryCampaignAdminController {

    private final LotteryCampaignAdminService campaignAdminService;
    private final AdminUserPermissionService permissionService;

    @Operation(summary = "活动列表")
    @GetMapping("/campaigns")
    public Result<LotteryCampaignAdminService.PageResult> listCampaigns(@Valid @ModelAttribute LotteryCampaignQueryRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询抽奖活动列表, adminUserId={}", adminUserId);
        return Result.success(campaignAdminService.listCampaigns(request));
    }

    @Operation(summary = "活动详情")
    @GetMapping("/campaigns/{id}")
    public Result<LotteryCampaignAdminVO> getCampaign(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询抽奖活动详情, adminUserId={}, campaignId={}", adminUserId, id);
        return Result.success(campaignAdminService.getCampaign(id));
    }

    @Operation(summary = "保存活动")
    @PostMapping("/campaigns")
    public Result<Void> saveCampaign(@Valid @RequestBody LotteryCampaignSaveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员保存抽奖活动, adminUserId={}, campaignId={}", adminUserId, request.getId());
        campaignAdminService.saveCampaign(request, adminUserId);
        return Result.success();
    }

    @Operation(summary = "开启活动")
    @PostMapping("/campaigns/{id}/open")
    public Result<Void> openCampaign(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员开启抽奖活动, adminUserId={}, campaignId={}", adminUserId, id);
        campaignAdminService.openCampaign(id, adminUserId);
        return Result.success();
    }

    @Operation(summary = "关闭活动")
    @PostMapping("/campaigns/{id}/close")
    public Result<Void> closeCampaign(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员关闭抽奖活动, adminUserId={}, campaignId={}", adminUserId, id);
        campaignAdminService.closeCampaign(id, adminUserId);
        return Result.success();
    }

    @Operation(summary = "删除活动")
    @DeleteMapping("/campaigns/{id}")
    public Result<Void> deleteCampaign(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除抽奖活动, adminUserId={}, campaignId={}", adminUserId, id);
        campaignAdminService.deleteCampaign(id, adminUserId);
        return Result.success();
    }

    @Operation(summary = "奖项列表")
    @GetMapping("/campaigns/{campaignId}/tiers")
    public Result<List<LotteryPrizeTierAdminVO>> listTiers(@PathVariable("campaignId") Long campaignId) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询奖项列表, adminUserId={}, campaignId={}", adminUserId, campaignId);
        return Result.success(campaignAdminService.listTiers(campaignId));
    }

    @Operation(summary = "保存奖项")
    @PostMapping("/campaigns/{campaignId}/tiers")
    public Result<Void> saveTier(
            @PathVariable("campaignId") Long campaignId,
            @Valid @RequestBody LotteryPrizeTierSaveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员保存奖项, adminUserId={}, campaignId={}, tierId={}", adminUserId, campaignId, request.getId());
        campaignAdminService.saveTier(campaignId, request, adminUserId);
        return Result.success();
    }

    @Operation(summary = "删除奖项")
    @DeleteMapping("/campaigns/{campaignId}/tiers/{tierId}")
    public Result<Void> deleteTier(
            @PathVariable("campaignId") Long campaignId,
            @PathVariable("tierId") Long tierId) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除奖项, adminUserId={}, campaignId={}, tierId={}", adminUserId, campaignId, tierId);
        campaignAdminService.deleteTier(campaignId, tierId, adminUserId);
        return Result.success();
    }

    private Long checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminId;
    }
}
