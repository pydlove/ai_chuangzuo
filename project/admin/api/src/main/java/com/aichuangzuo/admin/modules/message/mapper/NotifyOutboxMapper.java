package com.aichuangzuo.admin.modules.message.mapper;

import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息通知 outbox Mapper。
 */
@Mapper
public interface NotifyOutboxMapper extends BaseMapper<NotifyOutbox> {

    /**
     * 取一批待派发记录（跳过已被其他实例锁定的行）。
     *
     * @param now   当前时间，只取 next_retry_at <= now 的记录
     * @param limit 最大条数
     * @return 待派发记录（已被 SELECT FOR UPDATE SKIP LOCKED 锁定）
     */
    List<NotifyOutbox> selectPending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 标记单条记录为已派发。
     *
     * @param id     记录 id
     * @param sentAt 派发成功时间
     * @return 更新行数
     */
    int markSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    /**
     * 标记单条记录为失败（达到最大重试次数）。
     *
     * @param id        记录 id
     * @param lastError 最后一次失败原因
     * @return 更新行数
     */
    int markFailed(@Param("id") Long id, @Param("lastError") String lastError);

    /**
     * 重试：增加 retry_count，并把 next_retry_at 推到未来某个时间点。
     *
     * @param id          记录 id
     * @param nextRetryAt 下次可重试时间
     * @param lastError   本次失败原因
     * @return 更新行数
     */
    int scheduleRetry(@Param("id") Long id,
                      @Param("nextRetryAt") LocalDateTime nextRetryAt,
                      @Param("lastError") String lastError);
}
