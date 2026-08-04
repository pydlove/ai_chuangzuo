package com.aichuangzuo.admin.modules.learn.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 创作学院分类实体；继承 {@link BaseEntity} 自动获得审计字段 + is_deleted 软删除。
 */
@Getter
@Setter
@TableName("t_article_category")
public class LearnCategoryEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类 id；null 表示顶级 */
    private Long parentId;

    /** 分类名 */
    private String name;

    /** 排序值，升序展示 */
    private Integer sort;
}
