package com.aichuangzuo.user.modules.membership.controller;

import com.aichuangzuo.user.modules.membership.payment.xunhupay.dto.XunhupayNotifyParams;
import com.aichuangzuo.user.modules.membership.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付异步回调接口（公开访问，无需登录）。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public/payment/xunhupay")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentService paymentService;

    @PostMapping(value = "/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public String notify(
            @RequestParam("appid") String appid,
            @RequestParam("trade_order_id") String tradeOrderId,
            @RequestParam("total_fee") String totalFee,
            @RequestParam("transaction_id") String transactionId,
            @RequestParam("open_order_id") String openOrderId,
            @RequestParam(value = "order_title", required = false) String orderTitle,
            @RequestParam("status") String status,
            @RequestParam(value = "plugins", required = false) String plugins,
            @RequestParam(value = "attach", required = false) String attach,
            @RequestParam("time") String time,
            @RequestParam("nonce_str") String nonceStr,
            @RequestParam("hash") String hash) {

        XunhupayNotifyParams params = new XunhupayNotifyParams();
        params.setAppid(appid);
        params.setTradeOrderId(tradeOrderId);
        params.setTotalFee(totalFee);
        params.setTransactionId(transactionId);
        params.setOpenOrderId(openOrderId);
        params.setOrderTitle(orderTitle);
        params.setStatus(status);
        params.setPlugins(plugins);
        params.setAttach(attach);
        params.setTime(time);
        params.setNonceStr(nonceStr);
        params.setHash(hash);

        String rawBody = buildRawBody(params);
        log.info("收到虎皮椒异步通知 orderNo={}, status={}", tradeOrderId, status);

        try {
            paymentService.handleXunhupayNotify(params, rawBody);
            return "success";
        } catch (Exception e) {
            log.error("虎皮椒异步通知处理失败 orderNo={}", tradeOrderId, e);
            return "fail";
        }
    }

    private String buildRawBody(XunhupayNotifyParams params) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("appid", params.getAppid());
        map.put("trade_order_id", params.getTradeOrderId());
        map.put("total_fee", params.getTotalFee());
        map.put("transaction_id", params.getTransactionId());
        map.put("open_order_id", params.getOpenOrderId());
        map.put("order_title", params.getOrderTitle());
        map.put("status", params.getStatus());
        map.put("plugins", params.getPlugins());
        map.put("attach", params.getAttach());
        map.put("time", params.getTime());
        map.put("nonce_str", params.getNonceStr());
        map.put("hash", params.getHash());
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + (e.getValue() == null ? "" : e.getValue()))
                .collect(Collectors.joining("&"));
    }
}
