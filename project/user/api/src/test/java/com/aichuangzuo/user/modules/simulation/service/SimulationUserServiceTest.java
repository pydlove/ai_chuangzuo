package com.aichuangzuo.user.modules.simulation.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.util.InviteCodeGenerator;
import com.aichuangzuo.user.modules.simulation.vo.RobotCreatedVO;
import com.aichuangzuo.user.modules.user.service.InviteRewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimulationUserServiceTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private InviteCodeGenerator inviteCodeGenerator;
    private InviteRewardService inviteRewardService;
    private SimulationUserService simulationUserService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        inviteCodeGenerator = mock(InviteCodeGenerator.class);
        inviteRewardService = mock(InviteRewardService.class);
        simulationUserService = new SimulationUserService(
                userMapper, passwordEncoder, inviteCodeGenerator, inviteRewardService);
    }

    @Test
    void createRobot_setsUserTypeZeroAndVerifiedEmail() {
        when(inviteCodeGenerator.generate()).thenReturn("ABCD23");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        org.mockito.Mockito.doAnswer(inv -> {
            inv.<User>getArgument(0).setId(1L);
            return null;
        }).when(userMapper).insert(any(User.class));

        RobotCreatedVO vo = simulationUserService.createRobot("BotSim1@X.SimRobot.com", "pass123456", null);

        assertNotNull(vo.userId());
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertEquals(0, saved.getUserType());
        assertEquals(1, saved.getEmailVerified());
        assertEquals(1, saved.getUserStatus());
        assertEquals("botsim1@x.simrobot.com", saved.getEmail());
        assertEquals("hashed", saved.getPasswordHash());
        verify(inviteRewardService, never()).rewardAfterRegister(any(), any());
    }

    @Test
    void createRobot_withInviteCode_rewardsInviter() {
        when(inviteCodeGenerator.generate()).thenReturn("ABCD23");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        simulationUserService.createRobot("bot2@test.simrobot.com", "pass123456", "abcd23");

        verify(inviteRewardService).rewardAfterRegister(any(User.class), eq("ABCD23"));
    }

    @Test
    void createRobot_duplicateEmail_returnsExistingUser() {
        User existing = new User();
        existing.setId(99L);
        existing.setEmail("dup@test.simrobot.com");
        when(userMapper.selectByEmail("dup@test.simrobot.com")).thenReturn(existing);

        RobotCreatedVO vo = simulationUserService.createRobot("dup@test.simrobot.com", "pass123456", null);

        assertEquals(99L, vo.userId());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void randomRobotInviteCodes_delegatesToMapper() {
        when(userMapper.selectRobotInviteCodes(5)).thenReturn(List.of("AAAA11", "BBBB22"));

        List<String> codes = simulationUserService.randomRobotInviteCodes(5);

        assertEquals(2, codes.size());
        assertEquals("AAAA11", codes.get(0));
    }
}
