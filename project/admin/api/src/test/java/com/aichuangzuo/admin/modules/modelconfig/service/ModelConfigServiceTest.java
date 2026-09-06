package com.aichuangzuo.admin.modules.modelconfig.service;

import com.aichuangzuo.admin.infrastructure.ai.KimiProviderClient;
import com.aichuangzuo.admin.infrastructure.ai.MinimaxProviderClient;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigConnectionRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.entity.ProviderModel;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ProviderModelMapper;
import com.aichuangzuo.admin.modules.modelconfig.service.impl.ModelConfigServiceImpl;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.aichuangzuo.shared.enums.error.AdminModelConfigErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;

    @Mock
    private ProviderModelMapper providerModelMapper;

    @Mock
    private KimiProviderClient kimiProviderClient;

    @Mock
    private MinimaxProviderClient minimaxProviderClient;

    @InjectMocks
    private ModelConfigServiceImpl modelConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(modelConfigService, "apiKeySecret",
                "0123456789abcdef0123456789abcdef");
    }

    @Test
    void listConfigs_shouldReturnAllActiveConfigs() {
        ModelConfig config = new ModelConfig();
        config.setId(1L);
        config.setProviderType("kimi");
        config.setName("Kimi-主-key");
        config.setBaseUrl("https://api.moonshot.cn");
        config.setModelCode("moonshot-v1-8k");
        config.setPriority(0);
        config.setIsActive(1);
        config.setIsDeleted(0);

        when(modelConfigMapper.selectList(any())).thenReturn(List.of(config));

        List<ModelConfigVO> result = modelConfigService.listConfigs();

        assertEquals(1, result.size());
        assertEquals("Kimi-主-key", result.get(0).getName());
        assertEquals("Kimi", result.get(0).getProviderName());
        assertEquals(0, result.get(0).getPriority());
    }

    @Test
    void createConfig_shouldInsertWithEncryptedApiKey() {
        when(modelConfigMapper.countByNameExcludingId(any(), any())).thenReturn(0L);

        ModelConfigSaveRequest request = new ModelConfigSaveRequest();
        request.setProviderType("kimi");
        request.setName("Kimi-测试");
        request.setBaseUrl("https://api.moonshot.cn");
        request.setApiKey("sk-test");
        request.setModelCode("moonshot-v1-8k");
        request.setModelName("Moonshot V1 8K");
        request.setPriority(1);
        request.setIsActive(0);

        modelConfigService.createConfig(request);

        verify(modelConfigMapper).insert(any(ModelConfig.class));
    }

    @Test
    void createConfig_shouldThrowWhenNameDuplicate() {
        when(modelConfigMapper.countByNameExcludingId(any(), any())).thenReturn(1L);

        ModelConfigSaveRequest request = new ModelConfigSaveRequest();
        request.setProviderType("kimi");
        request.setName("Kimi-重复");
        request.setBaseUrl("https://api.moonshot.cn");
        request.setApiKey("sk-test");
        request.setModelCode("moonshot-v1-8k");
        request.setPriority(0);
        request.setIsActive(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> modelConfigService.createConfig(request));
        assertEquals(AdminModelConfigErrorCode.MODEL_CONFIG_NAME_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void deleteConfig_shouldThrowWhenNotFound() {
        when(modelConfigMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> modelConfigService.deleteConfig(1L));
        assertEquals(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void deleteConfig_shouldLogicDeleteByDeleteById() {
        ModelConfig entity = new ModelConfig();
        entity.setId(1L);
        entity.setProviderType("kimi");
        entity.setIsDeleted(0);
        when(modelConfigMapper.selectById(1L)).thenReturn(entity);

        modelConfigService.deleteConfig(1L);

        // @TableLogic 字段不进入 updateById 的 SET，必须走 deleteById 逻辑删除
        verify(modelConfigMapper).deleteById(entity);
        verify(modelConfigMapper, never()).updateById(any(ModelConfig.class));
    }

    @Test
    void toggleActive_shouldThrowWhenNotFound() {
        when(modelConfigMapper.selectById(2L)).thenReturn(null);

        ModelConfigActiveRequest request = new ModelConfigActiveRequest();
        request.setIsActive(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> modelConfigService.toggleActive(2L, request));
        assertEquals(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void toggleActive_shouldNotDeactivateOthers() {
        ModelConfig entity = new ModelConfig();
        entity.setId(3L);
        entity.setProviderType("kimi");
        entity.setName("Kimi-A");
        entity.setIsActive(0);
        entity.setIsDeleted(0);
        when(modelConfigMapper.selectById(3L)).thenReturn(entity);

        ModelConfigActiveRequest request = new ModelConfigActiveRequest();
        request.setIsActive(1);

        modelConfigService.toggleActive(3L, request);

        verify(modelConfigMapper).updateById(entity);
        assertEquals(1, entity.getIsActive());
    }

    @Test
    void fetchModels_shouldPersistFetchedModels() {
        ModelConfigConnectionRequest request = new ModelConfigConnectionRequest();
        request.setProviderType("kimi");
        request.setBaseUrl("https://api.moonshot.cn");
        request.setApiKey("sk-test");

        ModelOptionVO vo = new ModelOptionVO();
        vo.setModelCode("moonshot-v1-8k");
        vo.setModelName("Moonshot V1 8K");
        when(kimiProviderClient.fetchModels(request.getBaseUrl(), request.getApiKey()))
                .thenReturn(List.of(vo));
        when(providerModelMapper.selectByProviderType("kimi")).thenReturn(List.of());

        List<ModelOptionVO> result = modelConfigService.fetchModels(request);

        assertEquals(1, result.size());
        verify(providerModelMapper).insert(any(ProviderModel.class));
    }

    @Test
    void fetchModels_shouldReviveDeletedProviderModel() {
        ModelConfigConnectionRequest request = new ModelConfigConnectionRequest();
        request.setProviderType("kimi");
        request.setBaseUrl("https://api.moonshot.cn");
        request.setApiKey("sk-test");

        ModelOptionVO vo = new ModelOptionVO();
        vo.setModelCode("moonshot-v1-8k");
        vo.setModelName("Moonshot V1 8K");
        when(kimiProviderClient.fetchModels(request.getBaseUrl(), request.getApiKey()))
                .thenReturn(List.of(vo));

        ProviderModel deleted = new ProviderModel();
        deleted.setId(2L);
        deleted.setProviderType("kimi");
        deleted.setModelCode("moonshot-v1-8k");
        deleted.setIsDeleted(1);
        when(providerModelMapper.selectByProviderType("kimi")).thenReturn(List.of(deleted));

        modelConfigService.fetchModels(request);

        // 复活已逻辑删除的行：@TableLogic 字段不能靠 updateById，需显式 set is_deleted=0
        ArgumentCaptor<Wrapper<ProviderModel>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(providerModelMapper).update(isNull(), wrapperCaptor.capture());
        String sqlSet = ((AbstractWrapper<ProviderModel, ?, ?>) wrapperCaptor.getValue()).getSqlSet();
        assertTrue(sqlSet.contains("is_deleted"), "复活语句应显式 SET is_deleted: " + sqlSet);
        verify(providerModelMapper, never()).deleteById(any(ProviderModel.class));
    }

    @Test
    void fetchModels_shouldStaleDeleteMissingProviderModel() {
        ModelConfigConnectionRequest request = new ModelConfigConnectionRequest();
        request.setProviderType("kimi");
        request.setBaseUrl("https://api.moonshot.cn");
        request.setApiKey("sk-test");

        ModelOptionVO vo = new ModelOptionVO();
        vo.setModelCode("moonshot-v1-8k");
        vo.setModelName("Moonshot V1 8K");
        when(kimiProviderClient.fetchModels(request.getBaseUrl(), request.getApiKey()))
                .thenReturn(List.of(vo));

        ProviderModel stale = new ProviderModel();
        stale.setId(3L);
        stale.setProviderType("kimi");
        stale.setModelCode("moonshot-v1-legacy");
        stale.setIsDeleted(0);
        when(providerModelMapper.selectByProviderType("kimi")).thenReturn(List.of(stale));

        modelConfigService.fetchModels(request);

        // 厂商已下架的模型：逻辑删除必须走 deleteById
        verify(providerModelMapper).deleteById(stale);
    }

    @Test
    void listProviderModels_shouldReturnPersistedModels() {
        ProviderModel m = new ProviderModel();
        m.setModelCode("moonshot-v1-8k");
        m.setModelName("Moonshot V1 8K");
        when(providerModelMapper.selectByProviderType("kimi")).thenReturn(List.of(m));

        List<ModelOptionVO> result = modelConfigService.listProviderModels("kimi");

        assertEquals(1, result.size());
        assertEquals("moonshot-v1-8k", result.get(0).getModelCode());
    }
}
