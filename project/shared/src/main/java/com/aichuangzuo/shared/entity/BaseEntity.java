package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 实体基类：承载各业务表通用的审计字段（创建/更新时间、创建人/更新人、逻辑删除标记）。
 *
 * <p>使用约定：
 * <ul>
 *   <li>子类使用 {@code @TableName("...")} 指定表名即可，无需重复声明这些字段。</li>
 *   <li>对应表必须包含 {@code created_at} / {@code updated_at} / {@code is_deleted} 三列；
 *       {@code created_by} / {@code updated_by} 若表中不存在，子类需用
 *       {@code @TableField(exist = false)} 重新声明同名字段以覆盖。</li>
 *   <li>时间字段由 {@code MybatisPlusMetaObjectHandler} 自动填充，业务代码无需手动 set。</li>
 * </ul>
 */
@Getter
@Setter
public abstract class BaseEntity {

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}