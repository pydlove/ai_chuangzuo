package com.aichuangzuo.admin.modules.homebanner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HomeBannerReq {

    @NotBlank(message = "图片 URL 不能为空")
    @Size(max = 512)
    private String imageUrl;

    @Size(max = 512)
    private String linkUrl = "";

    @NotNull
    private Integer sort = 0;
}
