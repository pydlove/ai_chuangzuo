package com.aichuangzuo.user.modules.user.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.service.EmailCodeService;
import com.aichuangzuo.user.modules.user.converter.UserConverter;
import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.service.UserProfileService;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户个人资料服务实现。
 *
 * <p>所有方法都从 {@link SecurityUserContext} 拿当前用户 ID，
 * 然后通过 {@link UserMapper} 加载实体做修改。
 *
 * <p>依赖：
 * <ul>
 *   <li>{@link UserMapper} - u_user 表读写</li>
 *   <li>{@link EmailCodeService} - 改邮箱时校验验证码</li>
 *   <li>{@link PasswordEncoder} - 改密码时 BCrypt 加解密</li>
 *   <li>{@link UserConverter} - User → UserProfileVO 映射</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final EmailCodeService emailCodeService;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;

    @Override
    public UserProfileVO getMyProfile() {
        Long userId = SecurityUserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }
        return userConverter.toProfileVO(user);
    }

    /**
     * 修改当前用户的昵称。会 trim 后再写库，避免前后空格污染展示。
     *
     * @param request 新昵称（已通过 Bean Validation，1-20 字符）
     * @return 更新后的视图对象
     * @throws BusinessException USER_NOT_FOUND
     */
    @Override
    public UserProfileVO updateNickname(UpdateNicknameRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }
        user.setNickname(request.getNickname().trim());
        userMapper.updateById(user);
        log.info("昵称已修改 userId={}", userId);
        return userConverter.toProfileVO(user);
    }

    @Override
    public UserProfileVO updateEmail(UpdateEmailRequest request) {
        // 由 Task 7 实现
        throw new UnsupportedOperationException("see Task 7");
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        // 由 Task 8 实现
        throw new UnsupportedOperationException("see Task 8");
    }
}