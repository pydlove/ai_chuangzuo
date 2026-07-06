package com.aichuangzuo.user.modules.style.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户风格视图对象。
 */
@Data
public class UserStyleVO {

    private String bizNo;
    private String styleName;
    private String prompt;
    private String scope;
    private Integer sourceType;
    private Integer useCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
