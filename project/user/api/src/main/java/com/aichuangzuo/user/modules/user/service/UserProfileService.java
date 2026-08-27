package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdatePhoneRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateProfileRequest;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

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
     * 修改个人资料（昵称、简介、性别、生日、所在地、职业）。
     *
     * @param request 个人资料请求（已通过 Bean Validation）
     * @return 更新后的 UserProfileVO
     * @throws com.aichuangzuo.shared.exception.BusinessException USER_NOT_FOUND
     */
    UserProfileVO updateProfile(UpdateProfileRequest request);

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
     * 修改手机号。需要新手机号已收到短信验证码。
     *
     * @param request 新手机号 + 6 位验证码（已通过 Bean Validation）
     * @return 更新后的 UserProfileVO（phone_verified 置 1）
     * @throws com.aichuangzuo.shared.exception.BusinessException SMS_CODE_ERROR / PHONE_ALREADY_EXISTS / PHONE_SAME_AS_OLD / USER_NOT_FOUND
     */
    UserProfileVO updatePhone(UpdatePhoneRequest request);

    /**
     * 上传头像。
     *
     * @param file 头像文件
     * @return 更新后的 UserProfileVO
     * @throws com.aichuangzuo.shared.exception.BusinessException AVATAR_FILE_INVALID / USER_NOT_FOUND
     */
    UserProfileVO updateAvatar(MultipartFile file);

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