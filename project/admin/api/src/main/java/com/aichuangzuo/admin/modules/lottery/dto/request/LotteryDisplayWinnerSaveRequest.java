package com.aichuangzuo.admin.modules.lottery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryDisplayWinnerSaveRequest {

    private Long id;

    @NotNull(message = "活动ID不能为空")
    private Long campaignId;

    private Long tierId;

    private Long userId;

    @NotBlank(message = "展示奖品名不能为空")
    private String prizeName;

    @NotBlank(message = "展示昵称不能为空")
    private String nickname;

    private String avatarUrl;

    @NotNull(message = "展示时间不能为空")
    private LocalDateTime winTime;

    private Integer sortOrder;
}
