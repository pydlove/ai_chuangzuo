package com.aichuangzuo.user.modules.learn.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArticleStatus {
    DRAFT("draft"),
    PUBLISHED("published");

    @EnumValue
    @JsonValue
    private final String code;

    @JsonCreator
    public static ArticleStatus fromCode(String code) {
        for (ArticleStatus v : values()) {
            if (v.code.equals(code)) return v;
        }
        throw new IllegalArgumentException("unknown status: " + code);
    }
}
