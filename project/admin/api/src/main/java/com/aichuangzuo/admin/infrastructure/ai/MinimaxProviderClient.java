package com.aichuangzuo.admin.infrastructure.ai;

import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MinimaxProviderClient implements AiProviderClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final List<ModelOptionVO> DEFAULT_MODELS = List.of(
            option("abab6.5s-chat", "abab6.5s-chat"),
            option("abab6-chat", "abab6-chat")
    );

    @Override
    public boolean testConnection(String baseUrl, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", "abab6.5s-chat",
                    "messages", List.of(Map.of("role", "user", "content", "hi"))
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    trim(baseUrl) + "/v1/text/chatcompletion_v2", entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            return false;
        } catch (RestClientException e) {
            log.warn("MiniMax test connection failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<ModelOptionVO> fetchModels(String baseUrl, String apiKey) {
        return DEFAULT_MODELS;
    }

    private static ModelOptionVO option(String code, String name) {
        ModelOptionVO vo = new ModelOptionVO();
        vo.setModelCode(code);
        vo.setModelName(name);
        return vo;
    }

    private String trim(String baseUrl) {
        return baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    }
}
