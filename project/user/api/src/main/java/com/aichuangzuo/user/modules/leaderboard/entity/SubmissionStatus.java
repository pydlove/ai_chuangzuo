package com.aichuangzuo.user.modules.leaderboard.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 自媒体收入申报审核状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum SubmissionStatus {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String desc;
}
