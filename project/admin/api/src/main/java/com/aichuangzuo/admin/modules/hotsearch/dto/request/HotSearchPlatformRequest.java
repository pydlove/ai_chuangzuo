package com.aichuangzuo.admin.modules.hotsearch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HotSearchPlatformRequest {
    @NotBlank
    @Size(max = 32)
    private String code;
    @NotBlank
    @Size(max = 64)
    private String name;
    @Size(max = 255)
    private String icon;
    private Integer sortOrder;
    private Integer enabled;
}
