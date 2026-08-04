package com.aichuangzuo.user.modules.membership.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 升级预览请求。
 */
@Data
public class UpgradePreviewRequest {

    /** 目标套餐：basic / pro / flagship。 */
    @NotBlank(message = "套餐不能为空")
    private String planKey;

    /** 目标周期：month / quarter / year。 */
    @NotBlank(message = "订阅周期不能为空")
    private String cycle;
}
