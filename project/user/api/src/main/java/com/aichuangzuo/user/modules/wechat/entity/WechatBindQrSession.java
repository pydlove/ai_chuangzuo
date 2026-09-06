package com.aichuangzuo.user.modules.wechat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 微信绑定二维码会话。
 */
@Getter
@Setter
@TableName("u_wechat_bind_qr_session")
public class WechatBindQrSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sceneStr;

    private Long userId;

    /**
     * 状态：0待扫描 1已扫描 2已绑定
     */
    private Integer status;

    private String openid;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
