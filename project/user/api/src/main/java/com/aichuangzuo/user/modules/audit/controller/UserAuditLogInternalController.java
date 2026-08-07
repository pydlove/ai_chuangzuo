package com.aichuangzuo.user.modules.audit.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.audit.dto.request.AuditLogCleanupRequest;
import com.aichuangzuo.user.modules.audit.entity.UserAuditLog;
import com.aichuangzuo.user.modules.audit.mapper.UserAuditLogMapper;
import com.aichuangzuo.user.modules.audit.vo.UserAuditLogPageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户端内部接口：供管理端查询和清理用户操作审计日志。
 * <p>由 {@code InternalKeyAuthenticationFilter} 校验 {@code X-Internal-Key}。
 */
@RestController
@RequestMapping("/api/v1/user/internal/audit-logs")
@RequiredArgsConstructor
public class UserAuditLogInternalController {

    private final UserAuditLogMapper userAuditLogMapper;

    @GetMapping
    public Result<UserAuditLogPageVO> list(
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {

        LambdaQueryWrapper<UserAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(UserAuditLog::getUserId, userId);
        }
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(UserAuditLog::getCreatedAt, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.lt(UserAuditLog::getCreatedAt, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(UserAuditLog::getCreatedAt);

        Page<UserAuditLog> result = userAuditLogMapper.selectPage(new Page<>(page, pageSize), wrapper);

        UserAuditLogPageVO vo = new UserAuditLogPageVO();
        vo.setList(result.getRecords());
        vo.setTotal(result.getTotal());
        vo.setPage(result.getCurrent());
        vo.setPageSize(result.getSize());
        return Result.success(vo);
    }

    @PostMapping("/cleanup")
    public Result<Void> cleanup(@RequestBody AuditLogCleanupRequest request) {
        Integer retentionDays = request.getRetentionDays();
        if (retentionDays == null || retentionDays < 1) {
            retentionDays = 30;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        userAuditLogMapper.delete(new LambdaUpdateWrapper<UserAuditLog>()
                .lt(UserAuditLog::getCreatedAt, cutoff));
        return Result.success();
    }
}
