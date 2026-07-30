package com.aichuangzuo.admin.modules.message.handler;

import com.aichuangzuo.admin.modules.generation.service.GenerationNotifyInternalClient;
import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 生成任务失败通知处理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationFailedHandler implements MessageNotifyHandler {

    public static final String BIZ_TYPE = "generation_failed";

    private final GenerationNotifyInternalClient notifyClient;
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
        String failReason = asString(payload.get("failReason"));

        if (taskId == null || userId == null) {
            throw new IllegalArgumentException(
                    "generation_failed payload 缺失必填字段：taskId/userId");
        }
        notifyClient.notifyFailed(taskId, userId, failReason);
    }

    private Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("解析 generation_failed payload 失败: " + e.getMessage(), e);
        }
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
