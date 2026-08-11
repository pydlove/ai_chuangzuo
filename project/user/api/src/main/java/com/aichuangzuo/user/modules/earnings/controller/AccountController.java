package com.aichuangzuo.user.modules.earnings.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.earnings.dto.request.ListEarningsRequest;
import com.aichuangzuo.user.modules.earnings.dto.request.RealNameRequest;
import com.aichuangzuo.user.modules.earnings.dto.request.WithdrawApplyRequest;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.earnings.service.WithdrawService;
import com.aichuangzuo.user.modules.earnings.vo.AccountSummaryVO;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordPageVO;
import com.aichuangzuo.user.modules.earnings.vo.MonthlySettlementVO;
import com.aichuangzuo.user.modules.earnings.vo.RealNameVO;
import com.aichuangzuo.user.modules.earnings.vo.WithdrawRequestVO;
import com.aichuangzuo.user.modules.user.service.InviteRewardService;
import com.aichuangzuo.user.modules.user.vo.InviteStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户账户与收益接口。
 */
@Tag(name = "用户账户收益")
@RestController
@RequestMapping("/api/v1/user/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final EarningsService earningsService;
    private final InviteRewardService inviteRewardService;
    private final WithdrawService withdrawService;

    @Operation(summary = "账户收益汇总")
    @GetMapping("/summary")
    public Result<AccountSummaryVO> summary() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("Get account summary, userId={}", userId);
        return Result.success(earningsService.getSummary(userId));
    }

    /**
     * 邀请有礼统计。
     *
     * @return 邀请码、累计邀请人数、会员天数奖励、创作币返利、余额、好友列表
     */
    @Operation(summary = "邀请有礼统计")
    @GetMapping("/invite-stats")
    public Result<InviteStatsVO> inviteStats() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("Get invite stats, userId={}", userId);
        return Result.success(inviteRewardService.getInviteStats(userId));
    }

    @Operation(summary = "按月收益汇总")
    @GetMapping("/monthly-summary")
    public Result<List<MonthlySettlementVO>> monthlySummary() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("Get monthly summary, userId={}", userId);
        return Result.success(earningsService.getMonthlySummary(userId));
    }

    @Operation(summary = "收益记录分页")
    @GetMapping("/earnings")
    public Result<EarningsRecordPageVO> earnings(@Valid ListEarningsRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("List earnings records, userId={}, month={}, page={}, pageSize={}",
                userId, request.getMonth(), request.getPage(), request.getPageSize());
        return Result.success(earningsService.listEarnings(userId, request));
    }

    @Operation(summary = "获取实名信息")
    @GetMapping("/real-name")
    public Result<RealNameVO> getRealName() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("Get real name info, userId={}", userId);
        return Result.success(withdrawService.getRealName(userId));
    }

    @Operation(summary = "提交实名认证")
    @PostMapping("/real-name")
    public Result<Void> submitRealName(@Valid @RequestBody RealNameRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("Submit real name, userId={}, realName={}", userId, request.getRealName());
        withdrawService.submitRealName(userId, request);
        return Result.success();
    }

    @Operation(summary = "提现记录列表")
    @GetMapping("/withdrawals")
    public Result<List<WithdrawRequestVO>> listWithdrawals() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("List withdrawals, userId={}", userId);
        return Result.success(withdrawService.listWithdrawRequests(userId));
    }

    @Operation(summary = "申请提现")
    @PostMapping("/withdrawals")
    public Result<String> applyWithdraw(@Valid @RequestBody WithdrawApplyRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("Apply withdraw, userId={}, amount={}, accountLength={}",
                userId, request.getAmount(), request.getAccount() == null ? 0 : request.getAccount().length());
        return Result.success(withdrawService.applyWithdraw(userId, request));
    }
}
