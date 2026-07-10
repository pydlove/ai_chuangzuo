package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 创作模板版本快照，对应表 {@code t_prompt_template_version}。
 *
 * <p>每次发布把当前 12 阶段配置序列化为 config_json 落库，保留可追溯的历史。
 * 草稿阶段不入此表，仍在 {@code t_prompt_template_stage}。
 */
@Getter
@Setter
@TableName("t_prompt_template_version")
public class PromptTemplateVersion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属模板 ID。 */
    private Long templateId;

    /** 版本号，从 1 开始自增。 */
    private Integer version;

    /** 版本状态：0-草稿，1-已发布，2-已下线。 */
    private Integer versionStatus;

    /** 12 阶段配置完整 JSON 字符串（与设计文档 §5.10 结构一致）。 */
    private String configJson;

    /** 本次发布变更说明。 */
    private String changeNote;

    /** 发布时间（草稿为 null）。 */
    private LocalDateTime publishedAt;

    /** 发布人 ID（系统发布为 0）。 */
    private Long publishedBy;

    /** 租户 ID（=0）。 */
    private Long tenantId;
}