package com.aichuangzuo.user.modules.hotsearch.crawler;

import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;

import java.util.List;

/**
 * 热搜抓取器接口。
 */
public interface HotSearchFetcher {

    /**
     * 判断当前抓取器是否支持指定平台。
     *
     * @param platform 平台配置
     * @return true 表示支持
     */
    boolean supports(HotSearchPlatform platform);

    /**
     * 抓取指定平台的热搜列表。
     *
     * @param platform 平台配置
     * @return 热搜项列表；抓取失败时返回空列表
     */
    List<HotSearchItem> fetch(HotSearchPlatform platform);
}
