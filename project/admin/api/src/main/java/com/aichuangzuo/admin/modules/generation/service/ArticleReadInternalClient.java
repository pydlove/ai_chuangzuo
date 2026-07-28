package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.vo.GeneratedArticleVO;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：读取已生成的 u_article 内容。
 *
 * <p>调用 user-api 的 {@code /api/v1/user/internal/generation/article/{articleBizNo}}，
 * 通过 {@code X-Internal-Key} 走 InternalKeyAuthenticationFilter 校验。
 */
@Slf4j
@Service
public class ArticleReadInternalClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public ArticleReadInternalClient(@Value("${user.api.base-url}") String baseUrl,
                                     @Value("${user.api.internal-key}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    public GeneratedArticleVO getArticle(String articleBizNo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/user/internal/generation/article/" + articleBizNo,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null || !Integer.valueOf(0).equals(body.get("code"))) {
                log.warn("读取 article 失败 user-api 响应: {}", body);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_ARTICLE_NOT_FOUND);
            }
            Object data = body.get("data");
            if (data == null) {
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_ARTICLE_NOT_FOUND);
            }
            // RestTemplate 默认把 JSON 对象反序列化为 LinkedHashMap
            return mapToVo((Map<?, ?>) data);
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("调用 user-api 读 article 失败：{}", e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_ARTICLE_NOT_FOUND);
        }
    }

    @SuppressWarnings("unchecked")
    private GeneratedArticleVO mapToVo(Map<?, ?> data) {
        GeneratedArticleVO vo = new GeneratedArticleVO();
        vo.setBizNo(asString(data.get("bizNo")));
        vo.setTitle(asString(data.get("title")));
        vo.setBody(asString(data.get("body")));
        vo.setPlatform(asString(data.get("platform")));
        vo.setSkill(asString(data.get("skill")));
        vo.setSkillName(asString(data.get("skillName")));
        vo.setTemplate(asString(data.get("template")));
        vo.setDescription(asString(data.get("description")));
        vo.setWordCount(asInt(data.get("wordCount")));
        Object tags = data.get("tags");
        if (tags instanceof List<?> list) {
            List<String> tagList = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    tagList.add(item.toString());
                }
            }
            vo.setTags(tagList);
        }
        return vo;
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
