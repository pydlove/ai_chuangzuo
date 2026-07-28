package com.aichuangzuo.admin.modules.skill.preset.mapper;

import com.aichuangzuo.admin.modules.skill.preset.dto.SystemSkillRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预设风格聚合查询 Mapper。SQL 定义在 {@code resources/mapper/GlobalSkillAggregateMapper.xml}。
 */
@Mapper
public interface GlobalSkillAggregateMapper {

    /**
     * 分页查询系统预设风格列表（含启用状态过滤和关键词搜索）。
     */
    List<SystemSkillRow> selectGlobalSkillPage(@Param("enableStatus") Integer enableStatus,
                                                @Param("keyword") String keyword,
                                                @Param("offset") long offset,
                                                @Param("limit") long limit);

    /**
     * 同条件下的总数。
     */
    long countGlobalSkillPage(@Param("enableStatus") Integer enableStatus,
                               @Param("keyword") String keyword);
}