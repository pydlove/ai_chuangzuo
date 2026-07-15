package com.aichuangzuo.admin.modules.generation.service;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 单次调用结果：内容 + token 消耗。
 *
 * <p>{@code GenerationAiService.call} 的返回类型；token 字段失败时为 null。
 */
@Data
@AllArgsConstructor
public class AiCallResult {

    /** AI 返回的 assistant content（非空）。 */
    private final String content;

    /** prompt tokens（provider 上报；未上报为 null）。 */
    private final Integer promptTokens;

    /** completion tokens（provider 上报；未上报为 null）。 */
    private final Integer completionTokens;

    /** 总 tokens = prompt + completion（provider 上报；未上报为 null）。 */
    private final Integer totalTokens;
}
