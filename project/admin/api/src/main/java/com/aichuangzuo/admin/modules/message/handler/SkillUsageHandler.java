package com.aichuangzuo.admin.modules.message.handler;

import com.aichuangzuo.admin.modules.generation.service.SkillUsageInternalClient;
import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 提示词使用计数 outbox 处理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillUsageHandler implements MessageNotifyHandler {

    public static final String BIZ_TYPE = "skill_usage";

    private final SkillUsageInternalClient usageClient;
    private final ObjectMapper objectMapper;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public void dispatch(NotifyOutbox row) {
        Map<String, Object> payload = parse(row.getPayload());
        Long taskId = asLong(payload.get("taskId"));
        Long userId = asLong(payload.get("userId"));
        String skillRef = asString(payload.get("skillRef"));

        if (userId == null || skillRef == null || skillRef.isBlank()) {
            throw new IllegalArgumentException(
                    "skill_usage payload 缺失必填字段：userId/skillRef");
        }
        usageClient.recordUsage(taskId, userId, skillRef);
    }

    private Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("解析 skill_usage payload 失败: " + e.getMessage(), e);
        }
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
