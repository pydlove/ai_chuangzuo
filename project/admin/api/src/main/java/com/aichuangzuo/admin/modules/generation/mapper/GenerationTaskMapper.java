package com.aichuangzuo.admin.modules.generation.mapper;

import com.aichuangzuo.admin.modules.generation.dto.GenerationTaskListRow;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GenerationTaskMapper extends BaseMapper<GenerationTask> {

    /**
     * 原子抢占一批待处理任务（FOR UPDATE SKIP LOCKED）。
     * 多个 worker / 实例并发调用不会拿到同一行。
     *
     * @param limit         抢占数量上限
     * @param workerId      当前 worker 实例 ID
     * @param leaseMinutes  lease 持续分钟数（5）
     * @return 抢占到的任务列表
     */
    List<GenerationTask> claimBatch(@Param("limit") int limit,
                                   @Param("workerId") String workerId,
                                   @Param("leaseMinutes") int leaseMinutes);

    /**
     * 释放 lease 已过期的 processing 任务回 queued（守护线程调用）。
     * 影响行数用于监控。
     */
    int releaseExpiredLeases(@Param("now") LocalDateTime now);

    /**
     * 归档过期任务到 history（在事务中调用）。
     * 选取 completed/failed 且 (retention_days 是 null 不归档) + (created_at + retention_days < now) 的行。
     * 返回选中的 task id 列表，便于 service 层逐行迁 history 后从主表删除。
     */
    List<Long> selectExpiredTaskIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * Admin 端-创作任务列表（含用户昵称）。
     *
     * @param status  null=查全部；指定时按状态过滤
     * @param keyword bizNo 或用户昵称关键字
     */
    List<GenerationTaskListRow> selectAdminList(@Param("status") Integer status,
                                                @Param("keyword") String keyword,
                                                @Param("offset") long offset,
                                                @Param("limit") int limit);

    /** 配合 {@link #selectAdminList} 的 count。 */
    long countAdminList(@Param("status") Integer status,
                        @Param("keyword") String keyword);
}
