package com.aichuangzuo.admin.modules.message.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端-消息管理-列表查询请求。
 *
 * <p>msgType 必填，由前端 tab 决定：announcement / feature / promotion。
 */
@Data
public class MessageQueryRequest {

    /** 消息类型：announcement / feature / promotion。 */
    @NotBlank
    private String msgType;

    /** 模糊搜索关键词（命中 title / summary）。 */
    private String keyword;

    @Min(1)
    private int page = 1;

    @Min(1)
    private int size = 20;
}
