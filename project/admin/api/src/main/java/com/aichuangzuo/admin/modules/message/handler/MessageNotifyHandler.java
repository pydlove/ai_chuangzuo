package com.aichuangzuo.admin.modules.message.handler;

import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;

/**
 * 消息通知 outbox 派发处理器。
 *
 * <p>每种业务类型（generation_completed / generation_failed / ...）实现一个 handler，
 * 由 {@link com.aichuangzuo.admin.modules.message.job.NotifyOutboxDispatcherJob} 按
 * {@link NotifyOutbox#getBizType()} 路由调用。</p>
 */
public interface MessageNotifyHandler {

    /**
     * 本 handler 负责的业务类型。
     *
     * @return 与 {@link NotifyOutbox#setBizType(String)} 值一致
     */
    String bizType();

    /**
     * 把 outbox 行派发到 user-api。
     *
     * <p>实现方应解析 {@code row.getPayload()}，调用对应 user-api 内部接口；
     * 调用成功由派发 job 负责把 outbox 标为 SENT，handler 抛异常即可让派发 job 进入重试。</p>
     *
     * @param row 待派发的 outbox 行
     */
    void dispatch(NotifyOutbox row);
}
