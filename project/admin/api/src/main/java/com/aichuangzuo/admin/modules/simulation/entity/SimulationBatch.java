package com.aichuangzuo.admin.modules.simulation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_simulation_batch")
public class SimulationBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次编号 SIM+日期+序号。 */
    private String batchNo;

    /** 机器人数。 */
    private Integer userCount;

    /** 会员套餐 key：basic/pro/flagship。 */
    private String planKey;

    /** 会员套餐名称快照。 */
    private String planName;

    /** 订阅周期：month/quarter/year。 */
    private String cycle;

    /** 阶段配置 JSON（SimulationStageConfig）。 */
    private String stageConfig;

    /** 状态 PENDING/RUNNING/COMPLETED/CANCELED。 */
    private String status;

    private Integer totalCount;
    private Integer completedCount;
    private Integer failedCount;

    private String remark;

    private Long tenantId;
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
