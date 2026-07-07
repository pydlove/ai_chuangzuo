package com.aichuangzuo.admin.modules.hotsearch.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 热搜抓取配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hot-search")
public class HotSearchProperties {

    /**
     * 是否启用定时抓取。
     */
    private boolean crawlEnabled = true;

    /**
     * 定时任务 cron 表达式，默认每天 02:00。
     */
    private String cron = "0 0 2 * * ?";

    /**
     * 需要抓取的平台编码列表；为空表示全部启用平台。
     */
    private List<String> platforms = new ArrayList<>();

    /**
     * 每个平台最多保留多少条，默认 50。
     */
    private Integer topN = 50;

    /**
     * jsoup 连接超时毫秒。
     */
    private Integer connectTimeoutMillis = 10000;

    /**
     * jsoup 读取超时毫秒。
     */
    private Integer readTimeoutMillis = 10000;
}
