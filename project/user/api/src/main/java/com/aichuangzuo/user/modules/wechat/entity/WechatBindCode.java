package com.aichuangzuo.user.modules.wechat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 微信绑定码。
 */
@Getter
@Setter
@TableName("u_wechat_bind_code")
public class WechatBindCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String bindCode;

    private String openid;

    private String appId;

    /**
     * 状态：0待绑定 1已激活 2已绑定
     */
    private Integer status;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
