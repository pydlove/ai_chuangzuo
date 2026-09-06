package com.aichuangzuo.user.modules.wechat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信绑定二维码响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatBindQrVO {

    /**
     * 二维码图片 URL
     */
    private String qrCodeUrl;

    /**
     * 场景值，前端轮询绑定状态时使用
     */
    private String sceneStr;

    /**
     * 过期时间戳（毫秒）
     */
    private Long expireTime;
}
