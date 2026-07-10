package com.aichuangzuo.admin.modules.generation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 创作 AI 调用日志，对应表 {@code a_generation_call_log}。
 *
 * <p>每次 worker 跑完流水线后批量插入；记录每次 AI 调用的 stage / 尝试 / 成功 / 错误 / 耗时。
 * 用于排查「这任务为什么失败 / 这次 AI 调了多久」。
 */
@Getter
@Setter
@TableName("a_generation_call_log")
public class GenerationCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属任务 ID。 */
    private Long taskId;

    /** 阶段序号 1-12。 */
    private Integer stageIndex;

    /** 阶段稳定标识符。 */
    private String stageName;

    /** 第几次尝试。 */
    private Integer attempt;

    /** 是否成功。 */
    private Integer success;

    /** 失败原因（成功时为 null）。 */
    private String error;

    /** 耗时 ms。 */
    private Integer durationMs;

    /** 调用开始时间。 */
    private LocalDateTime calledAt;

    /** userMsg 前 200 字。 */
    private String userMsgPreview;

    /** AI 返回前 200 字（成功时）。 */
    private String responsePreview;

    private Long tenantId;
    private LocalDateTime createdAt;
}
