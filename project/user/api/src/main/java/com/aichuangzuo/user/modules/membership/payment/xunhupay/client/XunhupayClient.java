package com.aichuangzuo.user.modules.membership.payment.xunhupay.client;

import com.aichuangzuo.user.modules.membership.payment.config.entity.PaymentConfig;
import com.aichuangzuo.user.modules.membership.payment.xunhupay.dto.XunhupayNotifyParams;
import com.aichuangzuo.user.modules.membership.payment.xunhupay.dto.XunhupayPaymentRequest;
import com.aichuangzuo.user.modules.membership.payment.xunhupay.dto.XunhupayPaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 虎皮椒支付客户端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XunhupayClient {

    private static final String VERSION = "1.1";
    private static final int NONCE_LENGTH = 9;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public XunhupayPaymentResponse createPayment(PaymentConfig config, String orderNo,
                                                  BigDecimal totalFee, String title) {
        Map<String, Object> params = new HashMap<>();
        params.put("version", VERSION);
        params.put("appid", config.getAppId());
        params.put("trade_order_id", orderNo);
        params.put("total_fee", totalFee.toPlainString());
        params.put("title", title);
        params.put("time", getSecondTimestamp(new Date()));
        putIfNotBlank(params, "notify_url", config.getNotifyUrl());
        putIfNotBlank(params, "return_url", config.getReturnUrl());
        putIfNotBlank(params, "plugins", "爱创作订阅");
        putIfNotBlank(params, "attach", orderNo);
        params.put("nonce_str", getRandomNumber(NONCE_LENGTH));
        params.put("hash", createSign(params, config.getAppSecret()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

        log.info("虎皮椒下单 orderNo={}", orderNo);
        String responseBody = restTemplate.postForObject(config.getGatewayUrl(), entity, String.class);
        log.info("虎皮椒下单响应 orderNo={}, body={}", orderNo, responseBody);

        try {
            XunhupayPaymentResponse response = objectMapper.readValue(responseBody, XunhupayPaymentResponse.class);
            if (response == null) {
                response = new XunhupayPaymentResponse();
                response.setErrcode(-1);
                response.setErrmsg("响应解析失败");
                return response;
            }
            if (response.getErrcode() == null) {
                response.setErrcode(-1);
            }
            if (response.getErrcode() == 0 && !verifyPaymentResponseHash(response, config.getAppSecret())) {
                log.error("虎皮椒下单响应签名验证失败 orderNo={}", orderNo);
                response.setErrcode(-1);
                response.setErrmsg("下单响应签名验证失败");
            }
            return response;
        } catch (Exception e) {
            log.error("虎皮椒下单响应解析失败 orderNo={}", orderNo, e);
            XunhupayPaymentResponse resp = new XunhupayPaymentResponse();
            resp.setErrcode(-1);
            resp.setErrmsg("响应解析失败");
            return resp;
        }
    }

    private boolean verifyPaymentResponseHash(XunhupayPaymentResponse response, String appSecret) {
        Map<String, Object> signParams = new HashMap<>();
        if (response.getOpenid() != null) {
            signParams.put("openid", response.getOpenid());
        }
        putIfNotBlank(signParams, "url", response.getUrl());
        putIfNotBlank(signParams, "url_qrcode", response.getUrlQrcode());
        if (response.getErrcode() != null) {
            signParams.put("errcode", response.getErrcode());
        }
        putIfNotBlank(signParams, "errmsg", response.getErrmsg());
        String expected = createSign(signParams, appSecret);
        return expected.equalsIgnoreCase(response.getHash());
    }

    private void putIfNotBlank(Map<String, Object> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value);
        }
    }

    public boolean verifyNotify(XunhupayNotifyParams params, String appSecret) {
        Map<String, Object> signParams = new HashMap<>();
        signParams.put("appid", params.getAppid());
        signParams.put("trade_order_id", params.getTradeOrderId());
        signParams.put("total_fee", params.getTotalFee());
        putIfNotBlank(signParams, "transaction_id", params.getTransactionId());
        putIfNotBlank(signParams, "open_order_id", params.getOpenOrderId());
        putIfNotBlank(signParams, "order_title", params.getOrderTitle());
        signParams.put("status", params.getStatus());
        putIfNotBlank(signParams, "plugins", params.getPlugins());
        putIfNotBlank(signParams, "attach", params.getAttach());
        signParams.put("time", params.getTime());
        signParams.put("nonce_str", params.getNonceStr());
        String expected = createSign(signParams, appSecret);
        return expected.equalsIgnoreCase(params.getHash());
    }

    public String createSign(Map<String, Object> params, String appSecret) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            sb.append(key).append("=").append(params.get(key)).append("&");
        }
        if (!sb.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        String source = sb + appSecret;
        return md5(source);
    }

    private String md5(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }

    private int getSecondTimestamp(Date date) {
        if (date == null) {
            return 0;
        }
        String timestamp = String.valueOf(date.getTime());
        int length = timestamp.length();
        if (length > 3) {
            return Integer.parseInt(timestamp.substring(0, length - 3));
        }
        return 0;
    }

    private String getRandomNumber(int length) {
        String str = "0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
