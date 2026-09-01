package com.aichuangzuo.user.modules.membership.payment.xunhupay.mapper;

import com.aichuangzuo.user.modules.membership.payment.xunhupay.entity.PaymentNotifyLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付异步通知日志 Mapper。
 */
@Mapper
public interface PaymentNotifyLogMapper extends BaseMapper<PaymentNotifyLog> {
}
