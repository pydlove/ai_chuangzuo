package com.aichuangzuo.user.modules.wechat.service;

import com.aichuangzuo.shared.entity.WechatOfficialAccountConfig;
import com.aichuangzuo.user.modules.wechat.mapper.WechatOfficialAccountConfigMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 微信公众号配置读取服务。
 *
 * <p>从 admin 端维护的 {@code a_wechat_official_account_config} 表读取配置，
 * AppSecret 使用 Jasypt 解密。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatConfigService {

    private static final long CONFIG_ID = 1L;

    private final WechatOfficialAccountConfigMapper mapper;
    private final StringEncryptor encryptor;

    /**
     * 获取当前微信公众号配置（AppSecret 已解密）。
     */
    public ConfigSnapshot getConfig() {
        WechatOfficialAccountConfig entity = mapper.selectById(CONFIG_ID);
        if (entity == null) {
            entity = defaultConfig();
        }

        ConfigSnapshot snapshot = new ConfigSnapshot();
        snapshot.setAppId(StringUtils.hasText(entity.getAppId()) ? entity.getAppId() : "");
        snapshot.setToken(StringUtils.hasText(entity.getToken()) ? entity.getToken() : "");
        snapshot.setPlaintextMode(entity.getPlaintextMode() != null && entity.getPlaintextMode() == 1);
        snapshot.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);

        String encryptedSecret = entity.getAppSecret();
        if (StringUtils.hasText(encryptedSecret)) {
            try {
                snapshot.setAppSecret(encryptor.decrypt(encryptedSecret));
            } catch (Exception e) {
                log.warn("微信公众号 AppSecret 解密失败");
                snapshot.setAppSecret("");
            }
        } else {
            snapshot.setAppSecret("");
        }
        return snapshot;
    }

    private WechatOfficialAccountConfig defaultConfig() {
        WechatOfficialAccountConfig config = new WechatOfficialAccountConfig();
        config.setId(CONFIG_ID);
        config.setAppId("");
        config.setAppSecret("");
        config.setToken("");
        config.setPlaintextMode(1);
        config.setEnabled(0);
        return config;
    }

    /**
     * 微信公众号配置快照。
     */
    @Getter
    @Setter
    public static class ConfigSnapshot {
        private String appId;
        private String appSecret;
        private String token;
        private boolean plaintextMode;
        private boolean enabled;
    }
}
