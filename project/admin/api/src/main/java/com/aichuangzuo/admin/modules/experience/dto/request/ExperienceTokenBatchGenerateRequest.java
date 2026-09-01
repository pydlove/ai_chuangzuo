package com.aichuangzuo.admin.modules.experience.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExperienceTokenBatchGenerateRequest {

    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量至少为 1")
    @Max(value = 1000, message = "单次最多生成 1000 个")
    private Integer count;

    @NotNull(message = "会员天数不能为空")
    @Min(value = 1, message = "会员天数必须大于 0")
    private Integer membershipDays;

    @NotNull(message = "套餐类型不能为空")
    @Pattern(regexp = "basic|pro|flagship", message = "套餐类型只能是 basic、pro 或 flagship")
    private String planKey;

    private LocalDateTime expiresAt;
}
