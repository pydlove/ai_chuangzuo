package com.aichuangzuo.user.modules.user.vo;

import lombok.Data;

/**
 * 当前登录用户的个人资料视图。
 *
 * <p>对应 GET /api/v1/user/me 和所有更新接口的成功响应。
 *
 * <p>字段说明：
 * <ul>
 *   <li>userId - 业务编号（u_user.biz_no），前端展示用，不暴露数据库主键</li>
 *   <li>nickname - 昵称，可能为空（前端需有兜底）</li>
 *   <li>email - 当前邮箱；前端展示通常脱敏但此处原样返回，由调用方决定</li>
 *   <li>avatarUrl - 头像 URL；空时前端用通用图标兜底</li>
 *   <li>emailVerified - 0/1；改邮箱成功后置 1</li>
 *   <li>inviterUserId - 邀请人用户主键 ID；null 表示未绑定，用于前端控制"绑定邀请人"入口</li>
 * </ul>
 */
@Data
public class UserProfileVO {
    private String userId;
    private String nickname;
    private String email;
    private String avatarUrl;
    private Integer emailVerified;
    private Long inviterUserId;
}