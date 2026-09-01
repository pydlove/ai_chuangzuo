package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 管理端 - 升级管理错误码。
 */
@Getter
public enum AdminUpgradeErrorCode implements ErrorCode {

    CONFIG_NOT_FOUND(270001, "升级配置不存在"),
    SCRIPT_NOT_FOUND(270002, "脚本不存在或不在允许目录"),
    SCRIPT_PATH_INVALID(270003, "脚本路径不合法"),
    SCRIPT_EXECUTION_FAILED(270004, "脚本执行失败"),
    JOB_NOT_FOUND(270005, "执行记录不存在"),
    ROOT_DIR_INVALID(270006, "脚本根目录不存在或不合法"),
    NOT_SUPER_ADMIN(270007, "仅超级管理员可操作升级管理");

    private final int code;
    private final String message;

    AdminUpgradeErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
