package com.aichuangzuo.admin.modules.lottery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CloneCampaignRequest {

    @NotBlank(message = "活动名称不能为空")
    private String name;
}
