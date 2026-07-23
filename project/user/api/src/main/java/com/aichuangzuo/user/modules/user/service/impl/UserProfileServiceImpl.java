package com.aichuangzuo.user.modules.user.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
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

    /** 密码长度下限。service 层集中校验，方便调整时只改一处。 */
    private static final int MIN_PASSWORD_LENGTH = 6;
    /** 密码长度上限；与 register 接口保持一致。 */
    private static final int MAX_PASSWORD_LENGTH = 20;

    private final UserMapper userMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
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
        return fillInviter(userConverter.toProfileVO(user), user.getId());
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
        return fillInviter(userConverter.toProfileVO(user), user.getId());
    }

    /**
     * 修改当前用户的邮箱。
     *
     * <p>流程：
     * <ol>
     *   <li>校验新邮箱收到的验证码（一次性，验证后失效）</li>
     *   <li>不允许新邮箱与旧邮箱相同</li>
     *   <li>新邮箱不能已被他人注册</li>
     *   <li>写入新邮箱并把 email_verified 置 1</li>
     * </ol>
     *
     * @param request 新邮箱 + 6 位验证码
     * @return 更新后的视图对象
     * @throws BusinessException EMAIL_CODE_ERROR / EMAIL_SAME_AS_OLD / EMAIL_ALREADY_EXISTS / USER_NOT_FOUND
     */
    @Override
    public UserProfileVO updateEmail(UpdateEmailRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }
        String newEmail = request.getNewEmail().trim().toLowerCase();

        if (!emailCodeService.validateEmailCode(newEmail, request.getEmailCode())) {
            throw new BusinessException(UserAuthErrorCode.EMAIL_CODE_ERROR);
        }
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(UserAuthErrorCode.EMAIL_SAME_AS_OLD);
        }
        if (userMapper.existsByEmail(newEmail, userId)) {
            throw new BusinessException(UserAuthErrorCode.EMAIL_ALREADY_EXISTS);
        }
        user.setEmail(newEmail);
        user.setEmailVerified(1);
        userMapper.updateById(user);
        log.info("邮箱已修改 userId={}, newEmail={}", userId, newEmail);
        return fillInviter(userConverter.toProfileVO(user), user.getId());
    }

    /**
     * 修改当前用户的密码。需要原密码校验通过，新密码长度 ≥6 且 ≤20，新密码两次一致。
     *
     * <p>成功后仅更新密码字段，不签发新 token —— 客户端继续使用旧 access token。
     *
     * @param request 旧/新/确认密码
     * @throws BusinessException ACCOUNT_DISABLED / PASSWORD_INCORRECT / PASSWORD_FORMAT_ERROR / PASSWORD_NOT_MATCH / USER_NOT_FOUND
     */
    @Override
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }
        if (user.getUserStatus() == null || user.getUserStatus() != 1) {
            throw new BusinessException(UserAuthErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(UserAuthErrorCode.PASSWORD_INCORRECT);
        }
        String newPwd = request.getNewPassword();
        if (newPwd.length() < MIN_PASSWORD_LENGTH || newPwd.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(UserAuthErrorCode.PASSWORD_FORMAT_ERROR);
        }
        if (!newPwd.equals(request.getConfirmPassword())) {
            throw new BusinessException(UserAuthErrorCode.PASSWORD_NOT_MATCH);
        }
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
        log.info("密码已修改 userId={}", userId);
    }

    /**
     * 把邀请人信息回填到视图对象。
     *
     * <p>u_user 表不存邀请关系，需要从 u_user_invite_relation 查询；
     * 拿到 inviter_id 后再查 u_user 取昵称用于展示。若邀请人未设置昵称，
     * 则退化为邮箱，确保前端始终能展示可识别的邀请人信息。
     *
     * @param vo     已转换的 UserProfileVO
     * @param userId 当前用户主键 ID
     * @return 回填后的 VO
     */
    private UserProfileVO fillInviter(UserProfileVO vo, Long userId) {
        if (vo == null) {
            return null;
        }
        UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(userId);
        if (relation != null) {
            vo.setInviterUserId(relation.getInviterId());
            User inviter = userMapper.selectById(relation.getInviterId());
            if (inviter != null) {
                String displayName = inviter.getNickname();
                if (displayName == null || displayName.isBlank()) {
                    displayName = inviter.getEmail();
                }
                vo.setInviterNickname(displayName);
            }
        }
        return vo;
    }
}