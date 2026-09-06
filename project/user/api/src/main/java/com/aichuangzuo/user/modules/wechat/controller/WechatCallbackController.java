package com.aichuangzuo.user.modules.wechat.controller;

import com.aichuangzuo.user.modules.wechat.service.WechatBindService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * 微信公众号服务器回调接口。
 * <p>该接口需要被微信服务器访问，因此配置在公开路径下。
 */
@RestController
@RequestMapping("/api/v1/public/wechat")
@RequiredArgsConstructor
@Slf4j
public class WechatCallbackController {

    private final WechatBindService wechatBindService;

    /**
     * 服务器配置 URL 验证。
     */
    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        log.info("微信服务器配置验证, signature={}, timestamp={}, nonce={}", signature, timestamp, nonce);
        return wechatBindService.verifyUrl(signature, timestamp, nonce, echostr);
    }

    /**
     * 接收微信消息/事件推送。
     */
    @PostMapping(value = "/callback", produces = MediaType.APPLICATION_XML_VALUE)
    public String callback(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            HttpServletRequest request) throws IOException {
        String body = readBody(request);
        log.debug("微信回调, signature={}, body={}", signature, body);
        return wechatBindService.handleCallback(signature, timestamp, nonce, body);
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
