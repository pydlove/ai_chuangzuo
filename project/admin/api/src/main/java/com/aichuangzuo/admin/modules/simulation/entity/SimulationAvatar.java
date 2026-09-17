package com.aichuangzuo.admin.modules.simulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模拟运营-头像库：管理员上传（服务端压缩为 JPEG），PROFILE 阶段随机取用，用后即删。
 */
@Data
@TableName("a_simulation_avatar")
public class SimulationAvatar {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 头像二进制（压缩后的 JPEG） */
    private byte[] avatar;

    /** 上传时间 */
    private LocalDateTime createdAt;
}
