package com.aichuangzuo.admin.modules.user.service;

import com.aichuangzuo.admin.modules.user.dto.request.AdminUserCreateRequest;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserLoginLogMapper;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.admin.modules.user.service.impl.AdminUserServiceImpl;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private PlatformUserMapper platformUserMapper;

    @Mock
    private PlatformUserLoginLogMapper platformUserLoginLogMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void listUsers_shouldReturnPage() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setNickname("test");
        user.setInviteCode("ABC123");
        user.setUserStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsDeleted(0);

        Page<PlatformUser> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(user));
        page.setTotal(1);

        when(platformUserMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());

        AdminUserPageVO result = adminUserService.listUsers("", null, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("user@example.com", result.getList().get(0).getAccount());
        assertEquals("enabled", result.getList().get(0).getStatus());
    }

    @Test
    void listUsers_withInviteCode_shouldFilterByExactInviteCode() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setNickname("test");
        user.setInviteCode("ABC123");
        user.setUserStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsDeleted(0);

        Page<PlatformUser> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(user));
        page.setTotal(1);

        when(platformUserMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());

        AdminUserPageVO result = adminUserService.listUsers(null, "ABC123", 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals("ABC123", result.getList().get(0).getInviteCode());

        ArgumentCaptor<LambdaQueryWrapper<PlatformUser>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(platformUserMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<PlatformUser> captured = wrapperCaptor.getValue();
        assertNotNull(captured);
    }

    @Test
    void getUser_shouldThrowWhenNotFound() {
        when(platformUserMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.getUser(999L));
        assertEquals(AdminUserErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void resetPassword_shouldReturnFixedPassword() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setIsDeleted(0);
        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserResetPasswordVO result = adminUserService.resetPassword(1L);

        assertEquals("Aichuangzuo@123", result.getNewPassword());
        verify(platformUserMapper).updateById(user);
        assertEquals("hashed", user.getPasswordHash());
    }

    @Test
    void createUser_realUser_shouldInsertWithDefaults() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("new@example.com");
        request.setNickname("新用户");
        request.setUserType(1);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserVO result = adminUserService.createUser(request);

        assertEquals("new@example.com", result.getEmail());
        assertEquals("新用户", result.getNickname());
        assertEquals("real", result.getUserType());
        assertEquals("enabled", result.getStatus());

        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        PlatformUser saved = captor.getValue();
        assertNotNull(saved.getBizNo());
        assertTrue(saved.getBizNo().startsWith("U"));
        assertEquals("hashed", saved.getPasswordHash());
        assertEquals(1, saved.getUserStatus());
        assertEquals(1, saved.getUserType());
        assertEquals(1, saved.getEmailVerified());
        assertNotNull(saved.getInviteCode());
        assertEquals(6, saved.getInviteCode().length());
    }

    @Test
    void createUser_robotUser_shouldMarkRobot() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("robot@example.com");
        request.setNickname("机器人");
        request.setUserType(0);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserVO result = adminUserService.createUser(request);

        assertEquals("robot", result.getUserType());
        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getUserType());
    }

    @Test
    void createUser_duplicateEmail_shouldThrow() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("dup@example.com");
        request.setNickname("重复");
        request.setUserType(1);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.createUser(request));
        assertEquals(AdminUserErrorCode.EMAIL_ALREADY_EXISTS.getCode(), ex.getCode());
        verify(platformUserMapper, never()).insert(any(PlatformUser.class));
    }

    @Test
    void createUser_invalidUserType_shouldThrow() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("type@example.com");
        request.setNickname("类型错误");
        request.setUserType(2);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.createUser(request));
        assertEquals(AdminUserErrorCode.USER_TYPE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void createUser_customPassword_shouldUseProvidedPassword() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("custom@example.com");
        request.setNickname("自定义密码");
        request.setPassword("mySecret123");
        request.setUserType(1);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("mySecret123")).thenReturn("customHash");

        adminUserService.createUser(request);

        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        assertEquals("customHash", captor.getValue().getPasswordHash());
    }
}
