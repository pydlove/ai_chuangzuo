
package com.aichuangzuo.admin.modules.security.smsconfig.service;

import com.aichuangzuo.admin.modules.security.smsconfig.dto.request.SmsConfigUpdateRequest;
import com.aichuangzuo.admin.modules.security.smsconfig.entity.SmsConfig;
import com.aichuangzuo.admin.modules.security.smsconfig.mapper.SmsConfigMapper;
import com.aichuangzuo.admin.modules.security.smsconfig.vo.SmsConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 短信配置服务。
 *
 * <p>单行配置（id=1），admin 端维护。AccessKeySecret 落库前用 Jasypt 加密。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsConfigService {

    private static final long CONFIG_ID = 1L;
    private static final Pattern MASKED_SECRET = Pattern.compile("^\\*+$");

    private final SmsConfigMapper mapper;
    private final StringEncryptor encryptor;

    public SmsConfigVO detail() {
        SmsConfig config = mapper.selectById(CONFIG_ID);
        if (config == null) {
            config = defaultConfig();
        }
        return toVo(config, true);
    }

    @Transactional
    public SmsConfigVO update(SmsConfigUpdateRequest request, Long adminUserId) {
        SmsConfig exist = mapper.selectById(CONFIG_ID);
        boolean isNew = exist == null;
        if (isNew) {
            exist = defaultConfig();
        }

        exist.setProvider(request.getProvider());
        exist.setAccessKeyId(request.getAccessKeyId());
        exist.setSignName(request.getSignName());
        exist.setTemplateCode(request.getTemplateCode());
        exist.setRegionId(request.getRegionId());
        exist.setEnabled(request.getEnabled());
        exist.setSendIntervalSeconds(request.getSendIntervalSeconds());
        exist.setDailyMaxPerPhone(request.getDailyMaxPerPhone());
        exist.setDailyMaxPerIp(request.getDailyMaxPerIp());
        exist.setGlobalDailyMax(request.getGlobalDailyMax());

        String secret = request.getAccessKeySecret();
        if (StringUtils.hasText(secret) && !MASKED_SECRET.matcher(secret).matches()) {
            exist.setAccessKeySecret(encryptor.encrypt(secret));
        } else if (isNew) {
            exist.setAccessKeySecret(null);
        }
        // 否则保留原密钥（UI 只展示掩码时不覆盖）

        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        if (isNew) {
            mapper.insert(exist);
        } else {
            mapper.updateById(exist);
        }

        log.info("admin={} 更新短信配置 enabled={}", adminUserId, exist.getEnabled());
        return toVo(exist, true);
    }

    private SmsConfig defaultConfig() {
        SmsConfig config = new SmsConfig();
        config.setId(CONFIG_ID);
        config.setProvider("aliyun");
        config.setRegionId("cn-hangzhou");
        config.setEnabled(0);
        config.setSendIntervalSeconds(60);
        config.setDailyMaxPerPhone(10);
        config.setDailyMaxPerIp(50);
        config.setGlobalDailyMax(1000);
        return config;
    }

    private SmsConfigVO toVo(SmsConfig config, boolean decryptSecret) {
        SmsConfigVO vo = new SmsConfigVO();
        BeanUtils.copyProperties(config, vo);
        if (decryptSecret && StringUtils.hasText(config.getAccessKeySecret())) {
            try {
                vo.setAccessKeySecret(encryptor.decrypt(config.getAccessKeySecret()));
            } catch (Exception e) {
                log.warn("短信 AccessKeySecret 解密失败，返回空");
                vo.setAccessKeySecret("");
            }
        }
        return vo;
    }
}
