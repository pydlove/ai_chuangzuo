package com.aichuangzuo.admin.modules.hotsearch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 热搜抓取配置，对应表 hot_search_config。
 * 单行表，固定 id=1。
 */
@Getter
@Setter
@TableName("hot_search_config")
public class HotSearchConfig {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String cron;

    private Integer enabled;

    private Integer topN;

    private Integer connectTimeoutMillis;

    private Integer readTimeoutMillis;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long updatedBy;
}
