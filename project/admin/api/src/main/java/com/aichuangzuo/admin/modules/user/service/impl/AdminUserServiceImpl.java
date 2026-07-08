package com.aichuangzuo.admin.modules.user.service.impl;

import com.aichuangzuo.admin.modules.user.dto.request.AdminUserCreateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserUpdateRequest;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserLoginLogMapper;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.admin.modules.user.service.AdminUserService;
import com.aichuangzuo.admin.modules.user.vo.AdminUserOptionVO;
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
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final PlatformUserMapper platformUserMapper;
    private final PlatformUserLoginLogMapper platformUserLoginLogMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String RESET_PASSWORD = "adc123456";
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO createUser(AdminUserCreateRequest request) {
        String email = request.getEmail().trim();
        if (!StringUtils.hasText(request.getNickname())) {
            throw new BusinessException(AdminUserErrorCode.NICKNAME_FORMAT_ERROR);
        }
        if (request.getUserType() == null || (request.getUserType() != 0 && request.getUserType() != 1)) {
            throw new BusinessException(AdminUserErrorCode.USER_TYPE_INVALID);
        }

        LambdaQueryWrapper<PlatformUser> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(PlatformUser::getEmail, email).eq(PlatformUser::getIsDeleted, 0);
        if (platformUserMapper.selectCount(existsWrapper) > 0) {
            throw new BusinessException(AdminUserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String password = StringUtils.hasText(request.getPassword()) ? request.getPassword().trim() : RESET_PASSWORD;
        if (password.length() < 6 || password.length() > 32) {
            throw new BusinessException(AdminUserErrorCode.PASSWORD_FORMAT_ERROR);
        }

        PlatformUser user = new PlatformUser();
        user.setBizNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setEmail(email);
        user.setNickname(request.getNickname().trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setInviteCode(generateInviteCode());
        user.setUserStatus(1);
        user.setUserType(request.getUserType());
        user.setEmailVerified(1);
        user.setTenantId(0L);
        user.setIsDeleted(0);
        platformUserMapper.insert(user);

        return toAdminUserVO(user);
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            String code = sb.toString();
            LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PlatformUser::getInviteCode, code).eq(PlatformUser::getIsDeleted, 0);
            if (platformUserMapper.selectCount(wrapper) == 0) {
                return code;
            }
        }
        throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateUser(Long id, AdminUserUpdateRequest request) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        if (request.getUserType() == null || (request.getUserType() != 0 && request.getUserType() != 1)) {
            throw new BusinessException(AdminUserErrorCode.USER_TYPE_INVALID);
        }

        String email = request.getEmail().trim();
        LambdaQueryWrapper<PlatformUser> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(PlatformUser::getEmail, email)
                .eq(PlatformUser::getIsDeleted, 0)
                .ne(PlatformUser::getId, id);
        if (platformUserMapper.selectCount(existsWrapper) > 0) {
            throw new BusinessException(AdminUserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.setEmail(email);
        user.setNickname(request.getNickname().trim());
        user.setUserStatus("enabled".equals(request.getStatus()) ? 1 : 0);
        user.setUserType(request.getUserType());
        user.setMembershipExpireAt(request.getExpireDate() == null ? null : request.getExpireDate().plusDays(1).atStartOfDay());
        user.setMembershipPlan(request.getMembershipPlan());
        platformUserMapper.updateById(user);
        return toAdminUserVO(user);
    }

    @Override
    public List<AdminUserOptionVO> listUserOptions(String keyword, int limit) {
        LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformUser::getIsDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(PlatformUser::getEmail, kw)
                    .or()
                    .like(PlatformUser::getNickname, kw));
        }
        wrapper.orderByDesc(PlatformUser::getCreatedAt);
        wrapper.last("LIMIT " + limit);
        List<PlatformUser> users = platformUserMapper.selectList(wrapper);
        return users.stream()
                .map(this::toAdminUserOptionVO)
                .collect(Collectors.toList());
    }

    private AdminUserVO toAdminUserVO(PlatformUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setAccount(user.getEmail());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getUserStatus() == 1 ? "enabled" : "disabled");
        vo.setUserType(user.getUserType() != null && user.getUserType() == 0 ? "robot" : "real");
        vo.setInviteCode(user.getInviteCode());
        vo.setMembershipExpireAt(user.getMembershipExpireAt());
        vo.setMembershipPlan(user.getMembershipPlan());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(platformUserLoginLogMapper.selectLastLoginAtByUserId(user.getId()));
        return vo;
    }

    private AdminUserOptionVO toAdminUserOptionVO(PlatformUser user) {
        AdminUserOptionVO vo = new AdminUserOptionVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        return vo;
    }
}
