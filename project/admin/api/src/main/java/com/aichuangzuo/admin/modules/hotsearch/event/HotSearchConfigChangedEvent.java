package com.aichuangzuo.admin.modules.hotsearch.event;

/**
 * 热搜抓取配置变更事件。
 * 由 HotSearchConfigServiceImpl 在 saveConfig 后发布，
 * HotSearchCrawlJob 监听并 reschedule。
 */
public record HotSearchConfigChangedEvent(Long adminId) {
}
