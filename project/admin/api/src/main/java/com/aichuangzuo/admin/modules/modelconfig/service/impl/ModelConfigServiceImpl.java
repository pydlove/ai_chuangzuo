package com.aichuangzuo.admin.modules.modelconfig.service.impl;

import com.aichuangzuo.admin.infrastructure.ai.AiProvider;
import com.aichuangzuo.admin.infrastructure.ai.AiProviderClient;
import com.aichuangzuo.admin.infrastructure.ai.KimiProviderClient;
import com.aichuangzuo.admin.infrastructure.ai.MinimaxProviderClient;
import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigConnectionRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.modelconfig.service.ModelConfigService;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.aichuangzuo.shared.enums.error.AdminModelConfigErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;
    private final KimiProviderClient kimiProviderClient;
    private final MinimaxProviderClient minimaxProviderClient;

    @Value("${admin.model.api-key-secret}")
    private String apiKeySecret;

    @Override
    public List<ModelConfigVO> listConfigs() {
        List<ModelConfigVO> result = new ArrayList<>();
        for (AiProvider provider : AiProvider.all()) {
            ModelConfig entity = modelConfigMapper.selectByProviderType(provider.getCode());
            result.add(toVo(entity, provider));
        }
        return result;
    }

    @Override
    public ModelConfigVO getConfig(String providerType) {
        AiProvider provider = resolveProvider(providerType);
        ModelConfig entity = modelConfigMapper.selectByProviderType(providerType);
        return toVo(entity, provider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(String providerType, ModelConfigSaveRequest request) {
        AiProvider provider = resolveProvider(providerType);

        ModelConfig existing = modelConfigMapper.selectByProviderTypeIncludingDeleted(providerType);
        boolean isNew = (existing == null);
        ModelConfig entity = isNew ? new ModelConfig() : existing;

        entity.setProviderType(providerType);
        entity.setBaseUrl(request.getBaseUrl());
        if (StringUtils.hasText(request.getApiKey())) {
            entity.setApiKeyEncrypted(encryptApiKey(request.getApiKey()));
        }
        entity.setModelCode(request.getModelCode());
        entity.setModelName(request.getModelName());
        entity.setIsDeleted(0);

        Integer active = request.getIsActive() != null ? request.getIsActive() : 0;
        entity.setIsActive(active);
        entity.setUpdatedBy(currentAdminIdOrZero());

        if (active == 1) {
            deactivateOthers(providerType);
        }

        if (isNew) {
            entity.setCreatedBy(entity.getUpdatedBy());
            modelConfigMapper.insert(entity);
        } else {
            modelConfigMapper.updateByIdIncludingDeleted(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(String providerType) {
        resolveProvider(providerType);
        ModelConfig entity = modelConfigMapper.selectByProviderType(providerType);
        if (entity == null) {
            throw new BusinessException(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND);
        }
        modelConfigMapper.deleteByProviderType(providerType, currentAdminIdOrZero());
    }

    @Override
    public List<ModelOptionVO> fetchModels(String providerType, ModelConfigConnectionRequest request) {
        AiProvider provider = resolveProvider(providerType);
        try {
            return clientFor(provider).fetchModels(request.getBaseUrl(), request.getApiKey());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("fetch models failed, provider={}", providerType, e);
            throw new BusinessException(AdminModelConfigErrorCode.FETCH_MODELS_FAILED);
        }
    }

    @Override
    public boolean testConnection(String providerType, ModelConfigConnectionRequest request) {
        AiProvider provider = resolveProvider(providerType);
        try {
            return clientFor(provider).testConnection(request.getBaseUrl(), request.getApiKey());
        } catch (Exception e) {
            log.error("test connection failed, provider={}", providerType, e);
            throw new BusinessException(AdminModelConfigErrorCode.TEST_CONNECTION_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleActive(String providerType, ModelConfigActiveRequest request) {
        resolveProvider(providerType);
        ModelConfig entity = modelConfigMapper.selectByProviderType(providerType);
        if (entity == null) {
            throw new BusinessException(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND);
        }

        Integer active = request.getIsActive() != null ? request.getIsActive() : 0;
        entity.setIsActive(active);
        entity.setUpdatedBy(currentAdminIdOrZero());

        if (active == 1) {
            deactivateOthers(providerType);
        }
        modelConfigMapper.updateByIdIncludingDeleted(entity);
    }

    private AiProvider resolveProvider(String providerType) {
        return AiProvider.fromCode(providerType)
                .orElseThrow(() -> new BusinessException(AdminModelConfigErrorCode.PROVIDER_NOT_SUPPORTED));
    }

    private AiProviderClient clientFor(AiProvider provider) {
        return switch (provider) {
            case KIMI -> kimiProviderClient;
            case MINIMAX -> minimaxProviderClient;
        };
    }

    private ModelConfigVO toVo(ModelConfig entity, AiProvider provider) {
        ModelConfigVO vo = new ModelConfigVO();
        vo.setProviderType(provider.getCode());
        vo.setProviderName(provider.getName());
        if (entity != null) {
            vo.setId(entity.getId());
            vo.setBaseUrl(entity.getBaseUrl());
            vo.setModelCode(entity.getModelCode());
            vo.setModelName(entity.getModelName());
            vo.setIsActive(entity.getIsActive());
        } else {
            vo.setBaseUrl("");
            vo.setModelCode("");
            vo.setModelName("");
            vo.setIsActive(0);
        }
        return vo;
    }

    private String encryptApiKey(String plain) {
        try {
            return AesUtil.encrypt(plain, apiKeySecret);
        } catch (Exception e) {
            log.error("encrypt api key failed", e);
            throw new BusinessException(AdminModelConfigErrorCode.API_KEY_ENCRYPT_FAILED);
        }
    }

    private void deactivateOthers(String providerType) {
        LambdaUpdateWrapper<ModelConfig> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ModelConfig::getIsActive, 1).ne(ModelConfig::getProviderType, providerType);

        ModelConfig update = new ModelConfig();
        update.setIsActive(0);
        update.setUpdatedBy(currentAdminIdOrZero());
        modelConfigMapper.update(update, wrapper);
    }

    private Long currentAdminIdOrZero() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return adminId != null ? adminId : 0L;
    }
}
