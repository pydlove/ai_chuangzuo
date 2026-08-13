package com.aichuangzuo.admin.modules.modelconfig.service.impl;

import com.aichuangzuo.admin.infrastructure.ai.AiProvider;
import com.aichuangzuo.admin.infrastructure.ai.AiProviderClient;
import com.aichuangzuo.admin.infrastructure.ai.KimiProviderClient;
import com.aichuangzuo.admin.infrastructure.ai.MinimaxProviderClient;
import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigChatTestRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigConnectionRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.entity.ProviderModel;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ProviderModelMapper;
import com.aichuangzuo.admin.modules.modelconfig.service.ModelConfigService;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigChatTestVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.aichuangzuo.shared.enums.error.AdminModelConfigErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;
    private final ProviderModelMapper providerModelMapper;
    private final KimiProviderClient kimiProviderClient;
    private final MinimaxProviderClient minimaxProviderClient;

    @Value("${admin.model.api-key-secret}")
    private String apiKeySecret;

    @Override
    public List<ModelConfigVO> listConfigs() {
        LambdaQueryWrapper<ModelConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ModelConfig::getIsDeleted, 0);
        wrapper.orderByAsc(ModelConfig::getPriority, ModelConfig::getId);
        return modelConfigMapper.selectList(wrapper).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public ModelConfigVO getConfig(Long id) {
        ModelConfig entity = requireConfig(id);
        return toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(ModelConfigSaveRequest request) {
        AiProvider provider = resolveProvider(request.getProviderType());
        checkNameUnique(request.getName(), null);

        ModelConfig entity = new ModelConfig();
        entity.setProviderType(provider.getCode());
        entity.setName(request.getName().trim());
        entity.setBaseUrl(request.getBaseUrl());
        entity.setApiKeyEncrypted(encryptApiKey(request.getApiKey()));
        entity.setModelCode(request.getModelCode());
        entity.setModelName(request.getModelName());
        entity.setPriority(request.getPriority());
        entity.setIsActive(request.getIsActive());
        entity.setIsDeleted(0);
        entity.setCreatedBy(currentAdminIdOrZero());
        entity.setUpdatedBy(entity.getCreatedBy());

        modelConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(Long id, ModelConfigSaveRequest request) {
        AiProvider provider = resolveProvider(request.getProviderType());
        ModelConfig entity = requireConfig(id);
        checkNameUnique(request.getName(), id);

        entity.setProviderType(provider.getCode());
        entity.setName(request.getName().trim());
        entity.setBaseUrl(request.getBaseUrl());
        if (StringUtils.hasText(request.getApiKey())) {
            entity.setApiKeyEncrypted(encryptApiKey(request.getApiKey()));
        }
        entity.setModelCode(request.getModelCode());
        entity.setModelName(request.getModelName());
        entity.setPriority(request.getPriority());
        entity.setIsActive(request.getIsActive());
        entity.setUpdatedBy(currentAdminIdOrZero());

        modelConfigMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        ModelConfig entity = requireConfig(id);
        entity.setIsDeleted(1);
        entity.setUpdatedBy(currentAdminIdOrZero());
        modelConfigMapper.updateById(entity);
    }

    @Override
    public List<ModelOptionVO> fetchModels(ModelConfigConnectionRequest request) {
        AiProvider provider = resolveProvider(request.getProviderType());
        try {
            List<ModelOptionVO> models = clientFor(provider).fetchModels(request.getBaseUrl(), request.getApiKey());
            upsertProviderModels(provider.getCode(), models);
            return models;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("fetch models failed, provider={}", request.getProviderType(), e);
            throw new BusinessException(AdminModelConfigErrorCode.FETCH_MODELS_FAILED);
        }
    }

    @Override
    public List<ModelOptionVO> listProviderModels(String providerType) {
        AiProvider provider = resolveProvider(providerType);
        return providerModelMapper.selectByProviderType(provider.getCode()).stream()
                .map(m -> {
                    ModelOptionVO vo = new ModelOptionVO();
                    vo.setModelCode(m.getModelCode());
                    vo.setModelName(m.getModelName());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean testConnection(ModelConfigConnectionRequest request) {
        AiProvider provider = resolveProvider(request.getProviderType());
        try {
            return clientFor(provider).testConnection(request.getBaseUrl(), request.getApiKey());
        } catch (Exception e) {
            log.error("test connection failed, provider={}", request.getProviderType(), e);
            throw new BusinessException(AdminModelConfigErrorCode.TEST_CONNECTION_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleActive(Long id, ModelConfigActiveRequest request) {
        ModelConfig entity = requireConfig(id);
        entity.setIsActive(request.getIsActive());
        entity.setUpdatedBy(currentAdminIdOrZero());
        modelConfigMapper.updateById(entity);
    }

    @Override
    public ModelConfigChatTestVO chatTest(ModelConfigChatTestRequest request) {
        AiProvider provider = resolveProvider(request.getProviderType());
        boolean stream = Boolean.TRUE.equals(request.getStream());
        return clientFor(provider).chatTest(
                request.getBaseUrl(),
                request.getApiKey(),
                request.getModelCode(),
                request.getPrompt(),
                stream);
    }

    private void upsertProviderModels(String providerType, List<ModelOptionVO> models) {
        if (models == null || models.isEmpty()) {
            return;
        }
        List<ProviderModel> existing = providerModelMapper.selectByProviderType(providerType);
        Map<String, ProviderModel> existingMap = existing.stream()
                .collect(Collectors.toMap(ProviderModel::getModelCode, m -> m, (a, b) -> a));

        Set<String> fetchedCodes = new HashSet<>();
        for (ModelOptionVO vo : models) {
            String code = vo.getModelCode();
            if (!StringUtils.hasText(code)) {
                continue;
            }
            fetchedCodes.add(code);
            ProviderModel entity = existingMap.get(code);
            if (entity == null) {
                entity = new ProviderModel();
                entity.setProviderType(providerType);
                entity.setModelCode(code);
                entity.setModelName(vo.getModelName());
                entity.setIsDeleted(0);
                providerModelMapper.insert(entity);
            } else if (entity.getIsDeleted() != null && entity.getIsDeleted() == 1) {
                entity.setIsDeleted(0);
                entity.setModelName(vo.getModelName());
                providerModelMapper.updateById(entity);
            } else if (!Objects.equals(entity.getModelName(), vo.getModelName())) {
                entity.setModelName(vo.getModelName());
                providerModelMapper.updateById(entity);
            }
        }

        for (ProviderModel entity : existing) {
            if (!fetchedCodes.contains(entity.getModelCode()) && entity.getIsDeleted() != null && entity.getIsDeleted() == 0) {
                entity.setIsDeleted(1);
                providerModelMapper.updateById(entity);
            }
        }
    }

    private ModelConfig requireConfig(Long id) {
        ModelConfig entity = modelConfigMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() != null && entity.getIsDeleted() == 1) {
            throw new BusinessException(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND);
        }
        return entity;
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

    private void checkNameUnique(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return;
        }
        long count = modelConfigMapper.countByNameExcludingId(name.trim(), excludeId == null ? 0L : excludeId);
        if (count > 0) {
            throw new BusinessException(AdminModelConfigErrorCode.MODEL_CONFIG_NAME_DUPLICATE);
        }
    }

    private ModelConfigVO toVo(ModelConfig entity) {
        AiProvider provider = AiProvider.fromCode(entity.getProviderType()).orElse(null);
        ModelConfigVO vo = new ModelConfigVO();
        vo.setId(entity.getId());
        vo.setProviderType(entity.getProviderType());
        vo.setProviderName(provider != null ? provider.getName() : entity.getProviderType());
        vo.setName(entity.getName());
        vo.setBaseUrl(entity.getBaseUrl());
        vo.setApiKey(decryptApiKeyOrEmpty(entity.getApiKeyEncrypted()));
        vo.setModelCode(entity.getModelCode());
        vo.setModelName(entity.getModelName());
        vo.setPriority(entity.getPriority());
        vo.setIsActive(entity.getIsActive());
        return vo;
    }

    private String decryptApiKeyOrEmpty(String encrypted) {
        if (!StringUtils.hasText(encrypted)) {
            return "";
        }
        try {
            return AesUtil.decrypt(encrypted, apiKeySecret);
        } catch (Exception e) {
            log.warn("decrypt api key failed", e);
            return "";
        }
    }

    private String encryptApiKey(String plain) {
        if (!StringUtils.hasText(plain)) {
            return "";
        }
        try {
            return AesUtil.encrypt(plain, apiKeySecret);
        } catch (Exception e) {
            log.error("encrypt api key failed", e);
            throw new BusinessException(AdminModelConfigErrorCode.API_KEY_ENCRYPT_FAILED);
        }
    }

    private Long currentAdminIdOrZero() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return adminId != null ? adminId : 0L;
    }
}
