package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 自媒体平台配置（对应 c_platform，管理端配置、用户端读取）。
 */
@Getter
@Setter
@TableName("c_platform")
public class Platform extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台唯一键，如 wechat / xiaohongshu。 */
    private String platformKey;

    /** 平台显示名，如 公众号。 */
    private String platformName;

    /** 平台简介。 */
    private String description;

    /** 推荐字数。 */
    private Integer recommendWords;

    /** 平台风格/特征描述。 */
    private String trait;

    /** 平台专属字数档位配置 JSON。 */
    private String wordCountPresetsJson;

    /** 排序，越小越靠前。 */
    private Integer sortOrder;

    /** 状态：0-停用，1-启用。 */
    private Integer status;

    /** 是否默认选中：0-否，1-是。 */
    private Integer isDefault;

    /** 平台图标 URL。 */
    private String iconUrl;

    /** 一句话卖点。 */
    private String tagline;

    /** 内容形式 JSON。 */
    private String contentFormJson;

    /** 主要收益 JSON。 */
    private String monetizationJson;

    /** 变现门槛。 */
    private String threshold;

    /** 适合谁。 */
    private String bestFor;

    /** 提示/推荐理由。 */
    private String reason;

    /** 变现难度。 */
    private String monetizationEase;

    /** 预计周期。 */
    private String timeToIncome;

    /** 收入空间。 */
    private String incomeRange;

    /** 运营难度。 */
    private String difficulty;
}
