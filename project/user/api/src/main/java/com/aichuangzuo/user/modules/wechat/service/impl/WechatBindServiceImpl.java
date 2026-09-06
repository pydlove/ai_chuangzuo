package com.aichuangzuo.user.modules.wechat.service.impl;

import com.aichuangzuo.user.modules.wechat.entity.UserWechatBind;
import com.aichuangzuo.user.modules.wechat.entity.WechatBindCode;
import com.aichuangzuo.user.modules.wechat.entity.WechatBindQrSession;
import com.aichuangzuo.user.modules.wechat.mapper.UserWechatBindMapper;
import com.aichuangzuo.user.modules.wechat.mapper.WechatBindCodeMapper;
import com.aichuangzuo.user.modules.wechat.mapper.WechatBindQrSessionMapper;
import com.aichuangzuo.user.modules.wechat.service.WechatBindService;
import com.aichuangzuo.user.modules.wechat.service.WechatConfigService;
import com.aichuangzuo.user.modules.wechat.util.WechatSignatureUtil;
import com.aichuangzuo.user.modules.wechat.util.WechatXmlUtil;
import com.aichuangzuo.user.modules.wechat.vo.WechatBindCodeVO;
import com.aichuangzuo.user.modules.wechat.vo.WechatBindQrVO;
import com.aichuangzuo.user.modules.wechat.vo.WechatBindStatusVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 微信公众号绑定服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WechatBindServiceImpl implements WechatBindService {

    private static final int QR_EXPIRE_SECONDS = 600;
    private static final int BIND_CODE_EXPIRE_SECONDS = 600;
    private static final int BIND_CODE_LENGTH = 6;
    private static final String PREFIX = "bind_";
    private static final String BIND_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String BIND_LINK = "https://www.ichuang.top/wechat-bind";
    /**
     * access_token 提前 5 分钟失效，避免边界时间失效
     */
    private static final long ACCESS_TOKEN_EXPIRE_BUFFER_MS = 5 * 60 * 1000;

    private final WechatConfigService wechatConfigService;
    private final RestTemplate restTemplate;
    private final UserWechatBindMapper userWechatBindMapper;
    private final WechatBindQrSessionMapper wechatBindQrSessionMapper;
    private final WechatBindCodeMapper wechatBindCodeMapper;

    private final AtomicReference<String> accessTokenCache = new AtomicReference<>();
    private volatile long accessTokenExpireAt = 0L;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public WechatBindQrVO generateBindQrCode(Long userId) {
        String sceneStr = PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        WechatBindQrSession session = new WechatBindQrSession();
        session.setSceneStr(sceneStr);
        session.setUserId(userId);
        session.setStatus(0);
        session.setExpireTime(LocalDateTime.now().plusSeconds(QR_EXPIRE_SECONDS));
        wechatBindQrSessionMapper.insert(session);

        String ticket = createQrTicket(sceneStr);
        String qrCodeUrl = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=" + ticket;

        return new WechatBindQrVO(qrCodeUrl, sceneStr,
                session.getExpireTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    @Override
    public WechatBindCodeVO generateBindCode(Long userId) {
        String appId = wechatConfigService.getConfig().getAppId();
        String bindCode = generateUniqueBindCode();

        WechatBindCode record = new WechatBindCode();
        record.setUserId(userId);
        record.setBindCode(bindCode);
        record.setAppId(appId);
        record.setStatus(0);
        record.setExpireTime(LocalDateTime.now().plusSeconds(BIND_CODE_EXPIRE_SECONDS));
        wechatBindCodeMapper.insert(record);

        return new WechatBindCodeVO(bindCode,
                record.getExpireTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    @Override
    public WechatBindStatusVO checkBindStatus(Long userId) {
        UserWechatBind bind = userWechatBindMapper.findByUserIdAndAppId(userId, wechatConfigService.getConfig().getAppId());
        if (bind != null) {
            return new WechatBindStatusVO(true, 2, bind.getNickname(), bind.getAvatarUrl());
        }
        WechatBindQrSession session = wechatBindQrSessionMapper.findLatestByUserId(userId);
        if (session == null) {
            return new WechatBindStatusVO(false, null, null, null);
        }
        return new WechatBindStatusVO(false, session.getStatus(), null, null);
    }

    @Override
    public WechatBindStatusVO checkBindStatusBySceneStr(String sceneStr) {
        WechatBindQrSession session = wechatBindQrSessionMapper.findBySceneStr(sceneStr);
        if (session == null) {
            return new WechatBindStatusVO(false, null, null, null);
        }
        if (session.getStatus() == 2) {
            UserWechatBind bind = userWechatBindMapper.findByUserIdAndAppId(session.getUserId(), wechatConfigService.getConfig().getAppId());
            return new WechatBindStatusVO(true, 2,
                    bind == null ? null : bind.getNickname(),
                    bind == null ? null : bind.getAvatarUrl());
        }
        return new WechatBindStatusVO(false, session.getStatus(), null, null);
    }

    @Override
    public String verifyUrl(String signature, String timestamp, String nonce, String echostr) {
        if (WechatSignatureUtil.checkSignature(wechatConfigService.getConfig().getToken(), signature, timestamp, nonce)) {
            return echostr;
        }
        log.warn("微信服务器配置验证失败, signature={}", signature);
        return "";
    }

    @Override
    public String handleCallback(String signature, String timestamp, String nonce, String body) {
        if (!WechatSignatureUtil.checkSignature(wechatConfigService.getConfig().getToken(), signature, timestamp, nonce)) {
            log.warn("微信回调签名校验失败");
            return "success";
        }
        if (body == null || body.isBlank()) {
            return "success";
        }

        Map<String, String> message = WechatXmlUtil.parseXml(body);
        String msgType = message.get("MsgType");
        String fromUser = message.get("FromUserName");
        String toUser = message.get("ToUserName");

        if ("text".equals(msgType)) {
            String content = message.getOrDefault("Content", "").trim();
            if ("绑定账号".equals(content) || "bind".equalsIgnoreCase(content)) {
                return handleBindAccountText(fromUser, toUser);
            }
            String lower = content.toLowerCase();
            if (lower.startsWith("绑定 ") || lower.startsWith("bind ")) {
                String bindCode = content.substring(content.indexOf(' ') + 1).trim().toUpperCase();
                return handleBindCodeMessage(fromUser, toUser, bindCode);
            }
            return "success";
        }

        if (!"event".equals(msgType)) {
            return "success";
        }

        String event = message.get("Event");
        if ("subscribe".equals(event)) {
            String eventKey = message.getOrDefault("EventKey", "");
            if (eventKey.startsWith("qrscene_")) {
                String sceneStr = eventKey.substring("qrscene_".length());
                processScan(fromUser, sceneStr);
                return WechatXmlUtil.buildTextMessage(fromUser, toUser, "绑定成功，欢迎加入爱创作工坊！");
            }
            return WechatXmlUtil.buildTextMessage(fromUser, toUser,
                    "欢迎加入爱创作工坊！点击菜单【开始创作】，生成你的第一篇内容。");
        }

        if ("SCAN".equals(event)) {
            String sceneStr = message.getOrDefault("EventKey", "");
            processScan(fromUser, sceneStr);
            return WechatXmlUtil.buildTextMessage(fromUser, toUser, "绑定成功，欢迎加入爱创作工坊！");
        }

        if ("CLICK".equals(event)) {
            String eventKey = message.getOrDefault("EventKey", "");
            return switch (eventKey) {
                case "bind_account" -> buildBindAccountLinkReply(fromUser, toUser);
                case "start_creation" -> WechatXmlUtil.buildTextMessage(fromUser, toUser,
                        "点击链接开始创作：\nhttps://www.ichuang.top/learn");
                case "learn" -> WechatXmlUtil.buildTextMessage(fromUser, toUser,
                        "欢迎来到创作学院！\n\n点击查看入门教程：\nhttps://www.ichuang.top/learn");
                case "help_doc" -> WechatXmlUtil.buildTextMessage(fromUser, toUser,
                        "使用帮助：\n1. 访问 https://www.ichuang.top/learn 开始创作\n2. 在 App/网页内点击【我的 → 绑定公众号】可接收服务提醒\n3. 如有问题请联系客服");
                default -> "success";
            };
        }

        return "success";
    }

    private String handleBindAccountText(String fromUser, String toUser) {
        return buildBindAccountLinkReply(fromUser, toUser);
    }

    private String buildBindAccountLinkReply(String fromUser, String toUser) {
        UserWechatBind bind = userWechatBindMapper.findByOpenidAndAppId(fromUser, wechatConfigService.getConfig().getAppId());
        if (bind != null) {
            return WechatXmlUtil.buildTextMessage(fromUser, toUser,
                    "您已成功绑定爱创作工坊账号 ✅\n\n如需解绑，回复【解绑】。");
        }
        return WechatXmlUtil.buildTextMessage(fromUser, toUser,
                "您还未绑定账号。\n\n请点击以下链接完成绑定：\n" + BIND_LINK);
    }

    private String handleBindCodeMessage(String fromUser, String toUser, String bindCode) {
        if (bindCode.isBlank() || bindCode.length() != BIND_CODE_LENGTH) {
            return WechatXmlUtil.buildTextMessage(fromUser, toUser,
                    "绑定码格式不正确，请发送「绑定 你的绑定码」。");
        }
        try {
            bindByCode(fromUser, bindCode);
            return WechatXmlUtil.buildTextMessage(fromUser, toUser,
                    "绑定成功，欢迎加入爱创作工坊！后续可在微信内接收服务提醒。");
        } catch (IllegalStateException e) {
            return WechatXmlUtil.buildTextMessage(fromUser, toUser, e.getMessage());
        }
    }

    private void bindByCode(String openid, String bindCode) {
        WechatBindCode record = wechatBindCodeMapper.findByBindCode(bindCode);
        if (record == null) {
            throw new IllegalStateException("绑定码不存在，请检查是否输入正确。");
        }
        if (record.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("绑定码已过期，请重新获取。");
        }
        if (record.getStatus() == 2) {
            throw new IllegalStateException("该绑定码已被使用。");
        }

        String appId = wechatConfigService.getConfig().getAppId();
        UserWechatBind existingByOpenid = userWechatBindMapper.findByOpenidAndAppId(openid, appId);
        if (existingByOpenid != null) {
            throw new IllegalStateException("该微信已绑定其他账号。");
        }
        UserWechatBind existingByUser = userWechatBindMapper.findByUserIdAndAppId(record.getUserId(), appId);
        if (existingByUser != null) {
            throw new IllegalStateException("您的账号已绑定其他微信。");
        }

        UserWechatBind bind = new UserWechatBind();
        bind.setUserId(record.getUserId());
        bind.setOpenid(openid);
        bind.setAppId(appId);
        bind.setBindTime(LocalDateTime.now());
        userWechatBindMapper.insert(bind);

        wechatBindCodeMapper.updateStatusByBindCode(bindCode, 2, openid);
        log.info("公众号绑定成功, userId={}, openid={}", record.getUserId(), openid);
    }

    private String generateUniqueBindCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(BIND_CODE_LENGTH);
            for (int i = 0; i < BIND_CODE_LENGTH; i++) {
                sb.append(BIND_CODE_CHARS.charAt(secureRandom.nextInt(BIND_CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (wechatBindCodeMapper.findByBindCode(code) == null) {
                return code;
            }
        }
        throw new IllegalStateException("生成唯一绑定码失败，请重试");
    }

    private void processScan(String openid, String sceneStr) {
        if (sceneStr == null || !sceneStr.startsWith(PREFIX)) {
            return;
        }
        WechatBindQrSession session = wechatBindQrSessionMapper.findBySceneStr(sceneStr);
        if (session == null) {
            log.warn("二维码会话不存在, sceneStr={}", sceneStr);
            return;
        }
        if (session.getExpireTime().isBefore(LocalDateTime.now())) {
            log.warn("二维码已过期, sceneStr={}", sceneStr);
            return;
        }
        if (session.getStatus() == 2) {
            return;
        }

        String appId = wechatConfigService.getConfig().getAppId();
        Long userId = session.getUserId();
        UserWechatBind existing = userWechatBindMapper.findByUserIdAndAppId(userId, appId);
        if (existing != null) {
            wechatBindQrSessionMapper.updateStatusBySceneStr(sceneStr, 2, openid);
            return;
        }

        UserWechatBind bind = new UserWechatBind();
        bind.setUserId(userId);
        bind.setOpenid(openid);
        bind.setAppId(appId);
        bind.setBindTime(LocalDateTime.now());
        userWechatBindMapper.insert(bind);

        wechatBindQrSessionMapper.updateStatusBySceneStr(sceneStr, 2, openid);
        log.info("公众号绑定成功, userId={}, openid={}", userId, openid);
    }

    private String createQrTicket(String sceneStr) {
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=" + accessToken;

        Map<String, Object> actionInfo = new HashMap<>();
        Map<String, Object> scene = new HashMap<>();
        scene.put("scene_str", sceneStr);
        actionInfo.put("scene", scene);

        Map<String, Object> params = new HashMap<>();
        params.put("expire_seconds", QR_EXPIRE_SECONDS);
        params.put("action_name", "QR_STR_SCENE");
        params.put("action_info", actionInfo);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            throw new RuntimeException("生成二维码请求 JSON 失败", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setContentLength(jsonBody.getBytes(StandardCharsets.UTF_8).length);
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("生成微信二维码失败：无响应");
        }

        Map<String, Object> result;
        try {
            result = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("解析微信二维码响应失败", e);
        }
        if (result.get("ticket") == null) {
            log.error("生成微信二维码失败, response={}", result);
            throw new RuntimeException("生成微信二维码失败");
        }
        return (String) result.get("ticket");
    }

    private synchronized String getAccessToken() {
        String cached = accessTokenCache.get();
        if (cached != null && System.currentTimeMillis() < accessTokenExpireAt) {
            return cached;
        }

        WechatConfigService.ConfigSnapshot config = wechatConfigService.getConfig();
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
}
