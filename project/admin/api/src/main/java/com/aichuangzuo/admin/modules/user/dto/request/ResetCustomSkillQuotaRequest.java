package com.aichuangzuo.admin.modules.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 重置用户自定义提示词额度请求。
 */
@Getter
@Setter
public class ResetCustomSkillQuotaRequest {

    /** 要释放的额度数量。 */
    @NotNull(message = "释放数量不能为空")
    @Min(value = 1, message = "释放数量至少为 1")
    @Max(value = 100, message = "释放数量不能超过 100")
    private Integer count;
}
