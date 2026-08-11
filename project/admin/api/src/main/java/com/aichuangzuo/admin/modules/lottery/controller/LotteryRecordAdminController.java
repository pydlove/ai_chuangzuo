package com.aichuangzuo.admin.modules.lottery.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryDisplayWinnerSaveRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryDrawRecordQueryRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryRedemptionCodeQueryRequest;
import com.aichuangzuo.admin.modules.lottery.service.LotteryDisplayWinnerAdminService;
import com.aichuangzuo.admin.modules.lottery.service.LotteryRecordAdminService;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryDisplayWinnerAdminVO;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryDrawRecordAdminVO;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryRedemptionCodeAdminVO;
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

@Tag(name = "管理端-抽奖记录与展示墙")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/lottery")
@RequiredArgsConstructor
public class LotteryRecordAdminController {

    private final LotteryRecordAdminService recordAdminService;
    private final LotteryDisplayWinnerAdminService displayWinnerAdminService;
    private final AdminUserPermissionService permissionService;

    @Operation(summary = "兑换记录列表")
    @GetMapping("/redemption-codes")
    public Result<LotteryRecordAdminService.PageResult<LotteryRedemptionCodeAdminVO>> listRedemptionCodes(
            @Valid @ModelAttribute LotteryRedemptionCodeQueryRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询兑换记录, adminUserId={}", adminUserId);
        return Result.success(recordAdminService.listRedemptionCodes(request));
    }

    @Operation(summary = "抽奖记录列表")
    @GetMapping("/draw-records")
    public Result<LotteryRecordAdminService.PageResult<LotteryDrawRecordAdminVO>> listDrawRecords(
            @Valid @ModelAttribute LotteryDrawRecordQueryRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询抽奖记录, adminUserId={}", adminUserId);
        return Result.success(recordAdminService.listDrawRecords(request));
    }

    @Operation(summary = "重置用户抽奖次数")
    @PostMapping("/draw-chances/reset")
    public Result<Void> resetDrawChance(@RequestParam Long campaignId, @RequestParam Long userId) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员重置用户抽奖次数, adminUserId={}, campaignId={}, userId={}", adminUserId, campaignId, userId);
        recordAdminService.resetDrawChance(campaignId, userId);
        return Result.success();
    }

    @Operation(summary = "展示墙列表")
    @GetMapping("/display-winners")
    public Result<List<LotteryDisplayWinnerAdminVO>> listDisplayWinners(@RequestParam Long campaignId) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询展示墙, adminUserId={}, campaignId={}", adminUserId, campaignId);
        return Result.success(displayWinnerAdminService.listByCampaign(campaignId));
    }

    @Operation(summary = "保存展示墙记录")
    @PostMapping("/display-winners")
    public Result<Void> saveDisplayWinner(@Valid @RequestBody LotteryDisplayWinnerSaveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员保存展示墙记录, adminUserId={}, winnerId={}", adminUserId, request.getId());
        displayWinnerAdminService.saveWinner(request, adminUserId);
        return Result.success();
    }

    @Operation(summary = "切换展示墙状态")
    @PostMapping("/display-winners/{id}/toggle")
    public Result<Void> toggleDisplayWinner(
            @PathVariable("id") Long id,
            @RequestParam Integer status) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员切换展示墙状态, adminUserId={}, winnerId={}, status={}", adminUserId, id, status);
        displayWinnerAdminService.toggleStatus(id, status, adminUserId);
        return Result.success();
    }

    @Operation(summary = "删除展示墙记录")
    @DeleteMapping("/display-winners/{id}")
    public Result<Void> deleteDisplayWinner(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除展示墙记录, adminUserId={}, winnerId={}", adminUserId, id);
        displayWinnerAdminService.deleteWinner(id, adminUserId);
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
