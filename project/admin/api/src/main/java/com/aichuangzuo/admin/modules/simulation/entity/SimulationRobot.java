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
@TableName("a_simulation_robot")
public class SimulationRobot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    /** 批次内序号。 */
    private Integer seq;

    private String email;

    /** AES 加密后的初始密码。 */
    private String passwordEncrypted;

    /** 用户端 u_user.id。 */
    private Long userId;

    /** 绑定的邀请码。 */
    private String inviteCode;

    /** LLM 生成的昵称（机器人内不重复）。 */
    private String nickname;

    /** randomuser 头像编号 1-200（机器人内不重复）。 */
    private Integer avatarImg;

    /** 状态 WAITING/IN_PROGRESS/COMPLETED/FAILED/CANCELED。 */
    private String status;

    /** 当前阶段（SimulationStage）。 */
    private String currentStage;

    /** 下一阶段允许执行时间。 */
    private LocalDateTime nextRunAt;

    /** 阶段上下文 JSON（文章/任务ID等）。 */
    private String context;

    private String failStage;
    private String failReason;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private Long tenantId;
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
