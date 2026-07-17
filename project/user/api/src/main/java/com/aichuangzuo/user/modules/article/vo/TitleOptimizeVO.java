package com.aichuangzuo.user.modules.article.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * AI 标题优化结果视图。
 */
@Getter
@Setter
public class TitleOptimizeVO {

    /** 平台 key → 2 条优化标题，平台含 wechat/xiaohongshu/toutiao/baijiahao/zhihu/douyin/bilibili。 */
    private Map<String, List<String>> titles;

    /** true=命中首次生成的缓存；false=本次新调用大模型生成。 */
    private Boolean cached;
}
