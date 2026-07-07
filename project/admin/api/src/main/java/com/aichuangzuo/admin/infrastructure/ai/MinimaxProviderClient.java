package com.aichuangzuo.admin.infrastructure.ai;

import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            if (!response.getStatusCode().is2xxSuccessful()) {
                return false;
            }
            // MiniMax 在鉴权/参数错误时仍返回 HTTP 200，业务状态码在 body 的 base_resp.status_code
            return isBusinessSuccess(response.getBody());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            return false;
        } catch (RestClientException e) {
            log.warn("MiniMax test connection failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isBusinessSuccess(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode baseResp = root.path("base_resp");
            if (baseResp.isMissingNode()) {
                return true;
            }
            int statusCode = baseResp.path("status_code").asInt(-1);
            if (statusCode != 0) {
                String msg = baseResp.path("status_msg").asText("");
                log.warn("MiniMax test connection business error: code={}, msg={}", statusCode, msg);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("MiniMax test connection response parse failed: {}", e.getMessage());
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
        if (baseUrl == null) return "";
        String s = baseUrl.trim().replaceAll("/+$", "");
        // 仅保留 scheme://host[:port]，去掉路径（避免 baseUrl 自带 /v1 时路径重复）
        int schemeEnd = s.indexOf("://");
        if (schemeEnd >= 0) {
            int pathStart = s.indexOf('/', schemeEnd + 3);
            if (pathStart > 0) s = s.substring(0, pathStart);
        }
        return s;
    }
}
