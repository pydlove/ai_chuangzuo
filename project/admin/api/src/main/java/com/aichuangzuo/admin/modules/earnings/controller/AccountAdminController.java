package com.aichuangzuo.admin.modules.earnings.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.earnings.dto.request.AccountQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.UserCoinRecordQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.UserEarningsRecordQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.UserRewardRecordQueryRequest;
import com.aichuangzuo.admin.modules.earnings.service.AccountAdminService;
import com.aichuangzuo.admin.modules.earnings.vo.*;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端账户明细")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AccountAdminController {

    private final AccountAdminService accountAdminService;

    @Operation(summary = "查询账户列表")
    @GetMapping
    public Result<UserAccountPageVO> list(AccountQueryRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询收益账户列表, adminUserId={}, userId={}, nickname={}, phone={}, email={}, page={}, size={}",
                adminUserId, request.getUserId(), request.getNickname(), request.getPhone(),
                request.getEmail(), request.getPage(), request.getSize());
        return Result.success(accountAdminService.listAccounts(request));
    }

    @Operation(summary = "查看账户详情")
    @GetMapping("/{userId}")
    public Result<UserAccountDetailVO> detail(@PathVariable Long userId) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查看收益账户详情, adminUserId={}, userId={}", adminUserId, userId);
        return Result.success(accountAdminService.getAccountDetail(userId));
    }

    @Operation(summary = "用户创作币明细")
    @GetMapping("/{userId}/coin-records")
    public Result<PageResult<UserCoinRecordVO>> listUserCoinRecords(
            @PathVariable Long userId,
            @ModelAttribute UserCoinRecordQueryRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询用户创作币明细, adminUserId={}, userId={}", adminUserId, userId);
        return Result.success(accountAdminService.listUserCoinRecords(userId, request));
    }

    @Operation(summary = "用户收益明细")
    @GetMapping("/{userId}/earnings-records")
    public Result<PageResult<EarningsRecordVO>> listUserEarningsRecords(
            @PathVariable Long userId,
            @ModelAttribute UserEarningsRecordQueryRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询用户收益明细, adminUserId={}, userId={}", adminUserId, userId);
        return Result.success(accountAdminService.listUserEarningsRecords(userId, request));
    }

    @Operation(summary = "用户奖励明细")
    @GetMapping("/{userId}/reward-records")
    public Result<PageResult<RewardRecordVO>> listUserRewardRecords(
            @PathVariable Long userId,
            @ModelAttribute UserRewardRecordQueryRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询用户奖励明细, adminUserId={}, userId={}", adminUserId, userId);
        return Result.success(accountAdminService.listUserRewardRecords(userId, request));
    }
}
