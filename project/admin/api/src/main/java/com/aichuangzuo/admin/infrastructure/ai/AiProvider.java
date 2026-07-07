package com.aichuangzuo.admin.infrastructure.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum AiProvider {
    KIMI("kimi", "Kimi"),
    MINIMAX("minimax", "MiniMax");

    private final String code;
    private final String name;

    public static Optional<AiProvider> fromCode(String code) {
        return Arrays.stream(values())
                .filter(p -> p.code.equalsIgnoreCase(code))
                .findFirst();
    }

    public static List<AiProvider> all() {
        return Arrays.asList(values());
    }
}
