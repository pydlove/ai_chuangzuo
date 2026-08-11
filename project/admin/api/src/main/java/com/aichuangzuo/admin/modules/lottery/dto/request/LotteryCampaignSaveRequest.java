package com.aichuangzuo.admin.modules.lottery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryCampaignSaveRequest {

    private Long id;

    @NotBlank(message = "活动名称不能为空")
    private String name;

    private String description;

    private String rules;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "免费次数不能为空")
    private Integer freeDrawsPerUser;
}
