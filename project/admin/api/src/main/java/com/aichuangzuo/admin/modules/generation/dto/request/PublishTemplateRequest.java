package com.aichuangzuo.admin.modules.generation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布创作模板请求。
 */
@Data
public class PublishTemplateRequest {

    @Size(max = 512)
    private String changeNote;
}