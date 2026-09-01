package com.aichuangzuo.admin.modules.settings.upgrademanagement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 升级管理配置更新请求。
 */
@Data
public class UpgradeConfigUpdateRequest {

    /** 脚本根目录。 */
    @NotBlank(message = "脚本根目录不能为空")
    private String scriptRootDir;

    /** 服务器 IP。 */
    private String serverIp;

    /** SSH 用户名。 */
    private String serverUser;

    /** SSH 密码。留空或全为 * 号表示不修改原密码。 */
    private String serverPassword;

    /** SSH 私钥路径。 */
    private String sshKeyPath;

    /** 脚本执行超时秒数。 */
    @NotNull(message = "超时时间不能为空")
    @Min(value = 10, message = "超时时间至少 10 秒")
    private Integer commandTimeoutSeconds;
}
