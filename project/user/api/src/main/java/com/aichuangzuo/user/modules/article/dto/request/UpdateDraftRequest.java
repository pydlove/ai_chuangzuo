package com.aichuangzuo.user.modules.article.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 修改草稿请求。
 */
@Data
public class UpdateDraftRequest {

    @Size(max = 256)
    private String customTitle;

    private String customRequirement;

    @Size(max = 32)
    private String platform;

    private Integer wordCount;

    @Size(max = 64)
    private String style;

    @Size(max = 64)
    private String template;

    /**
     * 重新保存时间；为 null 时服务端用 now()。
     */
    private LocalDateTime savedAt;
}