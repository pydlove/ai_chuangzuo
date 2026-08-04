package com.aichuangzuo.user.modules.commission.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 约稿任务状态。
 */
@Getter
@RequiredArgsConstructor
public enum CommissionTaskStatus {

    SUBMISSION(0, "submission", "投稿中"),
    REVIEW(1, "review", "评审中"),
    FINISHED(2, "finished", "已结束");

    private final int code;
    private final String alias;
    private final String label;

    public Integer getCode() {
        return code;
    }

    /**
     * 根据数字 code 查找枚举。
     */
    public static CommissionTaskStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据别名（如 in_progress / submission）查找对应的 code 列表。
     * <p>in_progress 表示"进行中"，包含投稿中和评审中。
     */
    public static List<Integer> resolveCodes(String alias) {
        if (alias == null || alias.isBlank()) {
            return Collections.emptyList();
        }
        String key = alias.trim().toLowerCase();
        if ("in_progress".equals(key) || "in-progress".equals(key) || "inprogress".equals(key)) {
            return List.of(SUBMISSION.code, REVIEW.code);
        }
        return Arrays.stream(values())
                .filter(s -> s.alias.equals(key))
                .map(CommissionTaskStatus::getCode)
                .findFirst()
                .map(List::of)
                .orElseGet(() -> {
                    // 兼容直接传数字字符串
                    try {
                        return List.of(Integer.parseInt(key));
                    } catch (NumberFormatException e) {
                        return Collections.emptyList();
                    }
                });
    }
}
