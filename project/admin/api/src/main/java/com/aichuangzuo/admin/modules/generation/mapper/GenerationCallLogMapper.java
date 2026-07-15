package com.aichuangzuo.admin.modules.generation.mapper;

import com.aichuangzuo.admin.modules.generation.entity.GenerationCallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GenerationCallLogMapper extends BaseMapper<GenerationCallLog> {

    /**
     * 批量插入（用于 pipeline 跑完后一次性写入当次任务所有 AI 调用记录）。
     * 走 foreach + values 语法，几十条以内性能 OK。
     */
    int batchInsert(@Param("list") List<GenerationCallLog> list);

    /**
     * 查某任务的所有调用记录（按 stage_index ASC, attempt ASC）。
     */
    List<GenerationCallLog> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 物理删除某任务的全部调用日志（配合 batchInsert 实现全量替换）。
     */
    int deleteByTaskId(@Param("taskId") Long taskId);
}
