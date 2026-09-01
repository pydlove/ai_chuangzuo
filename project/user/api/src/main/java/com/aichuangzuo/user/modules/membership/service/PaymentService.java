package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.payment.xunhupay.dto.XunhupayNotifyParams;
import com.aichuangzuo.user.modules.membership.vo.PaymentConfigVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;

/**
 * 支付服务。
 */
public interface PaymentService {

    /**
     * 获取当前支付配置公开信息。
     *
     * @return 支付配置 VO
     */
    PaymentConfigVO getPaymentConfig();

    /**
     * 创建订阅订单并发起支付。
     *
     * <p>测试模式下直接确认订单；正式模式下调用虎皮椒网关返回支付地址。
     *
     * @param userId  当前用户ID
     * @param request 订阅请求
     * @return 订阅结果（正式模式包含 payUrl）
     */
    SubscribeResultVO createPaymentOrder(Long userId, SubscribeRequest request);

    /**
     * 确认订单已支付并发放会员权益。
     *
     * @param orderNo           本地订单号
     * @param thirdPartyTradeId 第三方交易流水号
     * @param fromTestMode      是否来自测试模式
     */
    void confirmOrder(String orderNo, String thirdPartyTradeId, boolean fromTestMode);

    /**
     * 处理虎皮椒异步通知。
     *
     * @param params  通知参数
     * @param rawBody 原始请求体
     */
    void handleXunhupayNotify(XunhupayNotifyParams params, String rawBody);
}
