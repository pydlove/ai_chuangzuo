package com.aichuangzuo.user.modules.message.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息范围枚举。
 */
@Getter
@RequiredArgsConstructor
public enum MessageScope {

    /** 广播：发送给全体用户。 */
    BROADCAST(1, "广播"),

    /** 个人：发送给单个用户。 */
    PERSONAL(2, "个人");

    private final int code;
    private final String desc;
}
