package com.aichuangzuo.admin.modules.order.payment.xunhupay.client;

import com.aichuangzuo.admin.modules.order.payment.xunhupay.dto.XunhupayRefundResponse;
import com.aichuangzuo.admin.modules.settings.paymentconfig.entity.PaymentConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
 * 虎皮椒退款客户端。
 */
@Slf4j
@Component
public class XunhupayRefundClient {

    private static final int NONCE_LENGTH = 9;
    private static final String DEFAULT_REFUND_URL = "https://api.xunhupay.com/payment/refund.html";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发起退款申请。
     *
     * @param config 支付配置
     * @param orderNo 商户订单号
     * @param reason 退款原因
     * @return 退款响应
     */
    public XunhupayRefundResponse refund(PaymentConfig config, String orderNo, String reason) {
        Map<String, Object> params = new HashMap<>();
        params.put("appid", config.getAppId());
        params.put("trade_order_id", orderNo);
        if (reason != null && !reason.isBlank()) {
            params.put("reason", reason);
        }
        params.put("time", getSecondTimestamp(new Date()));
        params.put("nonce_str", getRandomNumber(NONCE_LENGTH));
        params.put("hash", createSign(params, config.getAppSecret()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

        String refundUrl = config.getRefundUrl();
        if (refundUrl == null || refundUrl.isBlank()) {
            refundUrl = DEFAULT_REFUND_URL;
        }

        log.info("虎皮椒退款 orderNo={}, refundUrl={}", orderNo, refundUrl);
        String responseBody = restTemplate.postForObject(refundUrl, entity, String.class);
        log.info("虎皮椒退款响应 orderNo={}, body={}", orderNo, responseBody);

        try {
            XunhupayRefundResponse response = objectMapper.readValue(responseBody, XunhupayRefundResponse.class);
            if (response == null) {
                response = new XunhupayRefundResponse();
                response.setErrcode(-1);
                response.setErrmsg("响应解析失败");
                return response;
            }
            if (response.getErrcode() == null) {
                response.setErrcode(-1);
            }
            if (response.getErrcode() == 0 && !verifyResponseHash(response, config.getAppSecret())) {
                log.error("虎皮椒退款响应签名验证失败 orderNo={}", orderNo);
                response.setErrcode(-1);
                response.setErrmsg("退款响应签名验证失败");
            }
            return response;
        } catch (Exception e) {
            log.error("虎皮椒退款响应解析失败 orderNo={}", orderNo, e);
            XunhupayRefundResponse resp = new XunhupayRefundResponse();
            resp.setErrcode(-1);
            resp.setErrmsg("响应解析失败");
            return resp;
        }
    }

    /**
     * 验证退款响应签名。
     */
    public boolean verifyResponseHash(XunhupayRefundResponse response, String appSecret) {
        Map<String, Object> signParams = new HashMap<>();
        putIfNotBlank(signParams, "trade_order_id", response.getTradeOrderId());
        putIfNotBlank(signParams, "transaction_id", response.getTransactionId());
        putIfNotBlank(signParams, "out_refund_no", response.getOutRefundNo());
        if (response.getRefundFee() != null) {
            signParams.put("refund_fee", response.getRefundFee());
        }
        putIfNotBlank(signParams, "reason", response.getReason());
        putIfNotBlank(signParams, "refund_status", response.getRefundStatus());
        putIfNotBlank(signParams, "refund_time", response.getRefundTime());
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

    public String createSign(Map<String, Object> params, String appSecret) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            Object value = params.get(key);
            if (value == null || "".equals(value)) {
                continue;
            }
            if ("hash".equals(key)) {
                continue;
            }
            sb.append(key).append("=").append(value).append("&");
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
