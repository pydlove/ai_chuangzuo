package com.aichuangzuo.admin.modules.simulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_simulation_robot_log")
public class SimulationRobotLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long robotId;
    private Long batchId;

    /** 阶段（SimulationStage）。 */
    private String stage;

    /** 结果 SUCCESS/FAILED/SKIPPED。 */
    private String status;

    /** 阶段明细（奖项/文章标题/提示词/约稿任务等）。 */
    private String detail;

    private String errorMsg;

    private LocalDateTime createdAt;
}
