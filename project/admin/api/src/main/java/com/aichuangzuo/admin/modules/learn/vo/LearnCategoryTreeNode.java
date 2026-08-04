package com.aichuangzuo.admin.modules.learn.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearnCategoryTreeNode {

    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private List<LearnCategoryTreeNode> children = new ArrayList<>();
}
