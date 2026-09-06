package com.aichuangzuo.user.modules.wechat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户微信绑定关系。
 */
@Getter
@Setter
@TableName("u_user_wechat_bind")
public class UserWechatBind {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String openid;

    private String unionid;

    private String appId;

    private String nickname;

    private String avatarUrl;

    private LocalDateTime bindTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
