package com.aichuangzuo.admin.modules.learn.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContentType {
    MARKDOWN("markdown"),
    RICH_TEXT("rich_text");

    @EnumValue
    private final String code;

    public static ContentType fromCode(String code) {
        for (ContentType v : values()) {
            if (v.code.equals(code)) return v;
        }
        throw new IllegalArgumentException("unknown content type: " + code);
    }
}
