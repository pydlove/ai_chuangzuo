package com.aichuangzuo.admin.modules.testimonial.exception;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 首页用户评价模块错误码，段位 2900xx。
 */
@Getter
@AllArgsConstructor
public enum TestimonialErrorCode implements ErrorCode {

    TESTIMONIAL_NOT_FOUND(290001, "评价不存在"),
    AVATAR_FILE_INVALID(290002, "头像文件无效，请上传不超过 5MB 的 JPG/PNG 图片"),
    AVATAR_UPLOAD_FAILED(290003, "头像上传失败");

    private final int code;
    private final String message;
}
