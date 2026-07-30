package com.aichuangzuo.admin.modules.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息通知 outbox，对应表 {@code a_message_notify_outbox}。
 *
 * <p>业务模块在事务中写入 PENDING 状态，由 {@code NotifyOutboxDispatcherJob}
 * 异步派发到 user-api；达到最大重试次数后置 FAILED，等待人工介入或补偿。
 */
@Getter
@Setter
@TableName("a_message_notify_outbox")
public class NotifyOutbox {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型：generation_completed / generation_failed / ... */
    private String bizType;

    /** 业务主键，如 task_id。 */
    private Long bizId;

    /** 目标用户ID。 */
    private Long targetUserId;

    /**
     * 透传给 user-api 的 JSON 请求体。
     *
     * <p>由具体业务 handler 序列化，格式与
     * {@code /api/v1/user/internal/generation/notify-completion} 的 payload 对齐。
     */
    private String payload;

    /** 状态：0-PENDING 1-SENT 2-FAILED */
    private Integer status;

    /** 已重试次数。 */
    private Integer retryCount;

    /** 下次可重试时间。 */
    private LocalDateTime nextRetryAt;

    /** 最后一次失败原因。 */
    private String lastError;

    private Long tenantId;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 成功派发时间。 */
    private LocalDateTime sentAt;

    private Long createdBy;
    private Long updatedBy;

    /** Outbox 状态枚举。 */
    public enum Status {
        /** 待派发。 */
        PENDING(0),

        /** 已派发。 */
        SENT(1),

        /** 达到最大重试次数，人工介入。 */
        FAILED(2);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }
}
