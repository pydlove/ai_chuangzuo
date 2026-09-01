package com.aichuangzuo.admin.modules.settings.upgrademanagement.vo;

import lombok.Data;

/**
 * 升级管理配置 VO。
 */
@Data
public class UpgradeConfigVO {

    private Long id;
    private String scriptRootDir;
    private String serverIp;
    private String serverUser;
    private String serverPassword;
    private String sshKeyPath;
    private Integer commandTimeoutSeconds;
}
