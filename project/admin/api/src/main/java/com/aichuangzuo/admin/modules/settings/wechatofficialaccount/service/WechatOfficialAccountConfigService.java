package com.aichuangzuo.admin.modules.settings.wechatofficialaccount.service;

import com.aichuangzuo.admin.modules.settings.wechatofficialaccount.dto.request.WechatOfficialAccountConfigUpdateRequest;
import com.aichuangzuo.admin.modules.settings.wechatofficialaccount.mapper.WechatOfficialAccountConfigMapper;
import com.aichuangzuo.admin.modules.settings.wechatofficialaccount.vo.WechatOfficialAccountConfigVO;
import com.aichuangzuo.shared.entity.WechatOfficialAccountConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 微信公众号配置服务。
 *
 * <p>单行配置（id=1），admin 端维护。AppSecret 落库前用 Jasypt 加密。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatOfficialAccountConfigService {

    private static final long CONFIG_ID = 1L;
    private static final Pattern MASKED_SECRET = Pattern.compile("^\\*+$");
    private static final long ACCESS_TOKEN_EXPIRE_BUFFER_MS = 5 * 60 * 1000;

    private final WechatOfficialAccountConfigMapper mapper;
    private final StringEncryptor encryptor;

    private final AtomicReference<String> accessTokenCache = new AtomicReference<>();
    private volatile long accessTokenExpireAt = 0L;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatOfficialAccountConfigVO detail() {
        WechatOfficialAccountConfig config = mapper.selectById(CONFIG_ID);
        if (config == null) {
            config = defaultConfig();
        }
        return toVo(config);
    }

    /**
     * 获取配置实体（AppSecret 已解密）。
     */
    public WechatOfficialAccountConfig getConfig() {
        WechatOfficialAccountConfig config = mapper.selectById(CONFIG_ID);
        if (config == null) {
            config = defaultConfig();
        }
        if (StringUtils.hasText(config.getAppSecret())) {
            try {
                config.setAppSecret(encryptor.decrypt(config.getAppSecret()));
            } catch (Exception e) {
                log.warn("微信公众号 AppSecret 解密失败");
                config.setAppSecret(null);
            }
        }
        return config;
    }

    @Transactional
    public WechatOfficialAccountConfigVO update(WechatOfficialAccountConfigUpdateRequest request, Long adminUserId) {
        WechatOfficialAccountConfig exist = mapper.selectById(CONFIG_ID);
        boolean isNew = exist == null;
        if (isNew) {
            exist = defaultConfig();
        }

        exist.setAppId(trim(request.getAppId()));
        exist.setToken(trim(request.getToken()));
        exist.setPlaintextMode(request.getPlaintextMode());
        exist.setEnabled(request.getEnabled());

        String secret = trim(request.getAppSecret());
        if (StringUtils.hasText(secret) && !MASKED_SECRET.matcher(secret).matches()) {
            exist.setAppSecret(encryptor.encrypt(secret));
        } else if (isNew) {
            exist.setAppSecret(null);
        }

        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        if (isNew) {
            mapper.insert(exist);
        } else {
            mapper.updateById(exist);
        }

        log.info("admin={} 更新微信公众号配置", adminUserId);
        return toVo(exist);
    }

    /**
     * 发布微信公众号自定义菜单。
     */
    public void publishMenu() {
        WechatOfficialAccountConfig config = getConfig();
        if (!StringUtils.hasText(config.getAppId()) || !StringUtils.hasText(config.getAppSecret())) {
            throw new IllegalStateException("微信公众号 AppID 或 AppSecret 未配置");
        }

        String accessToken = getAccessToken(config);
        String url = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=" + accessToken;

        Map<String, Object> menu = buildMenu();
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(menu);
        } catch (Exception e) {
            throw new RuntimeException("生成菜单 JSON 失败", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setContentLength(jsonBody.getBytes(StandardCharsets.UTF_8).length);
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("发布微信菜单失败：无响应");
        }

        Map<String, Object> result;
        try {
            result = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("解析微信菜单响应失败", e);
        }
        Object errcode = result.get("errcode");
        if (errcode != null && !Integer.valueOf(0).equals(errcode)) {
            log.error("发布微信菜单失败, result={}", result);
            if (Integer.valueOf(48001).equals(errcode)) {
                throw new RuntimeException(
                        "发布微信菜单失败：当前公众号未获得自定义菜单接口权限。未认证订阅号无法通过 API 创建菜单，请完成微信认证或升级为服务号。");
            }
            throw new RuntimeException("发布微信菜单失败：" + result.get("errmsg"));
        }
        log.info("发布微信公众号菜单成功");
    }

    private Map<String, Object> buildMenu() {
        Map<String, Object> startCreation = new HashMap<>();
        startCreation.put("type", "click");
        startCreation.put("name", "开始创作");
        startCreation.put("key", "start_creation");

        Map<String, Object> learn = new HashMap<>();
        learn.put("type", "click");
        learn.put("name", "创作学院");
        learn.put("key", "learn");

        Map<String, Object> bindAccount = new HashMap<>();
        bindAccount.put("type", "click");
        bindAccount.put("name", "绑定账号");
        bindAccount.put("key", "bind_account");

        Map<String, Object> helpDoc = new HashMap<>();
        helpDoc.put("type", "click");
        helpDoc.put("name", "帮助文档");
        helpDoc.put("key", "help_doc");

        Map<String, Object> mine = new HashMap<>();
        mine.put("name", "我的");
        mine.put("sub_button", List.of(bindAccount, helpDoc));

        Map<String, Object> menu = new HashMap<>();
        menu.put("button", List.of(startCreation, learn, mine));
        return menu;
    }

    private synchronized String getAccessToken(WechatOfficialAccountConfig config) {
        String cached = accessTokenCache.get();
        if (cached != null && System.currentTimeMillis() < accessTokenExpireAt) {
            return cached;
        }

        String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                config.getAppId(), config.getAppSecret());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restTemplate.getForObject(url, Map.class);
        if (result == null || result.get("access_token") == null) {
            log.error("获取微信 access_token 失败, response={}", result);
            throw new RuntimeException("获取微信 access_token 失败");
        }

        String accessToken = (String) result.get("access_token");
        Integer expiresIn = (Integer) result.getOrDefault("expires_in", 7200);
        long validMs = Math.max(0, expiresIn * 1000L - ACCESS_TOKEN_EXPIRE_BUFFER_MS);

        accessTokenCache.set(accessToken);
        accessTokenExpireAt = System.currentTimeMillis() + validMs;
        return accessToken;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private WechatOfficialAccountConfig defaultConfig() {
        WechatOfficialAccountConfig config = new WechatOfficialAccountConfig();
        config.setId(CONFIG_ID);
        config.setAppId("");
        config.setAppSecret("");
        config.setToken("");
        config.setPlaintextMode(1);
        config.setEnabled(0);
        return config;
    }

    private WechatOfficialAccountConfigVO toVo(WechatOfficialAccountConfig config) {
        WechatOfficialAccountConfigVO vo = new WechatOfficialAccountConfigVO();
        BeanUtils.copyProperties(config, vo);
        if (StringUtils.hasText(config.getAppSecret())) {
            try {
                vo.setAppSecret(encryptor.decrypt(config.getAppSecret()));
            } catch (Exception e) {
                log.warn("微信公众号 AppSecret 解密失败，返回空");
                vo.setAppSecret("");
            }
        }
        return vo;
    }
}
