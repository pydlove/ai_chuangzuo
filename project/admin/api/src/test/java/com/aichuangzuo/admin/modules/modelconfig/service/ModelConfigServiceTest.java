package com.aichuangzuo.admin.modules.modelconfig.service;

import com.aichuangzuo.admin.infrastructure.ai.KimiProviderClient;
import com.aichuangzuo.admin.infrastructure.ai.MinimaxProviderClient;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.modelconfig.service.impl.ModelConfigServiceImpl;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.shared.enums.error.AdminModelConfigErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;

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
    void listConfigs_shouldReturnAllProviders() {
        ModelConfig config = new ModelConfig();
        config.setId(1L);
        config.setProviderType("kimi");
        config.setBaseUrl("https://api.moonshot.cn");
        config.setModelCode("moonshot-v1-8k");
        config.setIsActive(1);

        when(modelConfigMapper.selectByProviderType("kimi")).thenReturn(config);
        when(modelConfigMapper.selectByProviderType("minimax")).thenReturn(null);

        List<ModelConfigVO> result = modelConfigService.listConfigs();

        assertEquals(2, result.size());
        assertEquals("Kimi", result.get(0).getProviderName());
        assertEquals("moonshot-v1-8k", result.get(0).getModelCode());
        assertEquals("MiniMax", result.get(1).getProviderName());
        assertEquals(0, result.get(1).getIsActive());
    }

    @Test
    void saveConfig_shouldCreateAndEncryptApiKey() {
        when(modelConfigMapper.selectByProviderTypeIncludingDeleted("kimi")).thenReturn(null);

        ModelConfigSaveRequest request = new ModelConfigSaveRequest();
        request.setBaseUrl("https://api.moonshot.cn");
        request.setApiKey("sk-test");
        request.setModelCode("moonshot-v1-8k");
        request.setModelName("Moonshot V1 8K");
        request.setIsActive(0);

        modelConfigService.saveConfig("kimi", request);

        verify(modelConfigMapper).insert(any(ModelConfig.class));
    }

    @Test
    void deleteConfig_shouldThrowWhenNotFound() {
        when(modelConfigMapper.selectByProviderType("kimi")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> modelConfigService.deleteConfig("kimi"));
        assertEquals(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void toggleActive_shouldThrowWhenNotFound() {
        when(modelConfigMapper.selectByProviderType("minimax")).thenReturn(null);

        ModelConfigActiveRequest request = new ModelConfigActiveRequest();
        request.setIsActive(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> modelConfigService.toggleActive("minimax", request));
        assertEquals(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND.getCode(), ex.getCode());
    }
}
