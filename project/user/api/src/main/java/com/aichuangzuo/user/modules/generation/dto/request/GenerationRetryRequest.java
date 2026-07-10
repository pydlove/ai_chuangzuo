package com.aichuangzuo.user.modules.generation.dto.request;

import lombok.Data;

/** 用户「重新生成」请求：写入新 task，复用失败任务的 input_param。 */
@Data
public class GenerationRetryRequest {
    /** 可选；不传则复用原失败 task 的 input_param 与 word_count。 */
    private Long sourceTaskId;
}
