package com.aichuangzuo.user.modules.style.vo;

import lombok.Data;

/**
 * 系统预设风格视图对象。
 */
@Data
public class SystemStyleVO {

    private String bizNo;
    private String name;
    private String description;
    private String promptSummary;
    private String prompt;
    private String scope;
}