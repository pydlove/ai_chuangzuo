package com.aichuangzuo.user.modules.learn.vo;

import lombok.Data;

import java.util.List;

@Data
public class LearnCategoryDetailVO {
    private Long id;
    private String name;
    private Long parentId;
    private String parentName;
    private List<LearnCategoryTreeVO> children;
    private List<LearnArticleVO> articles;
    private Integer page;
    private Integer size;
    private Long total;
}
