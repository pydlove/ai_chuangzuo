package com.aichuangzuo.admin.modules.message.job;

import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.aichuangzuo.admin.modules.message.handler.MessageNotifyHandler;
import com.aichuangzuo.admin.modules.message.mapper.NotifyOutboxMapper;
import com.aichuangzuo.admin.modules.scheduler.annotation.ScheduledTask;
import com.aichuangzuo.admin.modules.scheduler.executor.ScheduledTaskExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息通知 outbox 派发器。
 *
 * <p>每 5 秒（可配）扫一次 {@code a_message_notify_outbox}，取 status=PENDING 且
 * next_retry_at &lt;= now 的记录，按 {@code biz_type} 路由到对应 {@link MessageNotifyHandler}
 * 派发到 user-api。</p>
 *
 * <p>并发控制依赖 MySQL {@code SELECT ... FOR UPDATE SKIP LOCKED}：多实例部署时
 * 各实例互不阻塞也不会重复派发同一条记录。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyOutboxDispatcherJob {

    /** 最大重试次数（含首次派发失败后的重试）。 */
    private static final int MAX_RETRY = 5;

    /** 首次失败后 5s 重试，之后指数退避：5/10/20/40/80 秒。 */
    private static final int BASE_RETRY_SECONDS = 5;

    /** 每次派发最大条数。 */
    private static final int BATCH_SIZE = 50;

    private final NotifyOutboxMapper outboxMapper;
    private final List<MessageNotifyHandler> handlers;
    private final ScheduledTaskExecutor scheduledTaskExecutor;

    private Map<String, MessageNotifyHandler> handlerMap;

    @ScheduledTask(key = "notify_outbox_dispatch", name = "消息通知 Outbox 派发", description = "每 5 秒扫描待派发的消息通知 outbox，按 biz_type 路由派发到 user-api", triggerType = "fixed_delay", expression = "5000", sortOrder = 20)
    @Scheduled(fixedDelayString = "${notify.outbox.dispatcher.interval-ms:5000}")
    @Transactional(rollbackFor = Exception.class)
    public void dispatch() {
        scheduledTaskExecutor.executeAuto("notify_outbox_dispatch", () -> {
            LocalDateTime now = LocalDateTime.now();
            List<NotifyOutbox> pending = outboxMapper.selectPending(now, BATCH_SIZE);
            if (pending.isEmpty()) {
                return;
            }

            Map<String, MessageNotifyHandler> map = getHandlerMap();
            int sent = 0;
            int failed = 0;
            int scheduled = 0;

            for (NotifyOutbox row : pending) {
                MessageNotifyHandler handler = map.get(row.getBizType());
                if (handler == null) {
                    log.warn("未找到 biz_type={} 的消息通知 handler，outboxId={} 置为 FAILED", row.getBizType(), row.getId());
                    outboxMapper.markFailed(row.getId(), "未知 biz_type: " + row.getBizType());
                    failed++;
                    continue;
                }

                try {
                    handler.dispatch(row);
                    outboxMapper.markSent(row.getId(), LocalDateTime.now());
                    sent++;
                } catch (Exception e) {
                    int nextRetry = row.getRetryCount() == null ? 0 : row.getRetryCount() + 1;
                    if (nextRetry >= MAX_RETRY) {
                        outboxMapper.markFailed(row.getId(), truncate(e.getMessage()));
                        failed++;
                        log.error("outboxId={} bizType={} 达到最大重试次数，置为 FAILED", row.getId(), row.getBizType(), e);
                    } else {
                        LocalDateTime nextRetryAt = now.plus(
                                BASE_RETRY_SECONDS * (1L << nextRetry), ChronoUnit.SECONDS);
                        outboxMapper.scheduleRetry(row.getId(), nextRetryAt, truncate(e.getMessage()));
                        scheduled++;
                        log.warn("outboxId={} bizType={} 第 {} 次派发失败，下次重试 {}: {}",
                                row.getId(), row.getBizType(), nextRetry, nextRetryAt, e.getMessage());
                    }
                }
            }

            if (sent > 0 || failed > 0 || scheduled > 0) {
                log.info("消息通知派发完成 sent={} scheduled={} failed={}", sent, scheduled, failed);
            }
        });
    }

    private synchronized Map<String, MessageNotifyHandler> getHandlerMap() {
        if (handlerMap == null) {
            handlerMap = handlers.stream()
                    .collect(Collectors.toMap(MessageNotifyHandler::bizType, h -> h,
                            (a, b) -> {
                                throw new IllegalStateException("biz_type 重复的 handler: " + a.bizType());
                            }));
        }
        return handlerMap;
    }

    private static String truncate(String message) {
        if (message == null) return null;
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
