package com.aichuangzuo.user.modules.membership.payment.config.mapper;

import com.aichuangzuo.user.modules.membership.payment.config.entity.PaymentConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付配置 Mapper（只读）。
 */
@Mapper
public interface PaymentConfigMapper extends BaseMapper<PaymentConfig> {
}
