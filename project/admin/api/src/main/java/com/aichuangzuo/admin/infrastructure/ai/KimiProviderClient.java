package com.aichuangzuo.admin.infrastructure.ai;

import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KimiProviderClient implements AiProviderClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Map<String, String> FRIENDLY_NAMES = Map.of(
            "moonshot-v1-8k", "Moonshot V1 8K",
            "moonshot-v1-32k", "Moonshot V1 32K",
            "moonshot-v1-128k", "Moonshot V1 128K"
    );

    @Override
    public boolean testConnection(String baseUrl, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(trim(baseUrl) + "/v1/models", HttpMethod.GET, entity, String.class);
            return true;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            return false;
        } catch (RestClientException e) {
            log.warn("Kimi test connection failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<ModelOptionVO> fetchModels(String baseUrl, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                trim(baseUrl) + "/v1/models", HttpMethod.GET, entity, Map.class);

        Object data = response.getBody() != null ? response.getBody().get("data") : null;
        if (!(data instanceof List)) {
            return Collections.emptyList();
        }

        return ((List<Map<String, Object>>) data).stream()
                .map(m -> {
                    String code = (String) m.get("id");
                    ModelOptionVO vo = new ModelOptionVO();
                    vo.setModelCode(code);
                    vo.setModelName(FRIENDLY_NAMES.getOrDefault(code, code));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private String trim(String baseUrl) {
        return baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    }
}
