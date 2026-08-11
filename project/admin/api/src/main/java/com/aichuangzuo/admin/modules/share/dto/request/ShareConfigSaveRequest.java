package com.aichuangzuo.admin.modules.share.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShareConfigSaveRequest {

    private Long id;

    @NotBlank(message = "场景标识不能为空")
    @Pattern(regexp = "lottery|invite", message = "场景标识只能是 lottery 或 invite")
    @Size(max = 32, message = "场景标识长度不能超过32")
    private String sceneKey;

    @NotBlank(message = "配置标题不能为空")
    @Size(max = 128, message = "配置标题长度不能超过128")
    private String title;

    @NotBlank(message = "分享文案不能为空")
    @Size(max = 2000, message = "分享文案长度不能超过2000")
    private String content;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private Integer sortOrder;
}
