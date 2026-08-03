package com.aichuangzuo.user.modules.earnings.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.earnings.dto.request.ListEarningsRequest;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.earnings.vo.AccountSummaryVO;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordPageVO;
import com.aichuangzuo.user.modules.earnings.vo.MonthlySettlementVO;
import com.aichuangzuo.user.modules.user.service.InviteRewardService;
import com.aichuangzuo.user.modules.user.vo.InviteStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户账户与收益接口。
 */
@Tag(name = "用户账户收益")
@RestController
@RequestMapping("/api/v1/user/account")
@RequiredArgsConstructor
public class AccountController {

    private final EarningsService earningsService;
    private final InviteRewardService inviteRewardService;

    @Operation(summary = "账户收益汇总")
    @GetMapping("/summary")
    public Result<AccountSummaryVO> summary() {
        Long userId = SecurityUserContext.getCurrentUserId();
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
        return Result.success(inviteRewardService.getInviteStats(userId));
    }

    @Operation(summary = "按月收益汇总")
    @GetMapping("/monthly-summary")
    public Result<List<MonthlySettlementVO>> monthlySummary() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(earningsService.getMonthlySummary(userId));
    }

    @Operation(summary = "收益记录分页")
    @GetMapping("/earnings")
    public Result<EarningsRecordPageVO> earnings(@Valid ListEarningsRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(earningsService.listEarnings(userId, request));
    }
}
