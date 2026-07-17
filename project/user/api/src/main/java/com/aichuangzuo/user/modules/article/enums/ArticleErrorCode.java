package com.aichuangzuo.user.modules.article.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 用户作品/草稿模块业务错误码。
 *
 * <p>错误码段：113xxx
 */
@Getter
public enum ArticleErrorCode implements ErrorCode {

    ARTICLE_NOT_FOUND(113001, "作品不存在或无权访问"),
    DRAFT_NOT_FOUND(113002, "草稿不存在或无权访问"),
    ARTICLE_TITLE_EMPTY(113003, "作品标题不能为空"),
    ARTICLE_BODY_EMPTY(113004, "作品正文不能为空"),
    TITLE_OPTIMIZE_FAILED(113005, "AI 标题优化失败，请稍后重试");

    private final int code;
    private final String message;

    ArticleErrorCode(int code, String String) {
        this.code = code;
        this.message = String;
    }
}