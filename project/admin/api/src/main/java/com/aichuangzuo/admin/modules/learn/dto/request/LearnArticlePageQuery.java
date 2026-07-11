package com.aichuangzuo.admin.modules.learn.dto.request;

import lombok.Data;

@Data
public class LearnArticlePageQuery {

    private Long categoryId;
    /** 草稿/已发布；小写 code（draft/published），由 Service 解析为枚举 */
    private String status;
    private String keyword;
    private Integer page = 1;
    private Integer size = 20;
}
