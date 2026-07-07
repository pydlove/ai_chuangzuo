package com.aichuangzuo.admin.modules.hotsearch.crawler;

import lombok.Getter;
import lombok.Setter;

/**
 * 抓取到的原始热搜项。
 */
@Getter
@Setter
public class HotSearchItem {

    private Integer rank;

    private String title;

    private String hotValue;

    private String url;

    private Long searchCount;
}
