package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 创作提示词模板，对应表 {@code t_prompt_template}。
 *
 * <p>模板由 1 段组成：
 * <ul>
 *   <li>{@link #baseContent} 基础内容（去 AI 味），使用 {{title}} 等占位符</li>
 * </ul>
 *
 * <p>系统提示词（规定 AI 返回 JSON schema）由 {@code PromptConstants} 维护，
 * 不再存表；用户写作风格由 {@code u_user_style.prompt} 在用户提交任务时
 * 快照到 {@code a_generation_task.input_param.userStylePrompt}，executor 拼装
 * 时直接读取。
 *
 * <p>运行时约束：全表最多 1 条 template_status=PUBLISHED，由发布事务保证。
 */
@Getter
@Setter
@TableName("t_prompt_template")
public class PromptTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称（管理后台显示）。 */
    private String name;

    /** 基础内容（去 AI 味），支持 {{title}} {{description}} {{platform}} {{wordCount}} {{userStylePrompt}} 占位符。 */
    private String baseContent;

    /** 模板状态：0-草稿，1-已发布，2-已下线。详见 {@code TemplateStatus}。 */
    private Integer templateStatus;

    /** 最新已发布版本号（草稿/未发布时为 null）。 */
    private Integer latestPublishedVersion;

    /** 备注。 */
    private String remark;

    /** 租户 ID（=0）。 */
    private Long tenantId;
}
