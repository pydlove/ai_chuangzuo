package com.aichuangzuo.admin.modules.simulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_simulation_stats_filter")
public class SimulationStatsFilter {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计是否包含机器人数据：1-包含 0-排除。 */
    private Integer includeRobots;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
