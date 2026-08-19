package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuestionVO {
    private String key;
    private String text;
    private List<QuestionOptionVO> options;
    private Boolean isRequired;
    private Integer sortOrder;
}
