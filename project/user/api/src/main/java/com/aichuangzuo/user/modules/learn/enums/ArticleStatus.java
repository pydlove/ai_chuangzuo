package com.aichuangzuo.user.modules.learn.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArticleStatus {
    DRAFT("draft"),
    PUBLISHED("published");

    @EnumValue
    private final String code;

    public static ArticleStatus fromCode(String code) {
        for (ArticleStatus v : values()) {
            if (v.code.equals(code)) return v;
        }
        throw new IllegalArgumentException("unknown status: " + code);
    }
}
