package com.aichuangzuo.admin.modules.hotsearch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 热搜抓取执行日志。
 */
@Getter
@Setter
@TableName("hot_search_crawl_log")
public class HotSearchCrawlLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String triggerType;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Integer successCount;

    private Integer failCount;

    private Integer totalFetched;

    private String status;

    private String resultsJson;

    private String errorMsg;

    private Long createdBy;

    private LocalDateTime createdAt;
}
