package com.aichuangzuo.admin.modules.user.service;

import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：修改指定用户头像。
 *
 * <p>调用 user-api 内部接口，走与用户本人上传一致的存储逻辑，保证各端展示一致。
 */
@Slf4j
@Service
public class UserProfileInternalClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public UserProfileInternalClient(@Value("${user.api.base-url}") String baseUrl,
                                     @Value("${user.api.internal-key}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    public void updateAvatar(Long userId, byte[] imageBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        ByteArrayResource fileResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/user/internal/users/" + userId + "/avatar",
                    new HttpEntity<>(body, headers),
                    Map.class);
            Map respBody = response.getBody();
            if (respBody == null || !Integer.valueOf(0).equals(respBody.get("code"))) {
                log.warn("修改用户头像失败 user-api 响应: {}", respBody);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("调用 user-api 修改用户头像失败：{}", e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
        }
    }
}
