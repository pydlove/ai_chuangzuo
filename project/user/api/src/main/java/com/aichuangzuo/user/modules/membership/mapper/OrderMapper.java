package com.aichuangzuo.user.modules.membership.mapper;

import com.aichuangzuo.user.modules.membership.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户订单 Mapper。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
