package com.aichuangzuo.user.modules.hotsearch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日热搜榜单，对应表 hot_search_daily。
 */
@Getter
@Setter
@TableName("hot_search_daily")
public class HotSearchDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platformCode;

    private Integer rankNum;

    private String title;

    private String hotValue;

    private String url;

    private Long searchCount;

    private LocalDate snapshotDate;

    private LocalDateTime createdAt;
}
