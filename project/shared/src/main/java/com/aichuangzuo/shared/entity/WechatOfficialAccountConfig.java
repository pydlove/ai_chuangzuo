package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 微信公众号配置实体，对应表 {@code a_wechat_official_account_config}。
 *
 * <p>两端共享实体：admin-api 负责配置管理；user-api 负责读取并调用微信接口。</p>
 */
@Getter
@Setter
@TableName("a_wechat_official_account_config")
public class WechatOfficialAccountConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公众号 AppID。 */
    private String appId;

    /** 公众号 AppSecret（Jasypt 加密后的密文）。 */
    private String appSecret;

    /** 服务器配置 Token。 */
    private String token;

    /** 是否明文模式：0-否，1-是。 */
    private Integer plaintextMode;

    /** 是否启用：0-否，1-是。 */
    private Integer enabled;
}
