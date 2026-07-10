package com.aichuangzuo.shared.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章生成任务状态。
 *
 * <p>数据库以 TINYINT 持久化，通过 {@link EnumValue} 标记。
 */
@Getter
@AllArgsConstructor
public enum GenerationTaskStatus {

    /** 已入队，等待 worker 拉取 */
    QUEUED(0, "已入队"),

    /** 正在处理 */
    PROCESSING(1, "处理中"),

    /** 已完成（已写 article） */
    COMPLETED(2, "已完成"),

    /** 失败（已重试 3 次仍失败，或非重试类失败） */
    FAILED(3, "失败");

    @EnumValue
    private final int code;

    private final String label;
}
