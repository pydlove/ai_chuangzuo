package com.aichuangzuo.user.modules.article.vo;

import lombok.Data;

import java.util.List;

/**
 * 作品分页 VO。
 */
@Data
public class ArticlePageVO {

    private List<ArticleVO> list;

    private long total;

    private long page;

    private long pageSize;
}