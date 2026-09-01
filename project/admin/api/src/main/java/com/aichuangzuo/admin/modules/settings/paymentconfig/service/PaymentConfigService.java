package com.aichuangzuo.admin.modules.settings.paymentconfig.service;

import com.aichuangzuo.admin.modules.settings.paymentconfig.dto.request.PaymentConfigUpdateRequest;
import com.aichuangzuo.admin.modules.settings.paymentconfig.entity.PaymentConfig;
import com.aichuangzuo.admin.modules.settings.paymentconfig.mapper.PaymentConfigMapper;
import com.aichuangzuo.admin.modules.settings.paymentconfig.vo.PaymentConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 支付配置服务。
 *
 * <p>单行配置（id=1），admin 端维护。AppSecret 落库前用 Jasypt 加密。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfigService {

    private static final long CONFIG_ID = 1L;
    private static final Pattern MASKED_SECRET = Pattern.compile("^\\*+$");

    private final PaymentConfigMapper mapper;
    private final StringEncryptor encryptor;

    public PaymentConfigVO detail() {
        PaymentConfig config = mapper.selectById(CONFIG_ID);
        if (config == null) {
            config = defaultConfig();
        }
        return toVo(config, true);
    }

    /**
     * 获取支付配置实体（AppSecret 已解密）。
     */
    public PaymentConfig getConfig(Long id) {
        PaymentConfig config = mapper.selectById(id);
        if (config == null) {
            config = defaultConfig();
        }
        if (StringUtils.hasText(config.getAppSecret())) {
            try {
                config.setAppSecret(encryptor.decrypt(config.getAppSecret()));
            } catch (Exception e) {
                log.warn("支付 AppSecret 解密失败");
                config.setAppSecret(null);
            }
        }
        return config;
    }

    @Transactional
    public PaymentConfigVO update(PaymentConfigUpdateRequest request, Long adminUserId) {
        PaymentConfig exist = mapper.selectById(CONFIG_ID);
        boolean isNew = exist == null;
        if (isNew) {
            exist = defaultConfig();
        }

        exist.setProvider(request.getProvider());
        exist.setAppId(request.getAppId());
        exist.setGatewayUrl(request.getGatewayUrl());
        exist.setRefundUrl(request.getRefundUrl());
        exist.setNotifyUrl(request.getNotifyUrl());
        exist.setReturnUrl(request.getReturnUrl());
        exist.setEnabled(1);
        exist.setTestMode(request.getTestMode());

        String secret = request.getAppSecret();
        if (StringUtils.hasText(secret) && !MASKED_SECRET.matcher(secret).matches()) {
            exist.setAppSecret(encryptor.encrypt(secret));
        } else if (isNew) {
            exist.setAppSecret(null);
        }

        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        if (isNew) {
            mapper.insert(exist);
        } else {
            mapper.updateById(exist);
        }

        log.info("admin={} 更新支付配置 testMode={}", adminUserId, exist.getTestMode());
        return toVo(exist, true);
    }

    private PaymentConfig defaultConfig() {
        PaymentConfig config = new PaymentConfig();
        config.setId(CONFIG_ID);
        config.setProvider("xunhupay");
        config.setGatewayUrl("https://api.xunhupay.com/payment/do.html");
        config.setRefundUrl("https://api.xunhupay.com/payment/refund.html");
        config.setEnabled(1);
        config.setTestMode(1);
        return config;
    }

    private PaymentConfigVO toVo(PaymentConfig config, boolean decryptSecret) {
        PaymentConfigVO vo = new PaymentConfigVO();
        BeanUtils.copyProperties(config, vo);
        if (decryptSecret && StringUtils.hasText(config.getAppSecret())) {
            try {
                vo.setAppSecret(encryptor.decrypt(config.getAppSecret()));
            } catch (Exception e) {
                log.warn("支付 AppSecret 解密失败，返回空");
                vo.setAppSecret("");
            }
        }
        return vo;
    }
}
