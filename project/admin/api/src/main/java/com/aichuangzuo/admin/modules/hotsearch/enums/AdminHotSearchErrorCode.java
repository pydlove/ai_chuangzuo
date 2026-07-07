package com.aichuangzuo.admin.modules.hotsearch.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 热搜模块管理端错误码。
 *
 * <p>错误码段：25xxxx
 */
@Getter
public enum AdminHotSearchErrorCode implements ErrorCode {

    PLATFORM_NOT_FOUND(250001, "热搜平台不存在"),
    PLATFORM_CODE_DUPLICATED(250002, "平台编码已存在"),
    PLATFORM_IN_USE(250003, "平台存在榜单数据，禁止删除"),
    DAILY_NOT_FOUND(250004, "每日榜单条目不存在"),
    DAILY_RANK_DUPLICATED(250005, "同日同平台存在相同排名"),
    CONFIG_NOT_FOUND(250006, "抓取配置不存在"),
    INVALID_CRON(250007, "cron 表达式非法"),
    CRAWL_FAILED(250008, "手动抓取失败");

    private final int code;
    private final String message;

    AdminHotSearchErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
