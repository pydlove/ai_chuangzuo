package com.aichuangzuo.admin.modules.audit.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.audit.dto.request.AuditConfigRequest;
import com.aichuangzuo.admin.modules.audit.entity.AuditConfig;
import com.aichuangzuo.admin.modules.audit.service.AuditConfigService;
import com.aichuangzuo.admin.modules.audit.vo.AuditConfigVO;
import com.aichuangzuo.admin.modules.audit.vo.AuditLogPageVO;
import com.aichuangzuo.admin.modules.audit.vo.AuditLogVO;
import com.aichuangzuo.admin.modules.audit.client.UserAuditLogClient;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "审计日志")
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogAdminController {

    private final UserAuditLogClient userAuditLogClient;
    private final AuditConfigService auditConfigService;
    private final PlatformUserMapper platformUserMapper;

    @Operation(summary = "查询用户操作审计日志")
    @GetMapping
    public Result<AuditLogPageVO> list(
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {

        Long targetUserId = userId;
        if (targetUserId == null && keyword != null && !keyword.isBlank()) {
            LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(PlatformUser::getEmail, keyword)
                    .or()
                    .like(PlatformUser::getNickname, keyword)
                    .last("LIMIT 1");
            PlatformUser user = platformUserMapper.selectOne(wrapper);
            if (user == null) {
                AuditLogPageVO empty = new AuditLogPageVO();
                empty.setList(List.of());
                empty.setTotal(0);
                empty.setPage(page);
                empty.setPageSize(pageSize);
                return Result.success(empty);
            }
            targetUserId = user.getId();
        }

        Map<String, Object> data = userAuditLogClient.queryLogs(targetUserId, startDate, endDate, page, pageSize);
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("list");
        long total = asLong(data.get("total"));
        long currentPage = asLong(data.get("page"));
        long currentPageSize = asLong(data.get("pageSize"));

        List<AuditLogVO> list = new ArrayList<>();
        if (records != null) {
            List<Long> userIds = records.stream()
                    .map(r -> asLong(r.get("userId")))
                    .distinct()
                    .toList();
            Map<Long, PlatformUser> userMap = platformUserMapper.selectBatchIds(userIds)
                    .stream()
                    .collect(Collectors.toMap(PlatformUser::getId, u -> u));

            for (Map<String, Object> r : records) {
                AuditLogVO vo = new AuditLogVO();
                vo.setId(asLong(r.get("id")));
                vo.setUserId(asLong(r.get("userId")));
                PlatformUser user = userMap.get(vo.getUserId());
                vo.setNickname(user == null ? null : user.getNickname());
                vo.setEmail(user == null ? null : user.getEmail());
                vo.setActionType((String) r.get("actionType"));
                vo.setModule((String) r.get("module"));
                vo.setRequestMethod((String) r.get("requestMethod"));
                vo.setRequestUri((String) r.get("requestUri"));
                vo.setRequestParams((String) r.get("requestParams"));
                vo.setRequestBody((String) r.get("requestBody"));
                vo.setClientIp((String) r.get("clientIp"));
                vo.setUserAgent((String) r.get("userAgent"));
                vo.setStatusCode((Integer) r.get("statusCode"));
                vo.setErrorMsg((String) r.get("errorMsg"));
                vo.setDurationMs((Integer) r.get("durationMs"));
                vo.setCreatedAt(parseTime(r.get("createdAt")));
                list.add(vo);
            }
        }

        AuditLogPageVO vo = new AuditLogPageVO();
        vo.setList(list);
        vo.setTotal(total);
        vo.setPage(currentPage);
        vo.setPageSize(currentPageSize);
        return Result.success(vo);
    }

    @Operation(summary = "获取审计日志配置")
    @GetMapping("/config")
    public Result<AuditConfigVO> getConfig() {
        AuditConfig config = auditConfigService.getConfig();
        return Result.success(toVO(config));
    }

    @Operation(summary = "更新审计日志配置")
    @PutMapping("/config")
    public Result<AuditConfigVO> updateConfig(@Valid @RequestBody AuditConfigRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        AuditConfig config = auditConfigService.saveConfig(request, adminId);
        return Result.success(toVO(config));
    }

    private AuditConfigVO toVO(AuditConfig config) {
        AuditConfigVO vo = new AuditConfigVO();
        vo.setId(config.getId());
        vo.setRetentionDays(config.getRetentionDays());
        vo.setCleanupCron(config.getCleanupCron());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setUpdatedAt(config.getUpdatedAt());
        vo.setUpdatedBy(config.getUpdatedBy());
        return vo;
    }

    private long asLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private static final DateTimeFormatter CREATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime parseTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime t) return t;
        if (value instanceof String s) {
            try {
                return LocalDateTime.parse(s, CREATED_AT_FORMATTER);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
