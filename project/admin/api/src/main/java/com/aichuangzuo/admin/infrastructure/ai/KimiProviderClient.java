package com.aichuangzuo.admin.infrastructure.ai;

import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigChatTestVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KimiProviderClient implements AiProviderClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        String url = trim(baseUrl) + "/v1/chat/completions";
        return executeRaw(url, apiKey, stream, requestJson);
    }

    /**
     * 通用原始请求执行器：把请求体原样发出，把响应按字节原样读回（流式拼接成 SSE 原文），
     * HTTP 4xx/5xx 不抛异常，直接把状态码和错误响应体塞进返回结果。
     */
    protected ModelConfigChatTestVO executeRaw(String url, String apiKey, boolean stream,
                                              String requestJson) {
        RequestCallback requestCallback = req -> {
            req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            req.getHeaders().setBearerAuth(apiKey);
            req.getHeaders().setAccept(java.util.Collections.singletonList(
                    stream ? MediaType.TEXT_EVENT_STREAM : MediaType.APPLICATION_JSON));
            req.getBody().write(requestJson.getBytes(StandardCharsets.UTF_8));
        };

        ResponseExtractor<ModelConfigChatTestVO> extractor = response -> {
            ModelConfigChatTestVO vo = new ModelConfigChatTestVO();
            vo.setStatusCode(response.getStatusCode().value());
            vo.setRequestHeaders(formatRequestHeaders(apiKey, stream));
            vo.setRequestBody(requestJson);
            vo.setResponseBody(readBody(response));
            return vo;
        };

        try {
            return restTemplate.execute(url, HttpMethod.POST, requestCallback, extractor);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            ModelConfigChatTestVO vo = new ModelConfigChatTestVO();
            vo.setStatusCode(e.getStatusCode().value());
            vo.setRequestHeaders(formatRequestHeaders(apiKey, stream));
            vo.setRequestBody(requestJson);
            vo.setResponseBody(e.getResponseBodyAsString(StandardCharsets.UTF_8));
            return vo;
        } catch (RestClientException e) {
            ModelConfigChatTestVO vo = new ModelConfigChatTestVO();
            vo.setStatusCode(0);
            vo.setRequestHeaders(formatRequestHeaders(apiKey, stream));
            vo.setRequestBody(requestJson);
            vo.setResponseBody("请求失败：" + e.getMessage());
            return vo;
        }
    }

    private String formatRequestHeaders(String apiKey, boolean stream) {
        return "Content-Type: application/json\n"
                + "Accept: " + (stream ? "text/event-stream" : "application/json") + "\n"
                + "Authorization: Bearer " + (apiKey == null ? "" : apiKey);
    }

    private String readBody(ClientHttpResponse response) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private String trim(String baseUrl) {
        if (baseUrl == null) return "";
        String s = baseUrl.trim().replaceAll("/+$", "");
        int schemeEnd = s.indexOf("://");
        if (schemeEnd >= 0) {
            int pathStart = s.indexOf('/', schemeEnd + 3);
            if (pathStart > 0) s = s.substring(0, pathStart);
        }
        return s;
    }
}
