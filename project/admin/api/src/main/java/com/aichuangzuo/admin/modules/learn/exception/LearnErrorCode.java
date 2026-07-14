package com.aichuangzuo.admin.modules.learn.exception;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 创作学院模块错误码，段位 2700xx。
 */
@Getter
@AllArgsConstructor
public enum LearnErrorCode implements ErrorCode {

    CATEGORY_NAME_DUPLICATE(270001, "同级分类名重复"),
    CATEGORY_NOT_EMPTY(270002, "分类下存在子分类或文章，无法删除"),
    CONTENT_TOO_LARGE(270003, "正文超出大小限制"),
    ARTICLE_NOT_FOUND(270004, "文章不存在或已下线"),
    PUBLISHED_CONTENT_TYPE_LOCKED(270005, "已发布文章不允许切换正文类型"),
    CATEGORY_NOT_FOUND(270006, "分类不存在"),
    BANNER_NOT_FOUND(270007, "Banner 不存在");

    private final int code;
    private final String message;
}
