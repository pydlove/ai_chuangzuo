package com.aichuangzuo.admin.modules.style.review.mapper;

import com.aichuangzuo.admin.modules.style.review.dto.StyleReviewRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 风格审核聚合查询 Mapper。SQL 定义在 {@code resources/mapper/StyleReviewAggregateMapper.xml}。
 */
@Mapper
public interface StyleReviewAggregateMapper {

    /**
     * 分页查询审核列表（含状态过滤和关键词搜索）。
     */
    List<StyleReviewRow> selectReviewPage(@Param("status") Integer status,
                                          @Param("reviewed") Boolean reviewed,
                                          @Param("keyword") String keyword,
                                          @Param("offset") long offset,
                                          @Param("limit") long limit);

    /**
     * 同条件下的总数。
     */
    long countReviewPage(@Param("status") Integer status,
                         @Param("reviewed") Boolean reviewed,
                         @Param("keyword") String keyword);
}