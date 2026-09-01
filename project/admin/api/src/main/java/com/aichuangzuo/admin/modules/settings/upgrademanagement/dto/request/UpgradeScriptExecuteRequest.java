package com.aichuangzuo.admin.modules.settings.upgrademanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 升级脚本执行请求。
 */
@Data
public class UpgradeScriptExecuteRequest {

    /** 脚本相对根目录路径。 */
    @NotBlank(message = "脚本路径不能为空")
    private String scriptRelativePath;

    /** 脚本执行参数（可选），按顺序传入脚本。 */
    private List<String> arguments = new ArrayList<>();
}
