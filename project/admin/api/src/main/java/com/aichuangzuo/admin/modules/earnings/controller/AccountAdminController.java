package com.aichuangzuo.admin.modules.earnings.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.earnings.dto.request.AccountQueryRequest;
import com.aichuangzuo.admin.modules.earnings.service.AccountAdminService;
import com.aichuangzuo.admin.modules.earnings.vo.UserAccountDetailVO;
import com.aichuangzuo.admin.modules.earnings.vo.UserAccountPageVO;
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
}
