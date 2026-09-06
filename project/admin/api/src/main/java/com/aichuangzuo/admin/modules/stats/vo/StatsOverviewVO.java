package com.aichuangzuo.admin.modules.stats.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatsOverviewVO {

    /** 当前在线（最近 5 分钟活跃用户数） */
    private final long onlineCount;

    /** 今日日活 */
    private final long todayActive;
}
