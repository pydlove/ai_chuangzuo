package com.aichuangzuo.admin.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端用户邀请关系详情视图。
 */
@Data
public class AdminUserInviteDetailVO {

    /** 当前用户 ID。 */
    private Long userId;

    /** 当前用户邀请码。 */
    private String inviteCode;

    /** 邀请人信息；未绑定时为 null。 */
    private AdminUserInviteeVO inviter;

    /** 被邀请人列表。 */
    private List<AdminUserInviteeVO> invitees = new ArrayList<>();
}
