package com.aichuangzuo.user.modules.membership.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订阅价格预览请求。
 */
@Data
public class SubscribePreviewRequest {

    /** 套餐：basic / pro / flagship。 */
    @NotBlank(message = "套餐不能为空")
    private String planKey;

    /** 周期：month / quarter / year。 */
    @NotBlank(message = "订阅周期不能为空")
    private String cycle;
}
