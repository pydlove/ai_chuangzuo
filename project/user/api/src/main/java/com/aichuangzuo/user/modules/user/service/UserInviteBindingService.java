package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.user.modules.user.dto.request.BindInviteCodeRequest;

/**
 * 邀请人绑定服务。
 *
 * <p>提供注册后的“补绑邀请人”能力。
 */
public interface UserInviteBindingService {

    /**
     * 为当前登录用户绑定邀请人。
     *
     * @param request 邀请码请求
     */
    void bindInviteCode(BindInviteCodeRequest request);
}
