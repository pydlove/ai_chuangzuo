package com.aichuangzuo.user.modules.wechat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信绑定状态响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatBindStatusVO {

    /**
     * 是否已绑定
     */
    private Boolean bound;

    /**
     * 状态：0待扫描 1已扫描 2已绑定 -1已过期
     */
    private Integer status;

    /**
     * 微信昵称（已绑定时返回）
     */
    private String nickname;

    /**
     * 微信头像（已绑定时返回）
     */
    private String avatarUrl;
}
