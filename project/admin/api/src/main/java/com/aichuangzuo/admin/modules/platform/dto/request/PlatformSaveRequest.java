package com.aichuangzuo.admin.modules.platform.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 自媒体平台保存请求。
 */
@Data
public class PlatformSaveRequest {

    @NotBlank(message = "平台 key 不能为空")
    @Size(max = 32, message = "平台 key 长度不能超过 32")
    private String platformKey;

    @NotBlank(message = "平台名称不能为空")
    @Size(max = 64, message = "平台名称长度不能超过 64")
    private String platformName;

    @Size(max = 256, message = "平台简介长度不能超过 256")
    private String description;

    @Min(value = 0, message = "推荐字数不能为负数")
    @Max(value = 3000, message = "推荐字数不能超过 3000")
    private Integer recommendWords;

    @Size(max = 512, message = "平台特征长度不能超过 512")
    private String trait;

    @Valid
    private List<PlatformWordCountPresetItem> wordCountPresets;

    @Size(max = 256, message = "一句话卖点长度不能超过 256")
    private String tagline;

    @Size(max = 20, message = "内容形式最多 20 项")
    private List<String> contentForm;

    @Size(max = 20, message = "主要收益最多 20 项")
    private List<String> monetization;

    @Size(max = 256, message = "变现门槛长度不能超过 256")
    private String threshold;

    @Size(max = 256, message = "适合谁长度不能超过 256")
    private String bestFor;

    @Size(max = 512, message = "推荐理由长度不能超过 512")
    private String reason;

    @Size(max = 32, message = "变现难度长度不能超过 32")
    private String monetizationEase;

    @Size(max = 32, message = "预计周期长度不能超过 32")
    private String timeToIncome;

    @Size(max = 64, message = "收入空间长度不能超过 64")
    private String incomeRange;

    @Size(max = 16, message = "运营难度长度不能超过 16")
    private String difficulty;

    @Min(value = 0, message = "排序不能为负数")
    private Integer sortOrder;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值非法")
    @Max(value = 1, message = "状态值非法")
    private Integer status;

    @NotNull(message = "是否默认不能为空")
    @Min(value = 0, message = "是否默认值非法")
    @Max(value = 1, message = "是否默认值非法")
    private Integer isDefault;

    @Size(max = 512, message = "图标 URL 长度不能超过 512")
    private String iconUrl;
}
