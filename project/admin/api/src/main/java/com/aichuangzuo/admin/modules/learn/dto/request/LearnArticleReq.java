package com.aichuangzuo.admin.modules.learn.dto.request;

import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearnArticleReq {

    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 128)
    private String title;

    @Size(max = 255)
    private String summary;

    @NotNull
    private ContentType contentType;

    @NotNull
    private String content;

    private ArticleStatus status = ArticleStatus.DRAFT;

    private Integer sort = 0;
}
