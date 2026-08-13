package com.aichuangzuo.user.modules.generation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 生成任务额度退款记录，对应表 u_generation_task_refund。
 */
@Getter
@Setter
@TableName("u_generation_task_refund")
public class GenerationTaskRefund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long userId;

    private String benefitCode;

    private LocalDateTime refundedAt;
}
