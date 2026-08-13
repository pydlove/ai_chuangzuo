package com.aichuangzuo.admin.modules.scheduler.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 定时任务分页 VO。
 */
@Getter
@Setter
public class ScheduledTaskPageVO {

    private List<ScheduledTaskVO> list;

    private long total;

    private long page;

    private long pageSize;
}
