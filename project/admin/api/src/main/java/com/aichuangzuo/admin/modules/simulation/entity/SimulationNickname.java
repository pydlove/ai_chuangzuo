package com.aichuangzuo.admin.modules.simulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模拟运营-昵称库：管理员上传，PROFILE 阶段随机取用，用后即删。
 */
@Data
@TableName("a_simulation_nickname")
public class SimulationNickname {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 上传时间 */
    private LocalDateTime createdAt;
}
