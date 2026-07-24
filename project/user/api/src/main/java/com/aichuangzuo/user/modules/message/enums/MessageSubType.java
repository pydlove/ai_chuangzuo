package com.aichuangzuo.user.modules.message.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 消息子类型枚举。
 *
 * <p>用于在 msgType 之下做更细的区分,目前主要服务于 membership 类消息:
 * subscribed(订阅成功) / expiring(到期提醒) / invite_reward(邀请会员奖励到账)。
 *
 * <p>code 只存后缀,msg_type 已经携带主分类信息,不冗余。
 */
@Getter
public enum MessageSubType {

    /** 订阅成功通知(membership 类下)。 */
    SUBSCRIBED("subscribed", "订阅成功"),

    /** 到期提醒(membership 类下)。 */
    EXPIRING("expiring", "到期提醒"),

    /** 邀请奖励会员天数到账(membership 类下)。 */
    INVITE_REWARD("invite_reward", "邀请会员奖励到账");

    private final String code;
    private final String description;

    MessageSubType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据 code 解析,未匹配返回 null(防御性,不抛异常)。
     */
    public static MessageSubType of(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(t -> t.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
