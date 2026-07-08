package com.aichuangzuo.admin.modules.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端-消息管理-编辑请求。
 *
 * <p>仅允许编辑 title / summary / linkUrl；scope / targetUserIds / msgType / biz_no
 * 一旦发出即不可改，避免破坏已读关联。
 */
@Data
public class MessageUpdateRequest {

    @NotBlank
    @Size(max = 128)
    private String title;

    @NotBlank
    @Size(max = 512)
    private String summary;

    @Size(max = 256)
    private String linkUrl;
}
