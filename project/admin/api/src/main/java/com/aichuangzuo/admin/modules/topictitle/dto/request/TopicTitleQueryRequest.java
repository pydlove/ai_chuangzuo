package com.aichuangzuo.admin.modules.topictitle.dto.request;

import lombok.Data;

/**
 * 标题管理列表查询请求。
 */
@Data
public class TopicTitleQueryRequest {

    /** 关键字：标题模糊匹配。 */
    private String keyword;

    /** 使用状态：0-未使用，1-已使用；null-全部。 */
    private Integer usedStatus;

    private long page = 1;
    private long pageSize = 20;
}
