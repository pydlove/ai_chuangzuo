package com.aichuangzuo.user.modules.skill.market.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.skill.market.service.SkillUsageRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端内部接口：供管理端 outbox dispatcher 调用，记录提示词使用。
 * <p>由 {@code InternalKeyAuthenticationFilter} 校验 {@code X-Internal-Key}。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/internal/skills")
@RequiredArgsConstructor
public class SkillUsageInternalController {

    private final SkillUsageRecordService skillUsageRecordService;

    @PostMapping("/use")
    public Result<Void> recordUsage(@RequestBody Map<String, Object> payload) {
        Long taskId = asLong(payload.get("taskId"));
        Long userId = asLong(payload.get("userId"));
        String skillRef = asString(payload.get("skillRef"));
        log.info("记录提示词使用 taskId={} userId={} skillRef={}", taskId, userId, skillRef);
        if (userId == null || skillRef == null || skillRef.isBlank()) {
            log.warn("记录提示词使用入参缺失 taskId={} userId={} skillRef={}", taskId, userId, skillRef);
            return Result.success();
        }
        skillUsageRecordService.record(skillRef, userId);
        return Result.success();
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
