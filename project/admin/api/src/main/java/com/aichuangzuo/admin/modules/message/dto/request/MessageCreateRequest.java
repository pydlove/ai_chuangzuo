package com.aichuangzuo.admin.modules.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端-消息管理-新建请求。
 *
 * <p>业务规则：
 * <ul>
 *   <li>msgType=feature（新功能）时强制 scope=1，targetUserIds 忽略</li>
 *   <li>scope=1（广播）时 targetUserIds 必须为空或 null</li>
 *   <li>scope=2（指定人）时 targetUserIds 至少 1 个</li>
 * </ul>
 */
@Data
public class MessageCreateRequest {

    /** 消息类型：announcement / feature / promotion。 */
    @NotBlank
    private String msgType;

    /** 标题。 */
    @NotBlank
    @Size(max = 128)
    private String title;

    /** 摘要。 */
    @NotBlank
    @Size(max = 512)
    private String summary;

    /** 点击跳转路由（可选）。 */
    @Size(max = 256)
    private String linkUrl;

    /** 范围：1-广播，2-个人。 */
    @NotNull
    private Integer scope;

    /** 目标用户ID列表（scope=2 时必填）。 */
    private List<Long> targetUserIds;
}
