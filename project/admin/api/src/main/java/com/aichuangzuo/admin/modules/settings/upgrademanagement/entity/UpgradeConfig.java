package com.aichuangzuo.admin.modules.settings.upgrademanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 升级管理配置，对应表 {@code a_upgrade_config}。
 *
 * <p>单行配置（id=1），由 admin 端系统设置-升级管理维护。
 * 服务器密码落库为 Jasypt 加密后的密文。
 */
@Getter
@Setter
@TableName("a_upgrade_config")
public class UpgradeConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 脚本根目录。 */
    private String scriptRootDir;

    /** 服务器 IP。 */
    private String serverIp;

    /** SSH 用户名。 */
    private String serverUser;

    /** Jasypt 加密后的 SSH 密码。 */
    private String serverPassword;

    /** SSH 私钥路径。 */
    private String sshKeyPath;

    /** 脚本执行超时秒数。 */
    private Integer commandTimeoutSeconds;

    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
