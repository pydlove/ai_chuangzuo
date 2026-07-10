package com.aichuangzuo.admin.modules.learn.dto.request;

import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import lombok.Data;

@Data
public class LearnArticlePageQuery {

    private Long categoryId;
    private ArticleStatus status;
    private String keyword;
    private Integer page = 1;
    private Integer size = 20;
}
