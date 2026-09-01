package com.aichuangzuo.admin.modules.settings.upgrademanagement.vo;

import lombok.Data;

/**
 * 升级脚本 VO。
 */
@Data
public class UpgradeScriptVO {

    /** 脚本相对根目录路径。 */
    private String relativePath;

    /** 脚本文件名。 */
    private String name;

    /** 所属分类/目录。 */
    private String category;

    /** 脚本描述（从首行注释解析）。 */
    private String description;
}
