package com.aichuangzuo.user.modules.wechat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信绑定码响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatBindCodeVO {

    /**
     * 绑定码
     */
    private String bindCode;

    /**
     * 过期时间戳（毫秒）
     */
    private Long expireTime;
}
