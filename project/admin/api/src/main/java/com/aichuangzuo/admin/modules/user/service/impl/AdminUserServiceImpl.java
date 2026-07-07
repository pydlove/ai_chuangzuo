package com.aichuangzuo.admin.modules.user.service.impl;

import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserLoginLogMapper;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.admin.modules.user.service.AdminUserService;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final PlatformUserMapper platformUserMapper;
    private final PlatformUserLoginLogMapper platformUserLoginLogMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String RESET_PASSWORD = "adc123456";

    @Override
    public AdminUserPageVO listUsers(String keyword, int page, int pageSize) {
        Page<PlatformUser> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformUser::getIsDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(PlatformUser::getEmail, kw)
                    .or()
                    .like(PlatformUser::getNickname, kw)
                    .or()
                    .like(PlatformUser::getInviteCode, kw));
        }
        wrapper.orderByDesc(PlatformUser::getCreatedAt);
        Page<PlatformUser> result = platformUserMapper.selectPage(pageParam, wrapper);
        List<AdminUserVO> list = result.getRecords().stream()
                .map(this::toAdminUserVO)
                .collect(Collectors.toList());
        AdminUserPageVO vo = new AdminUserPageVO();
        vo.setList(list);
        vo.setTotal(result.getTotal());
        return vo;
    }

    @Override
    public AdminUserVO getUser(Long id) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        return toAdminUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, AdminUserStatusRequest request) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        int status = "enabled".equals(request.getStatus()) ? 1 : 0;
        user.setUserStatus(status);
        platformUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResetPasswordVO resetPassword(Long id) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        user.setPasswordHash(passwordEncoder.encode(RESET_PASSWORD));
        platformUserMapper.updateById(user);
        AdminUserResetPasswordVO vo = new AdminUserResetPasswordVO();
        vo.setNewPassword(RESET_PASSWORD);
        return vo;
    }

    private AdminUserVO toAdminUserVO(PlatformUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setAccount(user.getEmail());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getUserStatus() == 1 ? "enabled" : "disabled");
        vo.setInviteCode(user.getInviteCode());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(platformUserLoginLogMapper.selectLastLoginAtByUserId(user.getId()));
        return vo;
    }
}
