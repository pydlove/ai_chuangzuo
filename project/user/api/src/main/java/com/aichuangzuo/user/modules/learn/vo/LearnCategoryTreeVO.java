package com.aichuangzuo.user.modules.learn.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearnCategoryTreeVO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private List<LearnCategoryTreeVO> children = new ArrayList<>();
}
