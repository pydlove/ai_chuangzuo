package com.aichuangzuo.user.modules.simulation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 模拟运营-机器人注册请求（仅内部接口）。
 */
@Data
public class SimulationRobotCreateRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 20)
    private String password;

    /** 可选：绑定邀请码（须为现有用户的邀请码）。 */
    private String inviteCode;
}
