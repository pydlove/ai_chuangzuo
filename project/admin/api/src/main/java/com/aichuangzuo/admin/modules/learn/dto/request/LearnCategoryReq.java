package com.aichuangzuo.admin.modules.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearnCategoryReq {

    /** 父分类 id；null 表示顶级 */
    private Long parentId;

    @NotBlank
    @Size(max = 64)
    private String name;

    private Integer sort = 0;
}
