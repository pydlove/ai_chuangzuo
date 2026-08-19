package com.aichuangzuo.user.modules.platform.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户端自媒体平台配置视图对象。
 */
@Data
public class PlatformVO {

    private Long id;
    private String platformKey;
    private String platformName;
    private String description;
    private Integer recommendWords;
    private String trait;
    private Boolean isDefault;
    private String iconUrl;
    private List<String> wordCountPresets;
    private String tagline;
    private List<String> contentForm;
    private List<String> monetization;
    private String threshold;
    private String bestFor;
    private String reason;
    private String monetizationEase;
    private String timeToIncome;
    private String incomeRange;
    private String difficulty;
}
