package com.aichuangzuo.admin.modules.commission.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminCommissionErrorCode implements ErrorCode {
    TASK_NOT_FOUND(119001, "约稿任务不存在"),
    TASK_STATUS_INVALID(119002, "当前任务状态不允许此操作"),
    SUBMISSION_NOT_FOUND(119003, "投稿不存在"),
    SUBMISSION_STATUS_INVALID(119004, "投稿状态不允许此操作"),
    ADOPT_COUNT_EXCEEDED(119005, "采纳数量超过任务所需数量"),
    PARAM_INVALID(119006, "约稿任务参数不合法"),
    SUBMISSION_USER_NOT_FOUND(119007, "投稿用户不存在"),
    SUBMISSION_ALREADY_EXISTS(119008, "该用户已投递此任务"),
    EXCEL_FILE_INVALID(119009, "Excel文件不合法，请使用最新模板填写后上传"),
    EXCEL_PARSE_ERROR(119010, "Excel解析失败，请检查文件是否为有效的.xlsx格式");

    private final int code;
    private final String message;

    AdminCommissionErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
