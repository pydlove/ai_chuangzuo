package com.aichuangzuo.user.modules.simulation.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.util.InviteCodeGenerator;
import com.aichuangzuo.user.modules.simulation.vo.RobotCreatedVO;
import com.aichuangzuo.user.modules.user.service.InviteRewardService;
import com.aichuangzuo.shared.enums.UserStatusEnum;
import com.aichuangzuo.shared.enums.VerifyStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 模拟运营-机器人用户服务。
 *
 * <p>仅由管理端模拟批次经内部接口调用，用于创建虚拟机器人账号并归类 user_type=0。
 */
@Service
@RequiredArgsConstructor
public class SimulationUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final InviteRewardService inviteRewardService;

    /**
     * 创建机器人用户：跳过邮箱验证码，强制 user_type=0、邮箱已验证。
     * 邮箱已存在时幂等返回已有账号。
     */
    @Transactional(rollbackFor = Exception.class)
    public RobotCreatedVO createRobot(String email, String password, String inviteCode) {
        String normalizedEmail = email.trim().toLowerCase();
        User existing = userMapper.selectByEmail(normalizedEmail);
        if (existing != null) {
            return new RobotCreatedVO(existing.getId(), existing.getEmail());
        }

        User user = new User();
        user.setBizNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setInviteCode(inviteCodeGenerator.generate());
        user.setUserStatus(UserStatusEnum.ENABLED.getCode());
        user.setEmailVerified(VerifyStatusEnum.VERIFIED.getCode());
        user.setUserType(0);
        user.setNickname("用户" + user.getInviteCode());
        userMapper.insert(user);

        if (StringUtils.hasText(inviteCode)) {
            inviteRewardService.rewardAfterRegister(user, inviteCode.trim().toUpperCase());
        }
        return new RobotCreatedVO(user.getId(), user.getEmail());
    }

    /**
     * 随机取 N 个现有机器人用户的邀请码（供新机器人绑定）。
     */
    public List<String> randomRobotInviteCodes(int count) {
        return userMapper.selectRobotInviteCodes(count);
    }
}
