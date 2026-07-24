package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.dto.request.RegisterRequest;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserInviteRelationMapper userInviteRelationMapper;

    @Autowired
    private UserCoinRecordMapper userCoinRecordMapper;

    @MockBean
    private EmailCodeService emailCodeService;

    @Test
    void shouldRegisterNewUserSuccessfully() {
        String email = "register_test@example.com";
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setEmailCode("000000");
        request.setPassword("123456");
        request.setConfirmPassword("123456");

        AuthTokenVO token = authService.register(request, "127.0.0.1", "test-agent");
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertEquals(7200, token.getExpiresIn());
        assertNotNull(token.getUser());
    }

    @Test
    void shouldRegisterAfterSoftDeletedUserWithSameEmail() {
        String email = "deleted_then_register@example.com";
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        User deletedUser = new User();
        deletedUser.setEmail(email);
        deletedUser.setPasswordHash("$2a$10$dummyhashdummyhashdummyhashdummyhashdummyha");
        deletedUser.setBizNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        deletedUser.setInviteCode(UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        deletedUser.setUserStatus(1);
        deletedUser.setEmailVerified(1);
        userMapper.insert(deletedUser);
        userMapper.deleteById(deletedUser.getId());

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setEmailCode("000000");
        request.setPassword("123456");
        request.setConfirmPassword("123456");

        AuthTokenVO token = authService.register(request, "127.0.0.1", "test-agent");
        assertNotNull(token);
        assertNotNull(token.getUser());
        assertNotNull(userMapper.selectByEmail(email), "同一邮箱在软删除后应能重新注册");
    }

    @Test
    void shouldRejectInvalidInviteCodeBeforeInsert() {
        String email = "invalid_invite@example.com";
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setEmailCode("000000");
        request.setPassword("123456");
        request.setConfirmPassword("123456");
        request.setInviteCode("NOTEXIST");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request, "127.0.0.1", "test-agent"));
        assertEquals(UserAuthErrorCode.INVITE_CODE_INVALID.getCode(), ex.getCode());

        assertNull(userMapper.selectByEmail(email), "邀请码校验应在插入用户前失败，数据库不应出现该邮箱");
    }

    @Test
    void shouldGrantInviteRewardsWhenRegisterWithValidInviteCode() {
        User inviter = new User();
        inviter.setEmail("inviter-register@example.com");
        inviter.setPasswordHash("$2a$10$dummyhashdummyhashdummyhashdummyhashdummyha");
        inviter.setBizNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        inviter.setInviteCode(UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        inviter.setUserStatus(1);
        inviter.setEmailVerified(1);
        userMapper.insert(inviter);

        String email = "invitee-register@example.com";
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setEmailCode("000000");
        request.setPassword("123456");
        request.setConfirmPassword("123456");
        request.setInviteCode(inviter.getInviteCode());

        AuthTokenVO token = authService.register(request, "127.0.0.1", "test-agent");
        assertNotNull(token);

        User invitee = userMapper.selectByEmail(email);
        assertNotNull(invitee);
        assertEquals(0, invitee.getCoinBalance().compareTo(new BigDecimal("5")));

        UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(invitee.getId());
        assertNotNull(relation);
        assertEquals(inviter.getId(), relation.getInviterId());
        assertEquals(1, relation.getEffectiveStatus());

        assertEquals(1L, userCoinRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord>()
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getUserId, invitee.getId())
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getBizType, "invite_register_reward")));
    }
}
