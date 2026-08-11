package com.aichuangzuo.admin.modules.lottery.dto.request;

import lombok.Data;

@Data
public class LotteryCampaignQueryRequest {

    private String keyword;
    private Long page = 1L;
    private Long size = 20L;
}
