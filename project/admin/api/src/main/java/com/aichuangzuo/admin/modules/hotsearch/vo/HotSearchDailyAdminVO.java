package com.aichuangzuo.admin.modules.hotsearch.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HotSearchDailyAdminVO {
    private Long id;
    private String platformCode;
    private String platformName;
    private Integer rankNum;
    private String title;
    private String hotValue;
    private String url;
    private Long searchCount;
    private LocalDate snapshotDate;
}
