package com.aichuangzuo.admin.modules.topictitle.vo;

import lombok.Data;

import java.util.List;

/**
 * 标题管理分页结果。
 */
@Data
public class TopicTitlePageVO {

    private List<TopicTitleAdminVO> list;
    private long total;
    private long page;
    private long pageSize;
}
