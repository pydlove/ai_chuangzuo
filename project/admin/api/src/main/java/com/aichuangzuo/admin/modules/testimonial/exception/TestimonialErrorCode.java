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
    AVATAR_UPLOAD_FAILED(290003, "头像上传失败"),
    EXCEL_FILE_INVALID(290004, "Excel 文件无效，请上传不超过 10MB 的 .xlsx 文件，并检查表头"),
    EXCEL_PARSE_ERROR(290005, "Excel 解析失败"),
    EXCEL_IMPORT_EMPTY(290006, "Excel 中未找到有效数据");

    private final int code;
    private final String message;
}
