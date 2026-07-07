package com.aichuangzuo.admin.infrastructure.ai;

import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigChatTestVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
public class MinimaxProviderClient implements AiProviderClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    trim(baseUrl) + "/v1/models", HttpMethod.GET, entity, Map.class);
            Object data = response.getBody() != null ? response.getBody().get("data") : null;
            if (!(data instanceof List)) {
                log.warn("MiniMax fetch models: response has no 'data' list");
                return Collections.emptyList();
            }
            return ((List<Map<String, Object>>) data).stream()
                    .map(m -> {
                        String code = (String) m.get("id");
                        if (code == null || code.isBlank()) return null;
                        ModelOptionVO vo = new ModelOptionVO();
                        vo.setModelCode(code);
                        vo.setModelName(code);
                        return vo;
                    })
                    .filter(vo -> vo != null)
                    .collect(Collectors.toList());
        } catch (RestClientException e) {
            log.warn("MiniMax fetch models failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public ModelConfigChatTestVO chatTest(String baseUrl, String apiKey, String modelCode,
                                          String prompt, boolean stream) {
        Map<String, Object> body = Map.of(
                "model", modelCode,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", stream
        );
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("serialize chat request failed", e);
        }
        String url = trim(baseUrl) + "/v1/text/chatcompletion_v2";
        return executeRaw(url, apiKey, stream, requestJson);
    }

    private ModelConfigChatTestVO executeRaw(String url, String apiKey, boolean stream,
                                             String requestJson) {
        org.springframework.web.client.RequestCallback requestCallback = req -> {
            req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            req.getHeaders().setBearerAuth(apiKey);
            req.getHeaders().setAccept(java.util.Collections.singletonList(
                    stream ? MediaType.TEXT_EVENT_STREAM : MediaType.APPLICATION_JSON));
            req.getBody().write(requestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        };

        org.springframework.web.client.ResponseExtractor<ModelConfigChatTestVO> extractor = response -> {
            ModelConfigChatTestVO vo = new ModelConfigChatTestVO();
            vo.setStatusCode(response.getStatusCode().value());
            vo.setRequestHeaders("Content-Type: application/json\n"
                    + "Accept: " + (stream ? "text/event-stream" : "application/json") + "\n"
                    + "Authorization: Bearer " + (apiKey == null ? "" : apiKey));
            vo.setRequestBody(requestJson);
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.getBody(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            vo.setResponseBody(sb.toString());
            return vo;
        };

        try {
            return restTemplate.execute(url, HttpMethod.POST, requestCallback, extractor);
        } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            ModelConfigChatTestVO vo = new ModelConfigChatTestVO();
            vo.setStatusCode(e.getStatusCode().value());
            vo.setRequestHeaders("Content-Type: application/json\n"
                    + "Accept: " + (stream ? "text/event-stream" : "application/json") + "\n"
                    + "Authorization: Bearer " + (apiKey == null ? "" : apiKey));
            vo.setRequestBody(requestJson);
            vo.setResponseBody(e.getResponseBodyAsString(java.nio.charset.StandardCharsets.UTF_8));
            return vo;
        } catch (RestClientException e) {
            ModelConfigChatTestVO vo = new ModelConfigChatTestVO();
            vo.setStatusCode(0);
            vo.setRequestHeaders("Content-Type: application/json\n"
                    + "Accept: " + (stream ? "text/event-stream" : "application/json") + "\n"
                    + "Authorization: Bearer " + (apiKey == null ? "" : apiKey));
            vo.setRequestBody(requestJson);
            vo.setResponseBody("请求失败：" + e.getMessage());
            return vo;
        }
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
