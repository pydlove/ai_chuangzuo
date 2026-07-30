package com.aichuangzuo.admin.modules.message.service;

import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.aichuangzuo.admin.modules.message.mapper.NotifyOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息通知 outbox 服务：供业务模块把通知写入待派发队列。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyOutboxService {

    private final NotifyOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    /**
     * 插入一条待派发记录。
     *
     * <p>调用方应在同一个事务内调用，确保业务状态与通知排队状态原子提交。</p>
     *
     * @param bizType       业务类型，如 generation_completed
     * @param bizId         业务主键
     * @param targetUserId  目标用户
     * @param payload       透传给 user-api 的字段
     * @param executeAfter  最早可派发时间（允许延迟派发）
     * @return 写入后的 outbox id
     */
    public Long insertPending(String bizType, Long bizId, Long targetUserId,
                              Map<String, Object> payload, LocalDateTime executeAfter) {
        NotifyOutbox row = new NotifyOutbox();
        row.setBizType(bizType);
        row.setBizId(bizId);
        row.setTargetUserId(targetUserId);
        row.setPayload(serialize(payload));
        row.setStatus(NotifyOutbox.Status.PENDING.code());
        row.setRetryCount(0);
        row.setNextRetryAt(executeAfter == null ? LocalDateTime.now() : executeAfter);
        row.setTenantId(0L);
        row.setIsDeleted(0);
        outboxMapper.insert(row);
        log.info("写入消息通知 outbox bizType={} bizId={} userId={} outboxId={}",
                bizType, bizId, targetUserId, row.getId());
        return row.getId();
    }

    /**
     * 立即派发的便捷方法（executeAfter = now）。
     */
    public Long insertPending(String bizType, Long bizId, Long targetUserId,
                              Map<String, Object> payload) {
        return insertPending(bizType, bizId, targetUserId, payload, LocalDateTime.now());
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化 notify payload 失败: " + e.getMessage(), e);
        }
    }
}
