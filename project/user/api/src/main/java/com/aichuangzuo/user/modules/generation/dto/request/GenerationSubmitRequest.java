package com.aichuangzuo.user.modules.generation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户提交创作任务请求。
 *
 * <p>{@link #modelConfigId} 可空；空时由后端从 a_model_config.is_active=1 选一条。
 * 风格 {@link #styleRef} 是用户的写作风格名（U 用户风格表中的标识），可不传。
 */
@Data
public class GenerationSubmitRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 256, message = "标题长度不能超过 256")
    private String title;

    @Size(max = 4000, message = "描述过长")
    private String description;

    /** 平台：wechat / xiaohongshu / toutiao / ... */
    @Size(max = 32)
    private String platform;

    /** 写作风格引用名（u_style 表标识）。 */
    @Size(max = 64)
    private String styleRef;

    @Min(value = 100, message = "字数至少 100")
    @Max(value = 3000, message = "字数不能超过 3000")
    private Integer wordCount;

    /** 可选；为空时系统选当前 active 模型。 */
    private Long modelConfigId;
}
