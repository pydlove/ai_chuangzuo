package com.aichuangzuo.user.modules.simulation.controller;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.UnauthorizedException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.AdminJwtUtil;
import com.aichuangzuo.user.modules.simulation.dto.SimulationRobotCreateRequest;
import com.aichuangzuo.user.modules.simulation.service.SimulationUserService;
import com.aichuangzuo.user.modules.simulation.vo.RobotCreatedVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端内部接口：模拟运营（管理端模拟批次调用）。
 *
 * <p>路径前缀 /api/v1/user/internal/ 已由 InternalKeyAuthenticationFilter 校验 X-Internal-Key。
 */
@Tag(name = "内部-模拟运营")
@Slf4j
@RestController
@RequestMapping("/api/v1/user/internal/simulation")
@RequiredArgsConstructor
public class SimulationInternalController {

    private final SimulationUserService simulationUserService;
    private final AdminJwtUtil adminJwtUtil;

    @Operation(summary = "注册机器人用户")
    @PostMapping("/robots")
    public Result<RobotCreatedVO> createRobot(@RequestHeader("Authorization") String authHeader,
                                              @Valid @RequestBody SimulationRobotCreateRequest request) {
        requireInternalAdmin(authHeader);
        return Result.success(simulationUserService.createRobot(
                request.getEmail(), request.getPassword(), request.getInviteCode()));
    }

    @Operation(summary = "随机机器人邀请码")
    @GetMapping("/robot-invite-codes")
    public Result<List<String>> robotInviteCodes(@RequestHeader("Authorization") String authHeader,
                                                 @RequestParam(defaultValue = "5") int count) {
        requireInternalAdmin(authHeader);
        return Result.success(simulationUserService.randomRobotInviteCodes(Math.min(count, 50)));
    }

    private void requireInternalAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_EXPIRED);
        }
        adminJwtUtil.parseAccessToken(authHeader.substring(7));
    }
}
