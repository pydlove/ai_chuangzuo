package com.aichuangzuo.admin.modules.user.service;

import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserLoginLogMapper;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.admin.modules.user.service.impl.AdminUserServiceImpl;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

        AdminUserPageVO result = adminUserService.listUsers("", 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("user@example.com", result.getList().get(0).getAccount());
        assertEquals("enabled", result.getList().get(0).getStatus());
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
        when(passwordEncoder.encode("adc123456")).thenReturn("hashed");

        AdminUserResetPasswordVO result = adminUserService.resetPassword(1L);

        assertEquals("adc123456", result.getNewPassword());
        verify(platformUserMapper).updateById(user);
        assertEquals("hashed", user.getPasswordHash());
    }
}
