package com.aichuangzuo.user.modules.generation.mapper;

import com.aichuangzuo.shared.entity.GenerationTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GenerationTaskMapper extends BaseMapper<GenerationTask> {

    /**
     * 按用户 + 状态过滤，列表分页查询（FIFO 后入先看）。
     *
     * @param targetUserId  当前用户 ID
     * @param statuses      状态数组（空则不过滤）
     * @param offset        分页偏移
     * @param size          分页大小
     */
    List<GenerationTask> selectUserTasks(@Param("targetUserId") Long targetUserId,
                                        @Param("statuses") List<Integer> statuses,
                                        @Param("offset") long offset,
                                        @Param("size") int size);

    /**
     * 按用户 + 状态统计数量。
     */
    long countUserTasks(@Param("targetUserId") Long targetUserId,
                        @Param("statuses") List<Integer> statuses);

    /**
     * 统计每分钟该用户的提交数（限流用）。
     */
    long countSubmittedInLastMinute(@Param("targetUserId") Long targetUserId,
                                    @Param("sinceEpochSecond") long sinceEpochSecond);
}
