package com.aichuangzuo.admin.modules.topictitle.mapper;

import com.aichuangzuo.admin.modules.topictitle.entity.TopicTitleTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TopicTitleTaskMapper extends BaseMapper<TopicTitleTask> {

    /**
     * 取最早一条 QUEUED 任务。worker 跑在单线程，用 LIMIT 1 抢锁即可，
     * 不会有并发，所以不用 SKIP LOCKED 也不需要事务。
     */
    @Select("SELECT * FROM t_topic_title_task WHERE status = 0 ORDER BY id ASC LIMIT 1")
    TopicTitleTask selectNextQueued();

    /** 防止 List 引用 import 警告（其他场景可能复用）。 */
    @Select("SELECT * FROM t_topic_title_task WHERE status = #{status} ORDER BY id ASC")
    List<TopicTitleTask> selectByStatus(@Param("status") Integer status);
}