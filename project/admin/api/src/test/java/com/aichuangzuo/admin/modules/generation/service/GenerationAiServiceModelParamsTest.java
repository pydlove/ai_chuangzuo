package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.entity.GenerationConfig;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.shared.utils.AesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 验证 GenerationAiService 把 modelParams merge 进 LLM 请求体。
 *
 * <p>三级回退：stage modelParams > 创作设置默认 > 硬编码兜底。
 */
@ExtendWith(MockitoExtension.class)
class GenerationAiServiceModelParamsTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;

    @Mock
    private GenerationConfigService generationConfigService;

    @Mock
    private RestTemplate restTemplate;

    private GenerationAiService service;

    @BeforeEach
    void setup() throws Exception {
        ModelConfig cfg = new ModelConfig();
        cfg.setId(1L);
        cfg.setModelCode("test-model");
        cfg.setProviderType("kimi");
        cfg.setBaseUrl("https://api.example.com/");
        // 用 AES 真实加密 fake-key，方便 AesUtil.decrypt 还原
        cfg.setApiKeyEncrypted(AesUtil.encrypt("fake-key", "test-secret-1234"));
        cfg.setIsActive(1);
        when(modelConfigMapper.selectById(1L)).thenReturn(cfg);

        // 创作设置默认：temp 0.7 / max_tokens 8192 / top_p 1.0
        GenerationConfig genCfg = new GenerationConfig();
        genCfg.setDefaultTemperature(new BigDecimal("0.70"));
        genCfg.setDefaultMaxTokens(8192);
        genCfg.setDefaultTopP(new BigDecimal("1.00"));
        when(generationConfigService.getCurrent()).thenReturn(genCfg);

        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}"));

        service = new TestableGenerationAiService(modelConfigMapper, generationConfigService,
                "test-secret-1234", restTemplate);
    }

    @Test
    void call_shouldUseConfigDefaultsWhenModelParamsNull() {
        service.call(1L, "sys", "user", null);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(0.7, body.get("temperature"));
        assertEquals(8192, body.get("max_tokens"));
        assertEquals(1.0, body.get("top_p"));
    }

    @Test
    void call_shouldOverrideParamsFromModelParams() {
        Map<String, Object> params = Map.of("temperature", 0.3, "max_tokens", 1500, "top_p", 0.9);
        service.call(1L, "sys", "user", params);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(0.3, body.get("temperature"));
        assertEquals(1500, body.get("max_tokens"));
        assertEquals(0.9, body.get("top_p"));
    }

    @Test
    void call_shouldIgnoreUnknownFields() {
        Map<String, Object> params = Map.of("unknown_field", "value", "temperature", 0.5);
        service.call(1L, "sys", "user", params);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(0.5, body.get("temperature"));
        assertEquals(8192, body.get("max_tokens"));  // 未知字段不影响，落回创作设置默认
    }

    @Test
    void call_shouldUseAdminConfigValuesAsFallback() {
        // admin 改了创作设置：temp 0.2 / max_tokens 4096 / top_p 0.8
        GenerationConfig customCfg = new GenerationConfig();
        customCfg.setDefaultTemperature(new BigDecimal("0.20"));
        customCfg.setDefaultMaxTokens(4096);
        customCfg.setDefaultTopP(new BigDecimal("0.80"));
        when(generationConfigService.getCurrent()).thenReturn(customCfg);

        service.call(1L, "sys", "user", null);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(0.2, body.get("temperature"));
        assertEquals(4096, body.get("max_tokens"));
        assertEquals(0.8, body.get("top_p"));
    }

    @Test
    void call_shouldFallbackToHardcodedWhenConfigFieldNull() {
        // 数据迁移失败的极端场景：config 行的字段是 null，用硬编码兜底
        GenerationConfig nullCfg = new GenerationConfig();
        when(generationConfigService.getCurrent()).thenReturn(nullCfg);

        service.call(1L, "sys", "user", null);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(0.7, body.get("temperature"));
        assertEquals(32768, body.get("max_tokens"));
        assertEquals(1.0, body.get("top_p"));
    }

    @Test
    void call_shouldSendResponseFormatJsonObject() {
        // API 层强制 JSON 模式：让模型必须输出合法 JSON
        service.call(1L, "sys", "user", null);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> rf = (Map<String, Object>) body.get("response_format");
        assertEquals("json_object", rf.get("type"));
    }

    /**
     * 测试用子类：注入 mock RestTemplate，避免真实 HTTP 调用。
     */
    static class TestableGenerationAiService extends GenerationAiService {
        private final RestTemplate mockRestTemplate;

        TestableGenerationAiService(ModelConfigMapper mapper,
                                    GenerationConfigService generationConfigService,
                                    String secret,
                                    RestTemplate restTemplate) {
            super(mapper, generationConfigService, secret);
            this.mockRestTemplate = restTemplate;
        }

        @Override
        public org.springframework.web.client.RestTemplate getRestTemplate() {
            return mockRestTemplate;
        }
    }
}