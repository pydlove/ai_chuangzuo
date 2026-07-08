package com.aichuangzuo.admin.modules.style.preset.mapper;

import com.aichuangzuo.admin.modules.style.preset.dto.SystemStyleRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预设风格聚合查询 Mapper。SQL 定义在 {@code resources/mapper/GlobalStyleAggregateMapper.xml}。
 */
@Mapper
public interface GlobalStyleAggregateMapper {

    /**
     * 分页查询系统预设风格列表（含启用状态过滤和关键词搜索）。
     */
    List<SystemStyleRow> selectGlobalStylePage(@Param("enableStatus") Integer enableStatus,
                                                @Param("keyword") String keyword,
                                                @Param("offset") long offset,
                                                @Param("limit") long limit);

    /**
     * 同条件下的总数。
     */
    long countGlobalStylePage(@Param("enableStatus") Integer enableStatus,
                               @Param("keyword") String keyword);
}