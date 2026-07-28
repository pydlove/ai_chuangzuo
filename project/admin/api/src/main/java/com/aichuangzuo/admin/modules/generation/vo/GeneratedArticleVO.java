package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理端通过内部接口读取的已生成文章内容。
 */
@Data
public class GeneratedArticleVO {

    private String bizNo;
    private String title;
    private String body;
    private String platform;
    private String skill;
    private String skillName;
    private String template;
    private String description;
    private Integer wordCount;
    private List<String> tags;
}
