package com.aichuangzuo.admin.modules.generation.service;

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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 验证 GenerationAiService 把 modelParams merge 进 LLM 请求体。
 */
@ExtendWith(MockitoExtension.class)
class GenerationAiServiceModelParamsTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;

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

        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}"));

        service = new TestableGenerationAiService(modelConfigMapper, "test-secret-1234", restTemplate);
    }

    @Test
    void call_shouldUseDefaultParamsWhenModelParamsNull() {
        service.call(1L, "sys", "user", null);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(0.7, body.get("temperature"));
        assertEquals(8192, body.get("max_tokens"));
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
        assertEquals(8192, body.get("max_tokens"));  // 未知字段不影响
    }

    /**
     * 测试用子类：注入 mock RestTemplate，避免真实 HTTP 调用。
     */
    static class TestableGenerationAiService extends GenerationAiService {
        private final RestTemplate mockRestTemplate;

        TestableGenerationAiService(ModelConfigMapper mapper, String secret, RestTemplate restTemplate) {
            super(mapper, secret);
            this.mockRestTemplate = restTemplate;
        }

        @Override
        public org.springframework.web.client.RestTemplate getRestTemplate() {
            return mockRestTemplate;
        }
    }
}