package com.aichuangzuo.admin.modules.hotsearch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 热搜平台配置，对应表 hot_search_platform。
 */
@Getter
@Setter
@TableName("hot_search_platform")
public class HotSearchPlatform {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String icon;

    private Integer sortOrder;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
