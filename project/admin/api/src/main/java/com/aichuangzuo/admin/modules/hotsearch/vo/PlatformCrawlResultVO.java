package com.aichuangzuo.admin.modules.hotsearch.vo;

import lombok.Data;

@Data
public class PlatformCrawlResultVO {
    private String platformCode;
    private String platformName;
    private boolean success;
    private int fetched;
    private String error;
}
