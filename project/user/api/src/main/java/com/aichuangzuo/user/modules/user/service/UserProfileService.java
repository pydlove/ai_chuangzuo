package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;

/**
 * 用户个人资料服务：查询与修改当前登录用户的基本信息。
 *
 * <p>所有方法都依赖 {@code SecurityUserContext.getCurrentUserId()} 拿到当前用户，
 * 不接受外部传入 userId，避免越权。
 */
public interface UserProfileService {

    /**
     * 查询当前登录用户的个人资料。
     *
     * @return UserProfileVO
     * @throws com.aichuangzuo.shared.exception.BusinessException USER_NOT_FOUND 当用户不存在或已被删除
     */
    UserProfileVO getMyProfile();

    /**
     * 修改昵称。
     *
     * @param request 新昵称请求（已通过 Bean Validation）
     * @return 更新后的 UserProfileVO
     * @throws com.aichuangzuo.shared.exception.BusinessException USER_NOT_FOUND
     */
    UserProfileVO updateNickname(UpdateNicknameRequest request);

    /**
     * 修改邮箱。需要新邮箱已收到验证码。
     *
     * @param request 新邮箱 + 6 位验证码（已通过 Bean Validation）
     * @return 更新后的 UserProfileVO（email_verified 置 1）
     * @throws com.aichuangzuo.shared.exception.BusinessException EMAIL_CODE_ERROR / EMAIL_ALREADY_EXISTS / EMAIL_SAME_AS_OLD / USER_NOT_FOUND
     */
    UserProfileVO updateEmail(UpdateEmailRequest request);

    /**
     * 修改密码。需要原密码校验通过。
     *
     * <p>成功后不会自动签发新 token —— 客户端继续使用旧 access token，
     * 下次 token 过期时通过 refresh-token 流程拿到新 token。
     *
     * @param request 旧/新/确认密码
     * @throws com.aichuangzuo.shared.exception.BusinessException PASSWORD_INCORRECT / PASSWORD_FORMAT_ERROR / PASSWORD_NOT_MATCH / USER_NOT_FOUND
     */
    void changePassword(ChangePasswordRequest request);
}